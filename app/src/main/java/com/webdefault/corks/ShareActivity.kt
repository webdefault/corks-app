package com.webdefault.corks

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

import org.json.JSONObject

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by orlandoleite on 4/2/18.
 */

class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data
        if (data != null) {
            val value = data.path

            val myIntent = Intent(this, MainActivity::class.java)
            myIntent.putExtra(OPEN_FILE, value)
            startActivity(myIntent)

            finish()

            // launch home Activity (with FLAG_ACTIVITY_CLEAR_TOP) here…
        }
    }

    companion object {
        var OPEN_FILE = "com.webdefault.corks.SharedActivity.OPEN_FILE"
    }
}
