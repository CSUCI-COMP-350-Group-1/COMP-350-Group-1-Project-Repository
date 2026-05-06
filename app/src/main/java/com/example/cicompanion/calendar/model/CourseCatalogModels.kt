package com.example.cicompanion.calendar.model

// Static course catalog models loaded from the bundled JSON asset.
data class CourseCatalogRoot(
    val schemaVersion: Int,
    val catalogName: String,
    val majors: List<CourseCatalogMajor>
)

data class CourseCatalogMajor(
    val code: String,
    val name: String,
    val courses: List<CourseCatalogCourse>
)

data class CourseCatalogCourse(
    val code: String,
    val title: String,
    val typicallyOffered: String
)