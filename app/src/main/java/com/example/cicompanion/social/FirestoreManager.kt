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
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.calendar.model.EventNotificationPreference
import java.security.MessageDigest


object FirestoreManager {

    // Parse stored HH:mm class times so saved classes can be sorted by time.
    private fun parseClassTimeForSort(timeText: String): java.time.LocalTime {
        return runCatching {
            java.time.LocalTime.parse(timeText)
        }.getOrDefault(java.time.LocalTime.MAX)
    }

    private const val TAG = "FirestoreManager"

    /**
     * Saves the signed-in user's profile data to Firestore.
     * The document ID is the user's Firebase UID.
     */
    fun saveUserToFirestore(user: FirebaseUser,onSuccess: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        val userProfile = hashMapOf(
            "uid" to user.uid,
            "displayName" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "bio" to "",
            "lastSignInAt" to System.currentTimeMillis()
        )

        userRef.set(userProfile, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User profile saved to Firestore for uid=${user.uid}")

                // PUSH NOTIFICATIONS
                // store this device's FCM token on the user document
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save user profile to Firestore.", exception)
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
    // EVENT NOTIFICATION FIX:
    // Classes opt in once by saved SelectedClass.id.
    // Custom/campus events opt in per event version by event.id + event.start.
    private const val TARGET_TYPE_RECURRING_CLASS = "recurring_class"
    private const val TARGET_TYPE_SINGLE_EVENT = "single_event"
    private const val SINGLE_EVENT_TARGET_SEPARATOR = "::"

    private val recurringClassOccurrenceRegex = Regex("^(.*)-\\d{4}-\\d{2}-\\d{2}$")

    private data class EventNotificationTarget(
        val targetType: String,
        val targetId: String
    )

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    // EVENT NOTIFICATION FIX:
    // Loads all enabled notification preference document IDs for the signed-in user.
    // CalendarViewModel uses this set to decide whether each event's toggle is on or off.
    suspend fun fetchEnabledEventNotificationPreferenceIds(): Set<String> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptySet()
        val db = FirebaseFirestore.getInstance()

        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("eventNotificationPreferences")
                .whereEqualTo("enabled", true)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                document.getString("preferenceId")
            }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching event notification preferences: ${e.message}", e)
            emptySet()
        }
    }

    // EVENT NOTIFICATION FIX:
    // This is the one stable preference-id builder used by the app.
    fun buildEventNotificationPreferenceId(
        targetType: String,
        targetId: String
    ): String {
        return sha256("$targetType::$targetId")
    }

    // EVENT NOTIFICATION FIX:
    // Older code may have created docs using SHA-256(event.id) only.
    // Keep this only so disabling/deleting can clean up stale test docs.
    private fun buildLegacyRawEventPreferenceId(eventId: String): String {
        return sha256(eventId)
    }

    // EVENT NOTIFICATION FIX:
    // A single event version is identified by both its event id and start time.
    // If the event time changes, the opt-in key changes, so the user must opt in again.
    private fun buildSingleEventVersionTargetId(event: CalendarEvent): String {
        val startKey = event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        return "${event.id}$SINGLE_EVENT_TARGET_SEPARATOR$startKey"
    }

    // EVENT NOTIFICATION FIX:
    // Schedule/class events use the original SelectedClass.id.
    // Single events use event.id + event.start.
    private fun deriveEventNotificationTarget(event: CalendarEvent): EventNotificationTarget {
        return if (event.calendarId == "schedule") {
            val recurringClassId = recurringClassOccurrenceRegex
                .matchEntire(event.id)
                ?.groupValues
                ?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?: event.id

            EventNotificationTarget(
                targetType = TARGET_TYPE_RECURRING_CLASS,
                targetId = recurringClassId
            )
        } else {
            EventNotificationTarget(
                targetType = TARGET_TYPE_SINGLE_EVENT,
                targetId = buildSingleEventVersionTargetId(event)
            )
        }
    }

    fun buildEventNotificationPreferenceIdForEvent(event: CalendarEvent): String {
        val target = deriveEventNotificationTarget(event)
        return buildEventNotificationPreferenceId(
            targetType = target.targetType,
            targetId = target.targetId
        )
    }

    // EVENT NOTIFICATION FIX:
    // Save or remove one event notification opt-in for the current user.
    // Classes remain one-time opt-in.
    // Custom/campus events are opt-in per event version.
    suspend fun saveEventNotificationPreference(event: CalendarEvent, enabled: Boolean): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val target = deriveEventNotificationTarget(event)
        val preferenceId = buildEventNotificationPreferenceId(
            targetType = target.targetType,
            targetId = target.targetId
        )

        val preferencesRef = db.collection("users")
            .document(user.uid)
            .collection("eventNotificationPreferences")

        return try {
            if (enabled) {
                val payload = hashMapOf(
                    "preferenceId" to preferenceId,
                    "targetType" to target.targetType,
                    "targetId" to target.targetId,
                    "title" to event.title,
                    "calendarId" to event.calendarId,
                    "start" to event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    "enabled" to true,
                    "updatedAt" to System.currentTimeMillis()
                )

                preferencesRef.document(preferenceId).set(payload).await()
            } else {
                // EVENT NOTIFICATION FIX:
                // Delete the current preference id and older legacy ids from previous test versions.
                val preferenceIdsToDelete = mutableSetOf(
                    preferenceId,
                    buildEventNotificationPreferenceId(target.targetType, event.id),
                    buildLegacyRawEventPreferenceId(event.id)
                )

                preferenceIdsToDelete.forEach { id ->
                    preferencesRef.document(id).delete().await()
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving event notification preference: ${e.message}", e)
            false
        }
    }

    // CALENDAR SCHEDULE CHANGE:
    // Save one selected class entry for the signed-in user.
    suspend fun saveSelectedClass(selectedClass: SelectedClass): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        val classData = hashMapOf(
            "id" to selectedClass.id,
            "majorCode" to selectedClass.majorCode,
            "majorName" to selectedClass.majorName,
            "courseCode" to selectedClass.courseCode,
            "courseTitle" to selectedClass.courseTitle,
            "typicallyOffered" to selectedClass.typicallyOffered,
            "daysOfWeek" to selectedClass.daysOfWeek,
            "startTime" to selectedClass.startTime,
            "endTime" to selectedClass.endTime,
            "startDate" to selectedClass.startDate,
            "endDate" to selectedClass.endDate,
            "location" to selectedClass.location,
            "notes" to selectedClass.notes,
            "termLabel" to selectedClass.termLabel,
            "colorArgb" to selectedClass.colorArgb,
            "reminderEnabled" to selectedClass.reminderEnabled,
            "reminderMinutesBefore" to selectedClass.reminderMinutesBefore,
            "createdAt" to selectedClass.createdAt,
            "updatedAt" to selectedClass.updatedAt
        )

        return try {
            db.collection("users")
                .document(user.uid)
                .collection("selectedClasses")
                .document(selectedClass.id)
                .set(classData)
                .await()

            true
            // CALENDAR SCHEDULE CHANGE:
            // Log the exact Firestore error so schedule save failures are easier to debug.
        } catch (e: Exception) {
            Log.e(TAG, "Error saving selected class: ${e.message}", e)
            false
        }
    }

    // CALENDAR SCHEDULE CHANGE:
    // Delete one selected class entry for the signed-in user.
    suspend fun deleteSelectedClass(selectedClassId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        return try {
            db.collection("users")
                .document(user.uid)
                .collection("selectedClasses")
                .document(selectedClassId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting selected class", e)
            false
        }
    }

    // CALENDAR SCHEDULE CHANGE
    // Fetch all selected class entries for the signed-in user.
    suspend fun fetchSelectedClasses(): List<SelectedClass> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()

        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("selectedClasses")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                SelectedClass(
                    id = document.getString("id") ?: return@mapNotNull null,
                    majorCode = document.getString("majorCode") ?: "",
                    majorName = document.getString("majorName") ?: "",
                    courseCode = document.getString("courseCode") ?: "",
                    courseTitle = document.getString("courseTitle") ?: "",
                    typicallyOffered = document.getString("typicallyOffered") ?: "",
                    daysOfWeek = (document.get("daysOfWeek") as? List<*>)?.mapNotNull {
                        (it as? Number)?.toInt()
                    } ?: emptyList(),
                    startTime = document.getString("startTime") ?: "",
                    endTime = document.getString("endTime") ?: "",
                    startDate = document.getString("startDate") ?: "",
                    endDate = document.getString("endDate") ?: "",
                    location = document.getString("location") ?: "",
                    notes = document.getString("notes") ?: "",
                    termLabel = document.getString("termLabel") ?: "",
                    colorArgb = (document.getLong("colorArgb") ?: 0xFFEF3347).toInt(),
                    reminderEnabled = document.getBoolean("reminderEnabled") ?: false,
                    reminderMinutesBefore = document.getLong("reminderMinutesBefore")?.toInt(),
                    createdAt = document.getLong("createdAt") ?: 0L,
                    updatedAt = document.getLong("updatedAt") ?: 0L
                )
            }.sortedWith(
                compareBy<SelectedClass>(
                    { it.daysOfWeek.minOrNull() ?: Int.MAX_VALUE },
                    { parseClassTimeForSort(it.startTime) },
                    { it.courseCode }
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching selected classes", e)
            emptyList()
        }
    }

    suspend fun saveCustomEvent(event: CalendarEvent): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        // CUSTOM EVENT FIX:
        // Firestore rules for customEvents require ownerId and isBookmarked.
        val eventData = hashMapOf(
            "id" to event.id,
            "title" to event.title,
            "description" to (event.description ?: ""),
            "location" to (event.location ?: ""),
            "start" to event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "end" to event.endExclusive.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "isAllDay" to event.isAllDay,
            "calendarId" to "custom",
            "isPinned" to event.isPinned,
            "ownerId" to user.uid,
            "isBookmarked" to false
        )

        return try {
            db.collection("users")
                .document(user.uid)
                .collection("customEvents")
                .document(event.id)
                .set(eventData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom event: ${e.message}", e)
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