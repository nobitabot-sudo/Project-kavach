package com.projectkavach.guardian
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ScamDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "scam.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE scams (id INTEGER PRIMARY KEY, category TEXT, time TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {}
}
