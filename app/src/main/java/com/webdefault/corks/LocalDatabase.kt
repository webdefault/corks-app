package com.webdefault.corks

import android.content.Context
import android.util.Log

import com.webdefault.lib.db.ResultColumns
import com.webdefault.lib.db.ResultLines
import com.webdefault.lib.db.SQLite
import com.webdefault.corks.editor.history.EditItem

import java.io.File
import java.util.LinkedList
import java.util.UUID

import javax.xml.transform.Result

/**
 * Created by orlandoleite on 2/19/18.
 */

class LocalDatabase private constructor(private val context:Context)
{
    private val path:String
    protected lateinit var db:SQLite;
    protected var dbInited:Boolean = false;
    
    private var newCount = 0
    
    private fun setupStructure()
    {
        val file = File(context.filesDir, "home/.corks")
        file.mkdirs()
    }
    
    init
    {
        path = context.filesDir.toString() + "/home/.corks/main.sqlite"
    }
    
    private fun setupDB()
    {
        if(!dbInited)
        {
            db = SQLite(path)
            
            if(db.executeQuery("SELECT DISTINCT tbl_name from sqlite_master where tbl_name = 'settings'")!!.size == 0)
            {
                var sql = ("CREATE TABLE 'settings' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                           + "'name' TEXT, "
                           + "'value_text' TEXT, "
                           + "'value_int' INTEGER DEFAULT 0, "
                           + "'value_real' REAL DEFAULT 0, "
                           + "'created_at' INTEGER)")
                db.executeQuery(sql)
                
                // setIntOption( "table_settings_version", 1, 0, 0, delegate );
                
                sql = "INSERT INTO 'settings' " + "SELECT 1 AS 'id', ? AS 'name', ? AS 'value_text', ? AS 'value_int', ? AS 'value_real', NULL as 'created_at'"
                db.executeQuery(sql, arrayOf("min_table_id", "", "1", "0"))
                
                sql = "INSERT INTO 'settings' " + "SELECT 2 AS 'id', ? AS 'name', ? AS 'value_text', ? AS 'value_int', ? AS 'value_real', NULL as 'created_at'"
                db.executeQuery(sql, arrayOf("max_table_id", "", "99", "0"))
                
                sql = "INSERT INTO 'settings' " + "SELECT 3 AS 'id', ? AS 'name', ? AS 'value_text', ? AS 'value_int', ? AS 'value_real', NULL as 'created_at'"
                db.executeQuery(sql, arrayOf("using_table_id", "", "0", "0"))
                
                
                sql = ("CREATE TABLE 'paths' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                       + "'name' TEXT, "
                       + "'path' TEXT, "
                       + "'type' INTEGER DEFAULT 0, "
                       + "'created_at' INTEGER)")
                db.executeQuery(sql)
                
                sql = ("CREATE TABLE 'open_files' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                       + "'name' TEXT, "
                       + "'path' TEXT, "
                       + "'status' INTEGER DEFAULT 0, "
                       + "'edit_position' INTEGER DEFAULT 0, "
                       + "'uuid' TEXT, "
                       + "'sort' REAL, "
                       + "'opened_at' INTEGER)")
                db.executeQuery(sql)
                
                sql = ("CREATE TABLE 'open_files_edition' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                       + "'open_file_id' TEXT, "
                       + "'start' INTEGER DEFAULT 0, "
                       + "'before' TEXT, "
                       + "'after' REAL)")
                db.executeQuery(sql)
                
                db.executeQuery("PRAGMA user_version = 1")
                
                val directory = File(context.filesDir, OPEN_FILES)
                directory.mkdirs()
            }
        }
        
        updateDB()
    }
    
    fun closeDB()
    {
        if(dbInited)
        {
            db.close()
            server = null
            dbInited = false;
        }
    }
    
    fun resetDB()
    {
        if(dbInited) db.close()
        dbInited = false;
        
        val file = File(path)
        file.delete()
        
        //deleteImages();
        //deleteEtc();
        
    }
    
    fun updateDB()
    {
        val lines = db.executeQuery("PRAGMA user_version")
        val version = Integer.parseInt(lines!![0]["user_version"]!!)
        
        /*when(version)
        {
            else ->
            {
            }
        }*/
    }
    
    @JvmOverloads
    fun intOption(name:String, defaultValue:Int = 0):Int
    {
        val lines = db.executeQuery(
                "SELECT value_int FROM settings WHERE name = ?",
                arrayOf(name))
        
        return if(lines!!.size > 0)
            Integer.parseInt(lines[0]["value_int"]!!)
        else
            defaultValue
    }
    
    fun realOption(name:String, defaultValue:Double?):Double
    {
        val lines = db.executeQuery(
                "SELECT value_real FROM settings WHERE name = ?",
                arrayOf(name))
        
        return if(lines!!.size > 0)
            java.lang.Double.parseDouble(lines[0]["value_real"]!!)
        else
            defaultValue!!
    }
    
    fun textOption(name:String):String?
    {
        // Log.v( LOG_TAG, name + " " + db.executeQuery( "SELECT value_text FROM settings WHERE name = ?", new String[]{ String.valueOf( name ) } ) );
        val lines = db.executeQuery(
                "SELECT value_text FROM settings WHERE name = ?",
                arrayOf(name))
        
        return if(lines!!.size > 0)
            lines[0]["value_text"]
        else
            null
    }
    
    private fun getOptionId(name:String):Int
    {
        val lines = db.executeQuery(
                "SELECT id FROM settings WHERE name = ?",
                arrayOf(name))
        
        return if(lines!!.size > 0)
            Integer.valueOf(lines[0]["id"]!!)
        else
            -1
    }
    
    fun setIntOption(name:String, value:Int):Boolean
    {
        return setOption(name, value.toString(), "int")
    }
    
    fun setRealOption(name:String, value:Double):Boolean
    {
        return setOption(name, value.toString(), "real")
    }
    
    fun setTextOption(name:String, value:String):Boolean
    {
        return setOption(name, value, "text")
    }
    
    private fun setOption(name:String, value:String, columnType:String):Boolean
    {
        db.beginTransaction()
        var success = false
        
        try
        {
            if(getOptionId(name) == -1)
            {
                db.executeQuery(
                        "INSERT INTO 'settings' ( 'name', 'value_$columnType', 'created_at' ) VALUES ( ?, ?, ? )",
                        arrayOf(name, value, currentTime().toString()))
            }
            else
            {
                // Log.v( LOG_TAG, name + "=" + value );
                db.executeQuery(
                        "UPDATE 'settings' SET 'value_$columnType' = ? WHERE name = ?",
                        arrayOf(value, name))
            }
            
            db.setTransactionSuccessful()
            success = true
        }
        finally
        {
            db.endTransaction()
            return success
        }
    }
    
    fun selectDeskPaths():ResultLines?
    {
        return db.executeQuery("SELECT * FROM paths WHERE type = ?",
                arrayOf(PATH_INCLUDE.toString()))
    }
    
    fun canShowPath(path:String):Boolean
    {
        return db.executeQuery("SELECT * FROM paths WHERE type = ? AND path = ?",
                arrayOf(PATH_EXCLUDE.toString(), path))!!.size == 0
    }
    
    fun showPathAtDesk(name:String, path:String)
    {
        db.beginTransaction()
        
        try
        {
            if(db.executeQuery("SELECT * FROM paths WHERE path = ?", arrayOf(path))!!.size == 0)
            {
                db.executeQuery("INSERT INTO 'paths' ('name', 'path', 'type', 'created_at') VALUES ( ?, ?, ?, ? );",
                        arrayOf(name, path, PATH_INCLUDE.toString(), currentTime().toString()))
                db.setTransactionSuccessful()
            }
        }
        finally
        {
            db.endTransaction()
        }
        
    }
    
    fun ignoreThisPath(name:String, path:String)
    {
        db.beginTransaction()
        
        try
        {
            db.executeQuery("INSERT INTO 'paths' ('name', 'path', 'type', 'created_at') VALUES ( ?, ?, ?, ? );",
                    arrayOf(name, path, PATH_EXCLUDE.toString(), currentTime().toString()))
            db.setTransactionSuccessful()
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun removePathFromDesk(path:String)
    {
        db.beginTransaction()
        
        try
        {
            db.executeQuery("DELETE FROM paths WHERE path = ?", arrayOf(path))
            db.setTransactionSuccessful()
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun isPathShowAtDesk(path:String):Boolean
    {
        return db.executeQuery("SELECT id FROM paths WHERE path = ? AND type = ?",
                arrayOf(path, PATH_INCLUDE.toString()))!!.size > 0
    }
    
    fun selectOpenFiles():ResultLines?
    {
        return db.executeQuery("SELECT * FROM open_files ORDER BY sort ASC")
    }
    
    fun openFile(name:String, path:String):ResultColumns?
    {
        Log.v(LOG_TAG, "openFile: $path")
        db.beginTransaction()
        var file:ResultColumns? = null
        
        try
        {
            if(db.executeQuery("SELECT id FROM 'open_files' WHERE path = ?", arrayOf(path))!!.size == 0)
            {
                var lines = db.executeQuery("SELECT sort FROM 'open_files' ORDER BY sort DESC LIMIT 1")
                val sort = if(lines!!.size > 0) java.lang.Double.valueOf(lines[0]["sort"]!!) + 1.0 else 1.0
                val uuid = UUID.randomUUID().toString()
                
                db.executeQuery(
                        "INSERT INTO 'open_files' (name, path, uuid, sort, opened_at) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(name, path, uuid, sort.toString(), currentTime().toString()))
                db.setTransactionSuccessful()
                
                lines = db.executeQuery("SELECT * FROM 'open_files' WHERE path = ?", arrayOf(path))
                file = lines!![0]
            }
        }
        finally
        {
            db.endTransaction()
            return file
        }
    }
    
    fun getOpenFileEdition(openFile:ResultColumns):ResultLines?
    {
        return db.executeQuery(
                "SELECT * FROM 'open_files_edition' WHERE open_file_id = ? ORDER BY id ASC",
                arrayOf<String>(openFile["id"]!!))
    }
    
    fun pauseOpenFile(openFile:ResultColumns, history:LinkedList<EditItem>, position:Int)
    {
        // Save file
        db.beginTransaction()
        
        try
        {
            val lines = db.executeQuery("SELECT id FROM 'open_files' WHERE id = ?", arrayOf<String>(openFile["id"]!!))
            if(lines!!.size > 0)
            {
                // val currentFile = lines[0]
                db.executeQuery(
                        "UPDATE 'open_files' SET name = ?, path = ?, uuid = ?, status = ?, edit_position = ? WHERE id = ?;",
                        arrayOf<String>(openFile["name"]!!, openFile["path"]!!, openFile["uuid"]!!,
                                openFile["status"]!!, "" + position, openFile["id"]!!))
                
                db.setTransactionSuccessful()
            }
        }
        finally
        {
            db.endTransaction()
        }
        
        // Save edition history
        db.beginTransaction()
        
        try
        {
            db.executeQuery("DELETE FROM 'open_files_edition' WHERE open_file_id = ?", arrayOf<String>(openFile["id"]!!))
            
            for(item in history)
            {
                db.executeQuery(
                        "INSERT INTO 'open_files_edition' (open_file_id, start, before, after) VALUES (?, ?, ?, ?)",
                        arrayOf<String>(openFile["id"]!!, "" + item.start, item.before, item.after))
            }
            
            db.setTransactionSuccessful()
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun renameOpenFile(oldPath:String, name:String, path:String):ResultColumns?
    {
        db.beginTransaction()
        var file:ResultColumns? = null
        
        try
        {
            val result = db.executeQuery("SELECT id FROM 'open_files' WHERE path = ?", arrayOf(oldPath))
            if(result!!.size >= 1)
            {
                file = result[0]
                
                db.executeQuery(
                        "UPDATE 'open_files' SET name = ?, path = ? WHERE id = ?",
                        arrayOf<String>(name, path, file["id"]!!))
                db.setTransactionSuccessful()
                
                val lines = db.executeQuery("SELECT * FROM 'open_files' WHERE path = ?", arrayOf(path))
                file = lines!![0]
            }
        }
        finally
        {
            db.endTransaction()
            return file
        }
    }
    
    fun getFileDataFromId(uuid:String?):File?
    {
        var file:File? = null
        try
        {
            val directory = File(context.filesDir, OPEN_FILES)
            file = File(directory, uuid)
        }
        catch(e:Exception)
        {
            e.printStackTrace()
        }
        
        return file
    }
    
    fun closeFile(fileInfo:ResultColumns)
    {
        db.beginTransaction()
        
        try
        {
            val file = getFileDataFromId(fileInfo["uuid"])
            if(file != null && file.exists()) file.delete()
            
            Log.v(LOG_TAG, "closeFile: $fileInfo")
            
            val id = fileInfo["id"]
            db.executeQuery("DELETE FROM 'open_files' WHERE id = ?", arrayOf<String>(id!!))
            db.executeQuery("DELETE FROM 'open_files_edition' WHERE open_file_id = ?", arrayOf<String>(id))
            
            db.setTransactionSuccessful()
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun updateOpenFile(fileInfo:ResultColumns)
    {
        db.beginTransaction()
        
        try
        {
            val lines = db.executeQuery("SELECT id FROM 'open_files' WHERE id = ?", arrayOf<String>(fileInfo["id"]!!))
            if(lines!!.size != 0)
            {
                db.executeQuery(
                        "UPDATE 'open_files' SET name = ?, path = ?, status = ? WHERE id = ?",
                        arrayOf<String>(fileInfo["name"]!!, fileInfo["path"]!!, fileInfo["status"]!!, fileInfo["id"]!!))
                db.setTransactionSuccessful()
            }
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun saveAs(oldPath:String, file:ResultColumns)
    {
        db.beginTransaction()
        
        try
        {
            val lines = db.executeQuery("SELECT id FROM 'open_files' WHERE path = ?", arrayOf(oldPath))
            if(lines!!.size == 0)
            {
                db.executeQuery(
                        "INSERT INTO 'open_files' (name, path, sort, opened_at) VALUES (?, ?, ?, ?)",
                        arrayOf<String>(file["name"]!!, file["path"]!!, file["sort"]!!, currentTime().toString()))
                db.setTransactionSuccessful()
            }
            else
            {
                val oldFile = lines[0]
                db.executeQuery(
                        "UPDATE 'open_files' SET name = ?, path = ?, status = ? WHERE id = ?",
                        arrayOf<String>(file["name"]!!, file["path"]!!, file["status"]!!, oldFile["id"]!!))
                db.setTransactionSuccessful()
            }
        }
        finally
        {
            db.endTransaction()
        }
    }
    
    fun addNewFile():ResultColumns?
    {
        // Log.v( LOG_TAG, "addNew" );
        
        db.beginTransaction()
        var file:ResultColumns? = null
        
        try
        {
            var name:String
            var lines:ResultLines?
            
            do
            {
                var untitled:String = context.getString( R.string.action_file_untitled );
                name = if(newCount == 0) untitled else "$untitled $newCount"
                lines = db.executeQuery("SELECT id FROM 'open_files' WHERE path = ?", arrayOf(name))
                
                newCount++
            }
            while(lines!!.size > 0)
            
            
            lines = db.executeQuery("SELECT sort FROM 'open_files' ORDER BY sort DESC LIMIT 1")
            val sort = if(lines!!.size > 0) java.lang.Double.valueOf(lines[0]["sort"]!!) + 1.0 else 1.0
            val uuid = UUID.randomUUID().toString()
            
            db.executeQuery(
                    "INSERT INTO 'open_files' (name, path, sort, status, uuid, opened_at) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(name, name, "" + sort, "" + NEW_FILE_UNTOUCHED, uuid, currentTime().toString()))
            db.setTransactionSuccessful()
            
            lines = db.executeQuery("SELECT * FROM 'open_files' WHERE path = ?", arrayOf(name))
            file = lines!![0]
        }
        finally
        {
            db.endTransaction()
            return file
        }
    }
    
    companion object
    {
        private val LOG_TAG = "LocalDatabase"
        
        private val PATH_INCLUDE = 1;
        private val PATH_EXCLUDE = 2;
        protected var server:LocalDatabase? = null;
        
        val NEW_FILE_UNTOUCHED = -2
        val NEW_FILE_EDITED = -1
        
        val FILE_SAVED = 0
        val FILE_EDITED = 1
        
        private val OPEN_FILES = "/home/.corks/open_files"
        
        fun getInstance(ctx:Context):LocalDatabase
        {
            if(server == null)
            {
                Log.v(LOG_TAG, "server created")
                server = LocalDatabase(ctx)
                server!!.setupStructure()
                server!!.setupDB()
            }
            
            return server!!;
        }
        
        fun currentTime():Int
        {
            return (System.currentTimeMillis() / 1000).toInt()
        }
    }
}
