package com.example.cicompanion.social

import android.util.Log
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.maps.CampusLocation
import com.example.cicompanion.maps.LocationType
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FirestoreManager {

    private const val TAG = "FirestoreManager"

    /**
     * Saves the signed-in user's profile data to Firestore.
     * The document ID is the user's Firebase UID.
     */
    fun saveUserToFirestore(user: FirebaseUser, onSuccess: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            val data = hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastSignInAt" to System.currentTimeMillis()
            )

            // Initialize display names and bio only if this is a new user
            if (!document.exists()) {
                data["displayName"] = user.displayName ?: ""
                data["bio"] = ""
            }

            userRef.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User profile saved to Firestore for uid=${user.uid}")
                    onSuccess?.invoke()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to save user profile to Firestore.", exception)
                }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to fetch user document before saving.", exception)
        }
    }

    // PUSH NOTIFICATIONS
    // store this device's FCM token on the user document
    fun saveFcmToken(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()

        // PUSH NOTIFICATIONS CHANGE:
        // Store token in the existing owner-only subcollection allowed by your rules.
        val tokenRef = db.collection("users")
            .document(userId)
            .collection("fcmTokens")
            .document("current")

        val tokenPayload = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis()
        )

        tokenRef.set(tokenPayload, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Saved FCM token for uid=$userId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save FCM token for uid=$userId", exception)
            }
    }

    suspend fun saveCustomEvent(event: CalendarEvent): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val eventData = hashMapOf(
            "id" to event.id,
            "title" to event.title,
            "description" to (event.description ?: ""),
            "location" to (event.location ?: ""),
            "start" to event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "end" to event.endExclusive.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "isAllDay" to event.isAllDay,
            "calendarId" to "custom",
            "isPinned" to event.isPinned
        )

        return try {
            db.collection("users").document(user.uid)
                .collection("customEvents").document(event.id)
                .set(eventData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom event", e)
            false
        }
    }

    suspend fun updateEventPinStatus(eventId: String, isPinned: Boolean): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("customEvents").document(eventId)
                .update("isPinned", isPinned)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pin status", e)
            false
        }
    }

    suspend fun deleteCustomEvent(eventId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("customEvents").document(eventId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting custom event", e)
            false
        }
    }

    suspend fun fetchCustomEvents(): List<CalendarEvent> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()

        return try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("customEvents")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: ""
                val title = doc.getString("title") ?: ""
                val description = doc.getString("description")
                val location = doc.getString("location")
                val startStr = doc.getString("start") ?: return@mapNotNull null
                val endStr = doc.getString("end") ?: return@mapNotNull null
                val isAllDay = doc.getBoolean("isAllDay") ?: false
                val isPinned = doc.getBoolean("isPinned") ?: false

                CalendarEvent(
                    id = id,
                    calendarId = "custom",
                    title = title,
                    description = description,
                    location = location,
                    htmlLink = null,
                    start = ZonedDateTime.parse(startStr),
                    endExclusive = ZonedDateTime.parse(endStr),
                    isAllDay = isAllDay,
                    isPinned = isPinned
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching custom events", e)
            emptyList()
        }
    }

    suspend fun fetchCampusLocations(): List<CampusLocation> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("campusLocations").get().await()
            if (snapshot.isEmpty) return emptyList()

            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: ""
                val lat = doc.getDouble("latitude") ?: 0.0
                val lng = doc.getDouble("longitude") ?: 0.0
                val description = doc.getString("description") ?: ""
                val typeStr = doc.getString("type") ?: "BUILDING"
                val colorHex = doc.getString("color") ?: "#D32F2F"

                val type = try { LocationType.valueOf(typeStr) } catch(e: Exception) { LocationType.BUILDING }
                val color = Color(android.graphics.Color.parseColor(colorHex))
                val icon = when (type) {
                    LocationType.BUILDING -> Icons.Default.Business
                    LocationType.FOOD -> Icons.Default.Restaurant
                    LocationType.AREA -> Icons.Default.People
                    LocationType.HOUSING -> Icons.Default.Home
                    LocationType.PARKING -> Icons.Default.LocalParking
                }

                CampusLocation(name, LatLng(lat, lng), description, type, icon, color)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campus locations", e)
            emptyList()
        }
    }

    suspend fun saveQuickAccessButtons(buttonRoutes: List<String>): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            val data = hashMapOf("quickAccessButtons" to buttonRoutes)
            db.collection("users").document(user.uid)
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving quick access buttons", e)
            false
        }
    }

    suspend fun fetchQuickAccessButtons(): List<String>? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val db = FirebaseFirestore.getInstance()
        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            if (!doc.exists()) return null
            
            val rawList = doc.get("quickAccessButtons") as? List<*>
            rawList?.mapNotNull { it as? String }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching quick access buttons", e)
            null
        }
    }
}
