package com.webdefault.lib.db

import org.json.JSONException
import org.json.JSONObject

import java.util.Comparator
import java.util.SortedMap
import java.util.TreeMap

class ResultColumns : TreeMap<String, String> {

    val json: JSONObject
        get() = JSONObject(this)

    constructor() : super() {
        // TODO Auto-generated constructor stub
    }

    constructor(key: String, value: String) : super() {
        put(key, value)
    }

    constructor(vararg args: String) : super() {

        var i = 0
        while (i < args.size) {
            put(args[i], args[i + 1])
            i += 2
        }
    }

    constructor(`object`: JSONObject) : super() {

        val iter = `object`.keys()
        while (iter.hasNext()) {
            val key = iter.next()
            try {
                put(key as String, `object`.getString(key))
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
    }

    constructor(comparator: Comparator<in String>) : super(comparator) {
        // TODO Auto-generated constructor stub
    }

    constructor(map: Map<out String, String>) : super(map) {
        // TODO Auto-generated constructor stub
    }

    constructor(map: SortedMap<String, out String>) : super(map) {
        // TODO Auto-generated constructor stub
    }
}
