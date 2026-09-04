package com.example.sprocket.testutil

import android.content.SharedPreferences

class FakeSharedPreferences(
    initialData: Map<String, Any?> = emptyMap()
) : SharedPreferences {

    private val data = initialData.toMutableMap()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? {
        return (data[key] as? String) ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return (data[key] as? MutableSet<String>) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return (data[key] as? Number)?.toInt() ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return (data[key] as? Number)?.toLong() ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return (data[key] as? Number)?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return (data[key] as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listeners.remove(listener)
    }

    inner class EditorImpl : SharedPreferences.Editor {
        private val pendingChanges = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pendingChanges[key] = this // Sentinel for remove
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                data.clear()
                clearRequested = false
            }
            for ((key, value) in pendingChanges) {
                if (value === this) {
                    data.remove(key)
                } else {
                    data[key] = value
                }
                listeners.forEach { it.onSharedPreferenceChanged(this@FakeSharedPreferences, key) }
            }
            pendingChanges.clear()
        }
    }
}
