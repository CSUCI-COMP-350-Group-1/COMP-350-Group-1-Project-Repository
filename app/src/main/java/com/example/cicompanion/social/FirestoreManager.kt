package com.example.cicompanion.social

import android.util.Log
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.maps.CampusLocation
import com.example.cicompanion.maps.LocationType
import com.example.cicompanion.maps.CustomPin
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
import java.security.MessageDigest

object FirestoreManager {

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
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save user profile to Firestore.", exception)
            }
    }

    fun saveFcmToken(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
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

    // EVENT NOTIFICATION MERGE:
    // Classes opt in once by saved SelectedClass.id.
    // Custom/campus/shared one-time events opt in per event version by event.id + event.start.
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

    // EVENT NOTIFICATION MERGE:
    // Loads all enabled notification preference document IDs for the signed-in user.
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

    // EVENT NOTIFICATION MERGE:
    // Stable Firestore-safe ID builder.
    fun buildEventNotificationPreferenceId(
        targetType: String,
        targetId: String
    ): String {
        return sha256("$targetType::$targetId")
    }

    // EVENT NOTIFICATION MERGE:
    // Older test data may have used SHA-256(event.id) only.
    // Keep this so disabling/deleting can clean stale docs too.
    private fun buildLegacyRawEventPreferenceId(eventId: String): String {
        return sha256(eventId)
    }

    // EVENT NOTIFICATION MERGE:
    // A single-event version is event id + start time.
    // If the event time changes, the opt-in key changes and the user must opt in again.
    private fun buildSingleEventVersionTargetId(event: CalendarEvent): String {
        val startKey = event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        return "${event.id}$SINGLE_EVENT_TARGET_SEPARATOR$startKey"
    }

    // EVENT NOTIFICATION MERGE:
    // Class events use the original selected class id.
    // One-time events use event.id + event.start.
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

    // EVENT NOTIFICATION MERGE:
    // Save/remove one event notification opt-in for the current user.
    // Classes remain one-time opt-in.
    // Custom/campus/shared events are opt-in per event version.
    suspend fun saveEventNotificationPreference(
        event: CalendarEvent,
        enabled: Boolean
    ): Boolean {
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


    /**
     * Encodes 2nd range fields into the 'notes' string to comply with strict Firestore rules
     * that only allow a fixed set of keys in SelectedClass documents.
     */
    private fun encodeNotesWithSecondRange(selectedClass: SelectedClass): String {
        if (!selectedClass.hasSecondTimeRange) return selectedClass.notes
        val data = "||2ND_RANGE||${selectedClass.daysOfWeek2.joinToString(",")}|${selectedClass.startTime2}|${selectedClass.endTime2}|${selectedClass.location2}|${selectedClass.notes2}"
        return if (selectedClass.notes.isBlank()) data else "${selectedClass.notes}\n$data"
    }

    /**
     * Decodes the structure in 'notes' back into SelectedClass fields.
     */
    private fun decodeNotesWithSecondRange(encoded: String, base: SelectedClass): SelectedClass {
        val delimiter = "||2ND_RANGE||"
        if (!encoded.contains(delimiter)) return base.copy(notes = encoded, hasSecondTimeRange = false)
        
        val parts = encoded.split(delimiter)
        val userNotes = parts[0].trim()
        val rangeData = parts.getOrNull(1)?.split("|", limit = 5) ?: return base.copy(notes = encoded)
        
        return if (rangeData.size >= 5) {
            base.copy(
                notes = userNotes,
                hasSecondTimeRange = true,
                daysOfWeek2 = rangeData[0].split(",").mapNotNull { it.toIntOrNull() },
                startTime2 = rangeData[1],
                endTime2 = rangeData[2],
                location2 = rangeData[3],
                notes2 = rangeData[4]
            )
        } else if (rangeData.size == 4) {
            // Old format: days|start|end|notes2
            base.copy(
                notes = userNotes,
                hasSecondTimeRange = true,
                daysOfWeek2 = rangeData[0].split(",").mapNotNull { it.toIntOrNull() },
                startTime2 = rangeData[1],
                endTime2 = rangeData[2],
                location2 = "",
                notes2 = rangeData[3]
            )
        } else {
            base.copy(notes = encoded)
        }
    }

    suspend fun saveSelectedClass(selectedClass: SelectedClass): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        // We only include keys allowed by the user's Firestore rules
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
            "notes" to encodeNotesWithSecondRange(selectedClass),
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
                .set(classData) // Using full set because we provided all allowed keys
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving selected class: ${e.message}", e)
            false
        }
    }

    suspend fun deleteSelectedClass(selectedClassId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("selectedClasses").document(selectedClassId)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting selected class", e)
            false
        }
    }

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
                val rawNotes = document.getString("notes") ?: ""
                val base = SelectedClass(
                    id = document.getString("id") ?: return@mapNotNull null,
                    majorCode = document.getString("majorCode") ?: "",
                    majorName = document.getString("majorName") ?: "",
                    courseCode = document.getString("courseCode") ?: "",
                    courseTitle = document.getString("courseTitle") ?: "",
                    typicallyOffered = document.getString("typicallyOffered") ?: "",
                    daysOfWeek = (document.get("daysOfWeek") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                    startTime = document.getString("startTime") ?: "",
                    endTime = document.getString("endTime") ?: "",
                    startDate = document.getString("startDate") ?: "",
                    endDate = document.getString("endDate") ?: "",
                    location = document.getString("location") ?: "",
                    notes = rawNotes,
                    termLabel = document.getString("termLabel") ?: "",
                    colorArgb = (document.getLong("colorArgb") ?: 0xFFEF3347).toInt(),
                    reminderEnabled = document.getBoolean("reminderEnabled") ?: false,
                    reminderMinutesBefore = document.getLong("reminderMinutesBefore")?.toInt(),
                    createdAt = document.getLong("createdAt") ?: 0L,
                    updatedAt = document.getLong("updatedAt") ?: 0L
                )
                decodeNotesWithSecondRange(rawNotes, base)
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

        val eventData = hashMapOf(
            "id" to event.id,
            "title" to event.title,
            "description" to (event.description ?: ""),
            "location" to (event.location ?: ""),
            "start" to event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "end" to event.endExclusive.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            "isAllDay" to event.isAllDay,
            "calendarId" to event.calendarId,
            "isPinned" to event.isPinned,
            "ownerId" to (event.ownerId ?: user.uid),
            "maxMembers" to event.maxMembers,
            "isPinnedByLeader" to event.isPinnedByLeader,
            "isBookmarked" to event.isBookmarked
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
                .update("isPinned", isPinned).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pin status", e)
            false
        }
    }

    suspend fun updateEventBookmarkStatus(eventId: String, isBookmarked: Boolean): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("customEvents").document(eventId)
                .update("isBookmarked", isBookmarked).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bookmark status", e)
            false
        }
    }

    suspend fun deleteCustomEvent(eventId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("customEvents").document(eventId)
                .delete().await()
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
                val startStr = doc.getString("start") ?: return@mapNotNull null
                val endStr = doc.getString("end") ?: return@mapNotNull null
                CalendarEvent(
                    id = doc.getString("id") ?: "",
                    calendarId = doc.getString("calendarId") ?: "custom",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description"),
                    location = doc.getString("location"),
                    htmlLink = null,
                    start = ZonedDateTime.parse(startStr),
                    endExclusive = ZonedDateTime.parse(endStr),
                    isAllDay = doc.getBoolean("isAllDay") ?: false,
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    ownerId = doc.getString("ownerId"),
                    maxMembers = doc.getLong("maxMembers")?.toInt(),
                    isPinnedByLeader = doc.getBoolean("isPinnedByLeader") ?: false,
                    isBookmarked = doc.getBoolean("isBookmarked") ?: false,
                    isShared = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching custom events", e)
            emptyList()
        }
    }

    suspend fun saveCustomPin(pin: CustomPin): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val pinData = hashMapOf(
            "id" to pin.id,
            "userId" to user.uid,
            "name" to pin.name,
            "latitude" to pin.latitude,
            "longitude" to pin.longitude,
            "description" to pin.description,
            "colorArgb" to pin.colorArgb,
            "isFavorited" to pin.isFavorited,
            "associatedEventId" to pin.associatedEventId
        )

        return try {
            db.collection("users").document(user.uid)
                .collection("customPins").document(pin.id)
                .set(pinData).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom pin", e)
            false
        }
    }

    suspend fun fetchCustomPins(): List<CustomPin> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()

        return try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("customPins")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                CustomPin(
                    id = doc.getString("id") ?: "",
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    description = doc.getString("description") ?: "",
                    colorArgb = doc.getLong("colorArgb")?.toInt() ?: 0,
                    isFavorited = doc.getBoolean("isFavorited") ?: false,
                    associatedEventId = doc.getString("associatedEventId")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching custom pins", e)
            emptyList()
        }
    }

    suspend fun deleteCustomPin(pinId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users").document(user.uid)
                .collection("customPins").document(pinId)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting custom pin", e)
            false
        }
    }

    suspend fun updateCustomPin(pin: CustomPin): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val pinData = hashMapOf(
            "id" to pin.id,
            "userId" to user.uid,
            "name" to pin.name,
            "latitude" to pin.latitude,
            "longitude" to pin.longitude,
            "description" to pin.description,
            "colorArgb" to pin.colorArgb,
            "isFavorited" to pin.isFavorited,
            "associatedEventId" to pin.associatedEventId
        )
        return try {
            db.collection("users").document(user.uid)
                .collection("customPins").document(pin.id)
                .set(pinData, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating custom pin", e)
            false
        }
    }

    suspend fun fetchCampusLocations(): List<CampusLocation> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("campusLocations").get().await()
            snapshot.documents.mapNotNull { doc ->
                val typeStr = doc.getString("type") ?: "BUILDING"
                val type = try { LocationType.valueOf(typeStr) } catch(e: Exception) { LocationType.BUILDING }
                val colorHex = doc.getString("color") ?: "#D32F2F"
                val color = Color(android.graphics.Color.parseColor(colorHex))
                val icon = when (type) {
                    LocationType.BUILDING -> Icons.Default.Business
                    LocationType.FOOD -> Icons.Default.Restaurant
                    LocationType.AREA -> Icons.Default.People
                    LocationType.HOUSING -> Icons.Default.Home
                    LocationType.PARKING -> Icons.Default.LocalParking
                    LocationType.CUSTOM -> Icons.Default.PushPin
                }
                CampusLocation(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    position = LatLng(doc.getDouble("latitude") ?: 0.0, doc.getDouble("longitude") ?: 0.0),
                    description = doc.getString("description") ?: "",
                    type = type,
                    icon = icon,
                    color = color
                )
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
                .set(data, SetOptions.merge()).await()
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

    private fun parseClassTimeForSort(timeText: String): java.time.LocalTime {
        return runCatching {
            java.time.LocalTime.parse(timeText)
        }.getOrDefault(java.time.LocalTime.MAX)
    }
}
