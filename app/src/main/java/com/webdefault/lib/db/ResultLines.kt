package com.webdefault.lib.db

import org.json.JSONArray
import org.json.JSONException

import java.util.ArrayList

class ResultLines : ArrayList<ResultColumns> {

    val json: JSONArray
        get() {
            val array = JSONArray()

            for (columns in this) {
                array.put(columns.json)
            }

            return array
        }

    constructor() : super() {}

    constructor(collection: Collection<ResultColumns>) : super(collection) {}

    constructor(capacity: Int) : super(capacity) {}

    constructor(array: JSONArray) {
        try {
            for (i in 0 until array.length()) {
                add(ResultColumns(array.getJSONObject(i)))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }
}
