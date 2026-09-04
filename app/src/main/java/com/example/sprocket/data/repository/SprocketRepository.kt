package com.example.sprocket.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.sprocket.data.model.AlertsConfig
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.OdometerReading
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
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

class SprocketRepository(
    context: Context? = null,
    sharedPreferences: SharedPreferences? = null
) {
    private val prefs: SharedPreferences = sharedPreferences
        ?: context?.getSharedPreferences("sprocket_data", Context.MODE_PRIVATE)
        ?: throw IllegalArgumentException("Either context or sharedPreferences must be provided")

    companion object {
        const val CURRENT_DATA_VERSION = 3
        const val KEY_DATA_VERSION = "data_version"
    }

    private var isReadOnly: Boolean = false

    private val defaultParts = listOf(
        Part("belt", "CVT belt", 20000, 48, 0, WearEngine.currentYear, WearEngine.currentMonth, 450000L),
        Part("front", "Front tyre", 20000, 60, 0, WearEngine.currentYear, WearEngine.currentMonth, 340000L),
        Part("rear", "Rear tyre", 12000, 60, 0, WearEngine.currentYear, WearEngine.currentMonth, 385000L),
        Part("oil", "Engine oil + filter", 3000, 12, 0, WearEngine.currentYear, WearEngine.currentMonth, 95000L),
        Part("air", "Air filter", 12000, 24, 0, WearEngine.currentYear, WearEngine.currentMonth, 75000L)
    )

    private val defaultHistory = emptyList<ServiceRecord>()

    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    val allParts: StateFlow<List<Part>> = _parts.asStateFlow()

    private val _activeParts = MutableStateFlow<List<Part>>(emptyList())
    val activeParts: StateFlow<List<Part>> = _activeParts.asStateFlow()

    private val _archivedParts = MutableStateFlow<List<Part>>(emptyList())
    val archivedParts: StateFlow<List<Part>> = _archivedParts.asStateFlow()

    // Screen-facing parts flow defaults to active parts
    val parts: StateFlow<List<Part>> = activeParts

    private val _history = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val history: StateFlow<List<ServiceRecord>> = _history.asStateFlow()

    private val _readings = MutableStateFlow<List<OdometerReading>>(emptyList())
    val readings: StateFlow<List<OdometerReading>> = _readings.asStateFlow()

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.WEAR)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    init {
        loadData()
    }

    private fun updatePartsInternal(newList: List<Part>) {
        _parts.value = newList
        _activeParts.value = newList.filter { !it.isArchived }
        _archivedParts.value = newList.filter { it.isArchived }
    }

    private fun loadStoredDataOrDefaults() {
        val partsJson = prefs.getString("parts", null)
        val historyJson = prefs.getString("history", null)
        val stateJson = prefs.getString("state", null)
        val readingsJson = prefs.getString("readings", null)

        val loadedParts = if (partsJson != null) parseParts(partsJson) else defaultParts
        updatePartsInternal(loadedParts)
        _history.value = if (historyJson != null) parseHistory(historyJson) else defaultHistory
        _vehicleState.value = if (stateJson != null) parseVehicleState(stateJson) else VehicleState()
        _readings.value = if (readingsJson != null) parseReadings(readingsJson) else emptyList()
    }

    private fun loadData() {
        val storedVersion = prefs.getInt(KEY_DATA_VERSION, 0)

        if (storedVersion > CURRENT_DATA_VERSION) {
            isReadOnly = true
            loadStoredDataOrDefaults()
            return
        }

        if (storedVersion == 0) {
            val hasData = prefs.contains("parts") || prefs.contains("state")
            if (!hasData) {
                _vehicleState.value = VehicleState()
                updatePartsInternal(defaultParts)
                _history.value = defaultHistory
                _readings.value = emptyList()
                saveData()
                prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
                return
            }
        }

        if (storedVersion < CURRENT_DATA_VERSION) {
            migrate(storedVersion)
            return
        }

        loadStoredDataOrDefaults()
    }

    private fun migrate(fromVersion: Int) {
        val partsJson = prefs.getString("parts", null)
        val historyJson = prefs.getString("history", null)
        val stateJson = prefs.getString("state", null)
        val readingsJson = prefs.getString("readings", null)

        val parts = if (partsJson != null) parseParts(partsJson) else defaultParts
        val history = if (historyJson != null) parseHistory(historyJson) else defaultHistory
        var state = if (stateJson != null) parseVehicleState(stateJson) else VehicleState()
        var readings = if (readingsJson != null) parseReadings(readingsJson) else emptyList()

        var version = fromVersion
        if (version < 2) {
            version = 2
        }

        if (version == 2) {
            // v2 -> v3 migration
            if (readings.isEmpty() && state.odometerKm > 0) {
                readings = listOf(
                    OdometerReading(
                        id = UUID.randomUUID().toString(),
                        year = if (state.lastReadYear > 0) state.lastReadYear else WearEngine.currentYear,
                        month = if (state.lastReadMonth in 1..12) state.lastReadMonth else WearEngine.currentMonth,
                        odometerKm = state.odometerKm
                    )
                )
            }
            state = state.copy(onboardingComplete = true)
            version = 3
        }

        _vehicleState.value = state
        updatePartsInternal(parts)
        _history.value = history
        _readings.value = readings
        saveData()
        prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
    }

    private fun saveData() {
        if (isReadOnly) return
        prefs.edit()
            .putString("parts", serializeParts(_parts.value))
            .putString("history", serializeHistory(_history.value))
            .putString("state", serializeVehicleState(_vehicleState.value))
            .putString("readings", serializeReadings(_readings.value))
            .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
            .apply()
    }

    fun updateOdometer(
        newKm: Int,
        year: Int = WearEngine.currentYear,
        month: Int = WearEngine.currentMonth
    ) {
        if (newKm <= 0) return
        val current = _vehicleState.value
        _vehicleState.value = current.copy(
            odometerKm = newKm,
            lastReadYear = year,
            lastReadMonth = month
        )

        val currentReadings = _readings.value.toMutableList()
        val existingIndex = currentReadings.indexOfFirst { it.year == year && it.month == month }
        if (existingIndex >= 0) {
            val existing = currentReadings[existingIndex]
            currentReadings[existingIndex] = existing.copy(odometerKm = newKm)
        } else {
            currentReadings.add(
                OdometerReading(
                    id = UUID.randomUUID().toString(),
                    year = year,
                    month = month,
                    odometerKm = newKm
                )
            )
        }
        currentReadings.sortWith(compareBy({ it.year }, { it.month }))
        _readings.value = currentReadings
        saveData()
    }

    fun monthlyAverageKm(): Int {
        val override = _vehicleState.value.monthlyAverageOverrideKm
        if (override != null) return override

        val list = _readings.value
        if (list.size < 2) return 560

        val sorted = list.sortedWith(compareBy({ it.year }, { it.month }))
        val earliest = sorted.first()
        val latest = sorted.last()
        val monthsSpanned = (latest.year - earliest.year) * 12 + (latest.month - earliest.month)
        if (monthsSpanned <= 0) return 560

        val distanceDelta = maxOf(0, latest.odometerKm - earliest.odometerKm)
        return (distanceDelta.toDouble() / monthsSpanned).roundToInt()
    }

    fun monthlyAverageReadingsCount(): Int {
        return if (_vehicleState.value.monthlyAverageOverrideKm != null) 0 else _readings.value.size
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

        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) {
                    it.copyWithUpdatedService(odoKm, year, month)
                } else {
                    it
                }
            }
        )

        _history.value = listOf(record) + _history.value
        saveData()
    }

    fun updateInterval(partId: String, newKm: Int?, newMo: Int?) {
        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) it.copyWithInterval(newKm, newMo) else it
            }
        )
        saveData()
    }

    fun snoozePart(partId: String, days: Int = 14) {
        val expiry = LocalDate.now().plusDays(days.toLong()).toEpochDay()
        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) it.copy(snoozedUntilEpochDay = expiry) else it
            }
        )
        saveData()
    }

    fun unsnoozePart(partId: String) {
        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) it.copy(snoozedUntilEpochDay = null) else it
            }
        )
        saveData()
    }

    fun archivePart(partId: String) {
        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) it.copy(isArchived = true) else it
            }
        )
        saveData()
    }

    fun unarchivePart(partId: String) {
        updatePartsInternal(
            _parts.value.map {
                if (it.id == partId) it.copy(isArchived = false) else it
            }
        )
        saveData()
    }

    fun deletePartAndHistory(partId: String) {
        updatePartsInternal(_parts.value.filter { it.id != partId })
        _history.value = _history.value.filter { it.partId != partId }
        saveData()
    }

    fun deletePart(partId: String) {
        archivePart(partId)
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
            standardCost = standardCost,
            snoozedUntilEpochDay = null,
            isArchived = false
        )
        updatePartsInternal(_parts.value + newPart)
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

    fun setReminderDayOfMonth(day: Int) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(reminderDayOfMonth = day.coerceIn(1, 31))
        saveData()
    }

    fun setReminderDay(day: String) {
        val dayInt = when (day) {
            "1st" -> 1
            "15th" -> 15
            "Last" -> 28
            else -> day.filter { it.isDigit() }.toIntOrNull() ?: 1
        }
        setReminderDayOfMonth(dayInt)
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

    fun setCurrencyCode(currencyCode: String) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(currencyCode = currencyCode)
        saveData()
    }

    fun setThemePreference(pref: String) {
        val cur = _vehicleState.value
        _vehicleState.value = cur.copy(themePreference = pref)
        saveData()
    }

    fun clearAll() {
        updatePartsInternal(emptyList())
        _history.value = emptyList()
        _readings.value = emptyList()
        _vehicleState.value = VehicleState(onboardingComplete = false)
        saveData()
    }

    fun loadSampleGarage() {
        val y = WearEngine.currentYear
        val m = WearEngine.currentMonth
        val sampleParts = defaultParts.map {
            it.copy(lastKm = 40000, lastYear = y, lastMonth = m)
        }
        updatePartsInternal(sampleParts)
        _history.value = defaultHistory
        _readings.value = listOf(
            OdometerReading(
                id = UUID.randomUUID().toString(),
                year = y,
                month = m,
                odometerKm = 40000
            )
        )
        _vehicleState.value = VehicleState(
            vehicleName = "NMAX '16",
            odometerKm = 40000,
            lastReadYear = y,
            lastReadMonth = m,
            onboardingComplete = true
        )
        saveData()
    }

    fun resetToDefaults() {
        clearAll()
    }

    // Export / Import
    fun exportJson(): String {
        val root = JSONObject()
        root.put("version", CURRENT_DATA_VERSION)
        root.put("vehicle", serializeVehicleStateObject(_vehicleState.value))
        root.put("parts", serializePartsArray(_parts.value))
        root.put("history", serializeHistoryArray(_history.value))
        root.put("readings", serializeReadingsArray(_readings.value))
        return root.toString()
    }

    fun importJson(json: String): Result<Unit> {
        return runCatching {
            val root = JSONObject(json)
            val version = root.getInt("version")
            if (version < 1 || version > CURRENT_DATA_VERSION) {
                throw IllegalArgumentException("Unsupported data version: $version")
            }
            val vehicleObj = root.getJSONObject("vehicle")
            val partsArr = root.getJSONArray("parts")
            val historyArr = root.getJSONArray("history")
            val readingsArr = root.getJSONArray("readings")

            val parsedVehicle = parseVehicleStateObject(vehicleObj)
            val parsedParts = parsePartsArray(partsArr)
            val parsedHistory = parseHistoryArray(historyArr)
            val parsedReadings = parseReadingsArray(readingsArr)

            _vehicleState.value = parsedVehicle
            updatePartsInternal(parsedParts)
            _history.value = parsedHistory
            _readings.value = parsedReadings.sortedWith(compareBy({ it.year }, { it.month }))
            saveData()
        }
    }

    // JSON Serializers
    private fun serializePartsArray(list: List<Part>): JSONArray {
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
            obj.put("snoozedUntilEpochDay", p.snoozedUntilEpochDay ?: -1L)
            obj.put("isArchived", p.isArchived)
            arr.put(obj)
        }
        return arr
    }

    private fun serializeParts(list: List<Part>): String = serializePartsArray(list).toString()

    private fun parsePartsArray(arr: JSONArray): List<Part> {
        val list = mutableListOf<Part>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val km = obj.optInt("intervalKm", -1).let { if (it == -1) null else it }
            val mo = obj.optInt("intervalMonths", -1).let { if (it == -1) null else it }
            val snoozedUntil: Long? = if (obj.has("snoozedUntilEpochDay") && !obj.isNull("snoozedUntilEpochDay")) {
                val epoch = obj.getLong("snoozedUntilEpochDay")
                if (epoch == -1L) null else epoch
            } else if (obj.optBoolean("isSnoozed", false)) {
                LocalDate.now().minusDays(1).toEpochDay()
            } else {
                null
            }
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
                    snoozedUntilEpochDay = snoozedUntil,
                    isArchived = obj.optBoolean("isArchived", false)
                )
            )
        }
        return list
    }

    private fun parseParts(json: String): List<Part> = parsePartsArray(JSONArray(json))

    private fun serializeHistoryArray(list: List<ServiceRecord>): JSONArray {
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
        return arr
    }

    private fun serializeHistory(list: List<ServiceRecord>): String = serializeHistoryArray(list).toString()

    private fun parseHistoryArray(arr: JSONArray): List<ServiceRecord> {
        val list = mutableListOf<ServiceRecord>()
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

    private fun parseHistory(json: String): List<ServiceRecord> = parseHistoryArray(JSONArray(json))

    private fun serializeReadingsArray(list: List<OdometerReading>): JSONArray {
        val arr = JSONArray()
        for (r in list) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("year", r.year)
            obj.put("month", r.month)
            obj.put("odometerKm", r.odometerKm)
            arr.put(obj)
        }
        return arr
    }

    private fun serializeReadings(list: List<OdometerReading>): String = serializeReadingsArray(list).toString()

    private fun parseReadingsArray(arr: JSONArray): List<OdometerReading> {
        val list = mutableListOf<OdometerReading>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                OdometerReading(
                    id = obj.getString("id"),
                    year = obj.getInt("year"),
                    month = obj.getInt("month"),
                    odometerKm = obj.getInt("odometerKm")
                )
            )
        }
        return list
    }

    private fun parseReadings(json: String): List<OdometerReading> = parseReadingsArray(JSONArray(json))

    private fun serializeVehicleStateObject(state: VehicleState): JSONObject {
        val obj = JSONObject()
        obj.put("vehicleName", state.vehicleName)
        obj.put("odometerKm", state.odometerKm)
        obj.put("lastReadYear", state.lastReadYear)
        obj.put("lastReadMonth", state.lastReadMonth)
        obj.put("monthlyAverageOverrideKm", state.monthlyAverageOverrideKm ?: -1)
        obj.put("unit", state.unit.name)
        obj.put("currencyCode", state.currencyCode)
        obj.put("soonThreshold", state.soonThreshold.toDouble())
        obj.put("reminderDayOfMonth", state.reminderDayOfMonth)
        obj.put("remindersEnabled", state.remindersEnabled)
        obj.put("themePreference", state.themePreference)
        obj.put("onboardingComplete", state.onboardingComplete)
        val alertsObj = JSONObject()
        alertsObj.put("overdue", state.alerts.overdue)
        alertsObj.put("soon", state.alerts.soon)
        alertsObj.put("recap", state.alerts.recap)
        obj.put("alerts", alertsObj)
        return obj
    }

    private fun serializeVehicleState(state: VehicleState): String = serializeVehicleStateObject(state).toString()

    private fun parseVehicleStateObject(obj: JSONObject): VehicleState {
        val alertsObj = obj.optJSONObject("alerts")
        val alerts = AlertsConfig(
            overdue = alertsObj?.optBoolean("overdue", true) ?: true,
            soon = alertsObj?.optBoolean("soon", true) ?: true,
            recap = alertsObj?.optBoolean("recap", false) ?: false
        )
        val overrideKm = if (obj.has("monthlyAverageOverrideKm") && !obj.isNull("monthlyAverageOverrideKm")) {
            val v = obj.getInt("monthlyAverageOverrideKm")
            if (v == -1) null else v
        } else {
            null
        }

        val reminderDayNum = if (obj.has("reminderDayOfMonth")) {
            obj.optInt("reminderDayOfMonth", 1)
        } else if (obj.has("reminderDay")) {
            val d = obj.optString("reminderDay", "1st")
            when (d) {
                "1st" -> 1
                "15th" -> 15
                "Last" -> 28
                else -> 1
            }
        } else {
            1
        }

        return VehicleState(
            vehicleName = obj.optString("vehicleName", ""),
            odometerKm = obj.optInt("odometerKm", 0),
            lastReadYear = obj.optInt("lastReadYear", 0),
            lastReadMonth = obj.optInt("lastReadMonth", 0),
            monthlyAverageOverrideKm = overrideKm,
            unit = DistanceUnit.valueOf(obj.optString("unit", DistanceUnit.KM.name)),
            currencyCode = obj.optString("currencyCode", "IDR"),
            soonThreshold = obj.optDouble("soonThreshold", 0.80).toFloat(),
            reminderDayOfMonth = reminderDayNum,
            remindersEnabled = obj.optBoolean("remindersEnabled", true),
            alerts = alerts,
            themePreference = obj.optString("themePreference", "SYSTEM"),
            onboardingComplete = obj.optBoolean("onboardingComplete", false)
        )
    }

    private fun parseVehicleState(json: String): VehicleState = parseVehicleStateObject(JSONObject(json))
}
