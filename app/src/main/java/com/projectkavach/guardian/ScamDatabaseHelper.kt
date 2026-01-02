package com.projectkavach.guardian
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class ScamDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "scam.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE scams (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, time TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {}

    fun insertThreatAlert(category: String, time: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("category", category)
            put("time", time)
        }
        db.insert("scams", null, values)
    }

    // Ab ye function asli data nikaalega
    fun getRecentThreats(limit: Int): List<ThreatData> {
        val list = mutableListOf<ThreatData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM scams ORDER BY id DESC LIMIT $limit", null)
        
        if (cursor.moveToFirst()) {
            do {
                list.add(ThreatData(
                    cursor.getLong(0), 
                    "Scam Attempt Detected", 
                    cursor.getString(1), 
                    "high", 
                    System.currentTimeMillis(), 
                    90
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getStatistics(): Map<String, Int> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM scams", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return mapOf("Total" to count)
    }
}
