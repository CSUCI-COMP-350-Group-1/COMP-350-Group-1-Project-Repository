package com.example.cicompanion.calendar.data.repository

import android.content.Context
import com.example.cicompanion.calendar.model.CourseCatalogCourse
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.CourseCatalogRoot
import org.json.JSONObject


// Loads the static CSUCI course catalog from app/src/main/assets
class CourseCatalogRepository(
    private val context: Context
) {
    fun loadCatalog(): CourseCatalogRoot {
        val jsonText = context.assets
            .open(CATALOG_FILE_NAME)
            .bufferedReader()
            .use { it.readText() }

        val rootObject = JSONObject(jsonText)
        val majorArray = rootObject.getJSONArray("majors")

        val majors = buildList {
            for (index in 0 until majorArray.length()) {
                val majorObject = majorArray.getJSONObject(index)
                val courseArray = majorObject.getJSONArray("courses")

                val courses = buildList {
                    for (courseIndex in 0 until courseArray.length()) {
                        val courseObject = courseArray.getJSONObject(courseIndex)
                        add(
                            CourseCatalogCourse(
                                code = courseObject.optString("code"),
                                title = courseObject.optString("title"),
                                typicallyOffered = courseObject.optString("typicallyOffered")
                            )
                        )
                    }
                }

                add(
                    CourseCatalogMajor(
                        code = majorObject.optString("code"),
                        name = majorObject.optString("name"),
                        courses = courses
                    )
                )
            }
        }

        return CourseCatalogRoot(
            schemaVersion = rootObject.optInt("schemaVersion", 1),
            catalogName = rootObject.optString("catalogName", "CSUCI Course Catalog"),
            majors = majors
        )
    }

    fun loadMajors(): List<CourseCatalogMajor> {
        return loadCatalog().majors
    }

    private companion object {
        const val CATALOG_FILE_NAME = "csuci_courses_catalog.json"
    }
}