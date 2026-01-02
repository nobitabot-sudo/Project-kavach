package com.projectkavach.guardian
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ScamDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "scam.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE scams (id INTEGER PRIMARY KEY, category TEXT, time TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {}

    fun insertThreatAlert(category: String, time: String) {
        writableDatabase.execSQL("INSERT INTO scams (category, time) VALUES ('$category', '$time')")
    }

    fun getStatistics(): Map<String, Int> = mapOf("Total" to 0)
    fun getRecentThreats(): List<String> = emptyList()
    fun getThreatsByCategory(): Map<String, Int> = emptyMap()
}
