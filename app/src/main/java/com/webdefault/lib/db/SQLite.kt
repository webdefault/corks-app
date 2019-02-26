package com.webdefault.lib.db

import android.database.sqlite.SQLiteDatabase

class SQLite(databasePath: String) {
    private var db: SQLiteDatabase;
    private var dbInited: Boolean = false;

    init {
        db = SQLiteDatabase.openOrCreateDatabase(databasePath, null)
        dbInited = true;
        /*db = SQLiteDatabase.openDatabase( databasePath, null,
				SQLiteDatabase.NO_LOCALIZED_COLLATORS );*/
    }

    fun beginTransaction() {
        db.beginTransaction()
    }

    fun setTransactionSuccessful() {
        db.setTransactionSuccessful()
    }

    fun endTransaction() {
        db.endTransaction()
    }

    @JvmOverloads
    fun executeQuery(query: String, values: Array<String>? = null): ResultLines? {
        var result: ResultLines? = null

        if (dbInited) {

            val cursor = db.rawQuery(query, values)

            result = ResultLines()

            if (cursor.moveToFirst()) {
                val totalRows = cursor.columnCount

                do {
                    val obj = ResultColumns()

                    for (i in 0 until totalRows) {
                        obj[cursor.getColumnName(i)] = cursor.getString(i)
                    }

                    result.add(obj)
                } while (cursor.moveToNext())
            }

            cursor.close()
        } else {
            println("EasySQL: db is not connected.")
        }

        return result
    }

    fun close() {
        if (dbInited) db.close()
        dbInited = false;
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (dbInited) db.close()
        dbInited = false;
    }
}
