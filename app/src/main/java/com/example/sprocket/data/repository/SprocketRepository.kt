package com.example.sprocket.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.sprocket.data.model.AlertsConfig
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import com.example.sprocket.data.model.SortMode
import com.example.sprocket.data.model.VehicleState
import com.example.sprocket.domain.WearEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SprocketRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sprocket_data", Context.MODE_PRIVATE)

    companion object {
        private const val CURRENT_DATA_VERSION = 2
        private const val KEY_DATA_VERSION = "data_version"
    }

    private val defaultParts = listOf(
        Part("belt", "CVT belt", 20000, 48, 0, WearEngine.currentYear, WearEngine.currentMonth, 450000L),
        Part("front", "Front tyre", 20000, 60, 0, WearEngine.currentYear, WearEngine.currentMonth, 340000L),
        Part("rear", "Rear tyre", 12000, 60, 0, WearEngine.currentYear, WearEngine.currentMonth, 385000L),
        Part("oil", "Engine oil + filter", 3000, 12, 0, WearEngine.currentYear, WearEngine.currentMonth, 95000L),
        Part("air", "Air filter", 12000, 24, 0, WearEngine.currentYear, WearEngine.currentMonth, 75000L)
    )

    private val defaultHistory = emptyList<ServiceRecord>()

    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    val parts: StateFlow<List<Part>> = _parts.asStateFlow()

    private val _history = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val history: StateFlow<List<ServiceRecord>> = _history.asStateFlow()

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.WEAR)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val storedVersion = prefs.getInt(KEY_DATA_VERSION, 1)
        if (storedVersion < CURRENT_DATA_VERSION) {
            _parts.value = defaultParts
            _history.value = defaultHistory
            _vehicleState.value = VehicleState()
            saveData()
            prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
            return
        }

        val partsJson = prefs.getString("parts", null)
        val historyJson = prefs.getString("history", null)
        val stateJson = prefs.getString("state", null)

        _parts.value = if (partsJson != null) parseParts(partsJson) else defaultParts
        _history.value = if (historyJson != null) parseHistory(historyJson) else defaultHistory
        _vehicleState.value = if (stateJson != null) parseVehicleState(stateJson) else VehicleState()
    }

    private fun saveData() {
        prefs.edit()
            .putString("parts", serializeParts(_parts.value))
            .putString("history", serializeHistory(_history.value))
            .putString("state", serializeVehicleState(_vehicleState.value))
            .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
            .apply()
    }

    fun updateOdometer(newKm: Int) {
        if (newKm <= 0) return
        val current = _vehicleState.value

        _vehicleState.value = current.copy(
            prevOdometerKm = current.odometerKm,
            odometerKm = newKm,
            lastReadYear = WearEngine.currentYear,
            lastReadMonth = WearEngine.currentMonth
        )
        saveData()
    }

    fun logReplacement(
        partId: String,
        cost: Long,
        performer: String,
        odoKm: Int = _vehicleState.value.odometerKm,
        customNote: String = "",
        year: Int = WearEngine.currentYear,
        month: Int = WearEngine.currentMonth
    ) {
        val part = _parts.value.find { it.id == partId } ?: return

        val note = if (customNote.isNotBlank()) customNote else "${part.name}, ${performer.lowercase()}"
        val record = ServiceRecord(
            id = UUID.randomUUID().toString(),
            partId = partId,
            year = year,
            month = month,
            odometerKm = odoKm,
            cost = cost,
            performer = performer,
            note = note
        )

        _parts.value = _parts.value.map {
            if (it.id == partId) {
                it.copyWithUpdatedService(odoKm, year, month)
            } else {
                it
            }
        }

        _history.value = listOf(record) + _history.value
        saveData()
    }

    fun updateInterval(partId: String, newKm: Int?, newMo: Int?) {
        _parts.value = _parts.value.map {
            if (it.id == partId) it.copyWithInterval(newKm, newMo) else it
        }
        saveData()
    }

    fun snoozePart(partId: String) {
        _parts.value = _parts.value.map {
            if (it.id == partId) it.copy(isSnoozed = true) else it
        }
        saveData()
    }

    fun deletePart(partId: String) {
        _parts.value = _parts.value.filter { it.id != partId }
        _history.value = _history.value.filter { it.partId != partId }
        saveData()
    }

    fun addPart(name: String, intervalKm: Int?, intervalMonths: Int?, standardCost: Long) {
        val id = UUID.randomUUID().toString()
        val curState = _vehicleState.value
        val newPart = Part(
            id = id,
            name = name,
            intervalKm = intervalKm,
            intervalMonths = intervalMonths,
            lastKm = curState.odometerKm,
            lastYear = WearEngine.currentYear,
            lastMonth = WearEngine.currentMonth,
            standardCost = standardCost
        )
        _parts.value = _parts.value + newPart
        saveData()
    }

    fun cycleSort() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.WEAR -> SortMode.NAME
            SortMode.NAME -> SortMode.INTERVAL
            SortMode.INTERVAL -> SortMode.WEAR
        }
    }

    fun toggleReminders() {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(remindersEnabled = !cur.remindersEnabled)
        saveData()
    }

    fun setReminderDay(day: String) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(reminderDay = day)
        saveData()
    }

    fun toggleAlert(type: String) {
        val cur = _vehicleState.value
        val newAlerts = when (type) {
            "over" -> cur.alerts.copy(overdue = !cur.alerts.overdue)
            "soon" -> cur.alerts.copy(soon = !cur.alerts.soon)
            "recap" -> cur.alerts.copy(recap = !cur.alerts.recap)
            else -> cur.alerts
        }
        _vehicleState.value = cur.copy(alerts = newAlerts)
        saveData()
    }

    fun setUnit(unit: DistanceUnit) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(unit = unit)
        saveData()
    }

    fun setThemePreference(pref: String) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(themePreference = pref)
        saveData()
    }

    fun resetToDefaults() {
        _parts.value = defaultParts
        _history.value = defaultHistory
        _vehicleState.value = VehicleState()
        saveData()
    }

    // JSON Serializers
    private fun serializeParts(list: List<Part>): String {
        val arr = JSONArray()
        for (p in list) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("intervalKm", p.intervalKm ?: -1)
            obj.put("intervalMonths", p.intervalMonths ?: -1)
            obj.put("lastKm", p.lastKm)
            obj.put("lastYear", p.lastYear)
            obj.put("lastMonth", p.lastMonth)
            obj.put("standardCost", p.standardCost)
            obj.put("isSnoozed", p.isSnoozed)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseParts(json: String): List<Part> {
        val list = mutableListOf<Part>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val km = obj.optInt("intervalKm", -1).let { if (it == -1) null else it }
            val mo = obj.optInt("intervalMonths", -1).let { if (it == -1) null else it }
            list.add(
                Part(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    intervalKm = km,
                    intervalMonths = mo,
                    lastKm = obj.getInt("lastKm"),
                    lastYear = obj.getInt("lastYear"),
                    lastMonth = obj.getInt("lastMonth"),
                    standardCost = obj.getLong("standardCost"),
                    isSnoozed = obj.optBoolean("isSnoozed", false)
                )
            )
        }
        return list
    }

    private fun serializeHistory(list: List<ServiceRecord>): String {
        val arr = JSONArray()
        for (h in list) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("partId", h.partId)
            obj.put("year", h.year)
            obj.put("month", h.month)
            obj.put("odometerKm", h.odometerKm)
            obj.put("cost", h.cost)
            obj.put("performer", h.performer)
            obj.put("note", h.note)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseHistory(json: String): List<ServiceRecord> {
        val list = mutableListOf<ServiceRecord>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                ServiceRecord(
                    id = obj.getString("id"),
                    partId = obj.getString("partId"),
                    year = obj.getInt("year"),
                    month = obj.getInt("month"),
                    odometerKm = obj.getInt("odometerKm"),
                    cost = obj.getLong("cost"),
                    performer = obj.getString("performer"),
                    note = obj.getString("note")
                )
            )
        }
        return list
    }

    private fun serializeVehicleState(state: VehicleState): String {
        val obj = JSONObject()
        obj.put("vehicleName", state.vehicleName)
        obj.put("odometerKm", state.odometerKm)
        obj.put("prevOdometerKm", state.prevOdometerKm)
        obj.put("lastReadYear", state.lastReadYear)
        obj.put("lastReadMonth", state.lastReadMonth)
        obj.put("monthlyAverageKm", state.monthlyAverageKm)
        obj.put("unit", state.unit.name)
        obj.put("soonThreshold", state.soonThreshold.toDouble())
        obj.put("reminderDay", state.reminderDay)
        obj.put("remindersEnabled", state.remindersEnabled)
        obj.put("themePreference", state.themePreference)
        val alertsObj = JSONObject()
        alertsObj.put("overdue", state.alerts.overdue)
        alertsObj.put("soon", state.alerts.soon)
        alertsObj.put("recap", state.alerts.recap)
        obj.put("alerts", alertsObj)
        return obj.toString()
    }

    private fun parseVehicleState(json: String): VehicleState {
        val obj = JSONObject(json)
        val alertsObj = obj.optJSONObject("alerts")
        val alerts = AlertsConfig(
            overdue = alertsObj?.optBoolean("overdue", true) ?: true,
            soon = alertsObj?.optBoolean("soon", true) ?: true,
            recap = alertsObj?.optBoolean("recap", false) ?: false
        )
        return VehicleState(
            vehicleName = obj.optString("vehicleName", "NMAX '16"),
            odometerKm = obj.optInt("odometerKm", 0),
            prevOdometerKm = obj.optInt("prevOdometerKm", 0),
            lastReadYear = obj.optInt("lastReadYear", WearEngine.currentYear),
            lastReadMonth = obj.optInt("lastReadMonth", WearEngine.currentMonth),
            monthlyAverageKm = obj.optInt("monthlyAverageKm", 560),
            unit = DistanceUnit.valueOf(obj.optString("unit", DistanceUnit.KM.name)),
            soonThreshold = obj.optDouble("soonThreshold", 0.80).toFloat(),
            reminderDay = obj.optString("reminderDay", "1st"),
            remindersEnabled = obj.optBoolean("remindersEnabled", true),
            alerts = alerts,
            themePreference = obj.optString("themePreference", "SYSTEM")
        )
    }
}
