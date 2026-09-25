package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EVTransaction
import com.example.data.model.GridState
import com.example.data.model.MLMetrics
import com.example.data.model.ScheduleSummary
import com.example.data.model.ScheduledEV
import com.example.data.model.StationAlert
import com.example.data.remote.AudioPlayerHelper
import com.example.data.remote.GeminiService
import com.example.data.simulation.IoTSimulator
import com.example.domain.ml.EdgeMLPredictor
import com.example.domain.optimizer.EVOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EVStationViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayerHelper(application.applicationContext)

    // Configuration Sliders
    private val _numEVs = MutableStateFlow(8)
    val numEVs: StateFlow<Int> = _numEVs.asStateFlow()

    private val _stationCapacityKw = MutableStateFlow(250.0f)
    val stationCapacityKw: StateFlow<Float> = _stationCapacityKw.asStateFlow()

    private val _numChargers = MutableStateFlow(6)
    val numChargers: StateFlow<Int> = _numChargers.asStateFlow()

    private val _gridLoadPct = MutableStateFlow(68.0f)
    val gridLoadPct: StateFlow<Float> = _gridLoadPct.asStateFlow()

    private val _maxChargerPower = MutableStateFlow(100.0f)
    val maxChargerPower: StateFlow<Float> = _maxChargerPower.asStateFlow()

    // Data and Results
    private val _evList = MutableStateFlow<List<EVTransaction>>(emptyList())
    val evList: StateFlow<List<EVTransaction>> = _evList.asStateFlow()

    private val _gridState = MutableStateFlow(
        IoTSimulator.computeGridState(250.0f, 68.0f, 6)
    )
    val gridState: StateFlow<GridState> = _gridState.asStateFlow()

    private val _scheduledEVs = MutableStateFlow<List<ScheduledEV>>(emptyList())
    val scheduledEVs: StateFlow<List<ScheduledEV>> = _scheduledEVs.asStateFlow()

    private val _scheduleSummary = MutableStateFlow(
        ScheduleSummary(0, 0, 0f, 250f, 0f, 6)
    )
    val scheduleSummary: StateFlow<ScheduleSummary> = _scheduleSummary.asStateFlow()

    private val _alerts = MutableStateFlow<List<StationAlert>>(emptyList())
    val alerts: StateFlow<List<StationAlert>> = _alerts.asStateFlow()

    private val _mlMetrics = MutableStateFlow(
        MLMetrics(0f, 0f, 0f, emptyList())
    )
    val mlMetrics: StateFlow<MLMetrics> = _mlMetrics.asStateFlow()

    private val _isModelRetrained = MutableStateFlow(false)
    val isModelRetrained: StateFlow<Boolean> = _isModelRetrained.asStateFlow()

    // Gemini TTS & Search state
    private val _isSpeakingAudio = MutableStateFlow(false)
    val isSpeakingAudio: StateFlow<Boolean> = _isSpeakingAudio.asStateFlow()

    private val _ttsMessage = MutableStateFlow<String?>(null)
    val ttsMessage: StateFlow<String?> = _ttsMessage.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _groundingResult = MutableStateFlow<GeminiService.SearchGroundingResult?>(null)
    val groundingResult: StateFlow<GeminiService.SearchGroundingResult?> = _groundingResult.asStateFlow()

    // Navigation Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        generateEVData()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateNumEVs(count: Int) {
        _numEVs.value = count
        generateEVData()
    }

    fun updateStationCapacity(cap: Float) {
        _stationCapacityKw.value = cap
        recomputeGridAndSchedule()
    }

    fun updateNumChargers(count: Int) {
        _numChargers.value = count
        recomputeGridAndSchedule()
    }

    fun updateGridLoad(load: Float) {
        _gridLoadPct.value = load
        recomputeGridAndSchedule()
    }

    fun updateMaxChargerPower(power: Float) {
        _maxChargerPower.value = power
        generateEVData()
    }

    fun generateEVData() {
        val evs = IoTSimulator.generateEVs(
            numEVs = _numEVs.value,
            maxChargerPower = _maxChargerPower.value
        )
        _evList.value = evs
        recomputeGridAndSchedule()
        evaluateML(evs)
    }

    fun recomputeGridAndSchedule() {
        val currentEvs = _evList.value
        val gState = IoTSimulator.computeGridState(
            stationCapacityKw = _stationCapacityKw.value,
            baseGridLoadPct = _gridLoadPct.value,
            totalChargers = _numChargers.value
        )
        _gridState.value = gState

        val (scheduled, summary) = EVOptimizer.optimizeSchedule(currentEvs, gState)
        _scheduledEVs.value = scheduled
        _scheduleSummary.value = summary

        val generatedAlerts = EVOptimizer.generateAlerts(scheduled, gState, summary)
        _alerts.value = generatedAlerts
    }

    fun retrainAIModel() {
        viewModelScope.launch {
            _isModelRetrained.value = true
            evaluateML(_evList.value)
        }
    }

    private fun evaluateML(evs: List<EVTransaction>) {
        val metrics = EdgeMLPredictor.evaluateOnEVs(evs, _gridLoadPct.value)
        _mlMetrics.value = metrics
    }

    fun resetSimulation() {
        _numEVs.value = 8
        _stationCapacityKw.value = 250.0f
        _numChargers.value = 6
        _gridLoadPct.value = 68.0f
        _maxChargerPower.value = 100.0f
        _isModelRetrained.value = false
        generateEVData()
    }

    /**
     * Broadcasts live station status using Gemini 3.8 Flash TTS with voice audio.
     */
    fun speakStationStatusBriefing() {
        val summary = _scheduleSummary.value
        val gState = _gridState.value
        val topAlert = _alerts.value.firstOrNull { it.level != com.example.data.model.AlertLevel.NORMAL }

        val briefingText = StringBuilder().apply {
            append("Edge AI Smart Charging Station Status Report. ")
            append("Grid load is currently at ${gState.currentGridLoadPct.toInt()} percent. ")
            append("Allocated station power is ${summary.allocatedPowerKw.toInt()} kilowatts out of ${gState.availableGridPowerKw.toInt()} kilowatts available. ")
            append("${summary.totalActiveCharging} electric vehicles are actively charging across ${summary.availableChargersRemaining} open bays. ")
            if (summary.totalQueued > 0) {
                append("${summary.totalQueued} vehicles are staged in the optimized queue. ")
            }
            if (topAlert != null) {
                append("Attention: ${topAlert.title}. ${topAlert.message} ")
            } else {
                append("All charging bay operations are nominal.")
            }
        }.toString()

        _isSpeakingAudio.value = true
        _ttsMessage.value = "Generating high-fidelity Gemini 3.8 Flash speech audio..."

        viewModelScope.launch {
            val ttsResult = GeminiService.generateSpeechAudio(briefingText)
            if (ttsResult.isSuccess && !ttsResult.base64Audio.isNullOrBlank()) {
                _ttsMessage.value = "Playing Gemini 3.8 Flash TTS broadcast..."
                audioPlayer.playBase64Audio(
                    base64Audio = ttsResult.base64Audio,
                    mimeType = ttsResult.mimeType ?: "audio/wav",
                    fallbackText = briefingText
                ) {
                    _isSpeakingAudio.value = false
                    _ttsMessage.value = null
                }
            } else {
                // System TTS fallback
                _ttsMessage.value = "Playing local Android TTS audio broadcast..."
                audioPlayer.speakWithSystemTts(briefingText) {
                    _isSpeakingAudio.value = false
                    _ttsMessage.value = null
                }
            }
        }
    }

    fun stopSpeaking() {
        audioPlayer.stopCurrent()
        _isSpeakingAudio.value = false
        _ttsMessage.value = null
    }

    /**
     * Executes Google Search Grounding with Gemini 3.5 Flash.
     */
    fun searchGridTariffsOrEVData(query: String) {
        _isSearching.value = true
        viewModelScope.launch {
            val result = GeminiService.searchWithGrounding(query)
            _groundingResult.value = result
            _isSearching.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
