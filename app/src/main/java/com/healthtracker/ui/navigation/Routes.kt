package com.healthtracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object PeopleRoute

/** [personId] of 0 means "add a new person". */
@Serializable
data class PersonEditRoute(val personId: Long = 0)

@Serializable
data class PersonProfileRoute(val personId: Long)

/** Either [recordId] is set (edit) or [type] is set (add). [type] is a RecordType name. */
@Serializable
data class RecordEditRoute(val personId: Long, val type: String? = null, val recordId: Long = 0)

@Serializable
data class RecordDetailRoute(val recordId: Long)

@Serializable
object SearchRoute

/** [noteId] of 0 means "add a new note". */
@Serializable
data class NoteEditRoute(val personId: Long, val noteId: Long = 0)

@Serializable
data class NoteDetailRoute(val noteId: Long)

/** [medicationId] of 0 means "add a new medication". */
@Serializable
data class MedicationEditRoute(val personId: Long, val medicationId: Long = 0)

@Serializable
data class MedicationDetailRoute(val medicationId: Long)
