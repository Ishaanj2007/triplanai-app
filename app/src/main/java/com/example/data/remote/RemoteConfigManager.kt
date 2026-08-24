package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppConfigState(
    val latestVersion: String = BuildConfig.VERSION_NAME,
    val minSupportedVersion: String = "2.0.1",
    val forceUpdate: Boolean = false,
    val updateMessage: String =
        "A new version of TripPlanAI is available.",
    val updateUrl: String =
        "https://github.com/Ishaanj2007/triplanai-app/releases",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String =
        "TripPlanAI is temporarily unavailable. Please check back shortly.",
    val isCheckingForUpdate: Boolean = false,
    val lastCheckTimestamp: Long = 0L,
    val checkStatusFeedback: String? = null
)

object VersionComparator {

    /**
     * Compares two version strings semantically.
     *
     * Examples:
     * 2.0.9 < 2.1.0
     * 2.1.0 == 2.1.0
     * 2.1.0 < 2.1.1
     * 2.1.9 < 2.2.0
     */
    fun compare(versionA: String, versionB: String): Int {
        val cleanA = cleanVersion(versionA)
        val cleanB = cleanVersion(versionB)

        val partsA = cleanA
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val partsB = cleanB
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(partsA.size, partsB.size)

        for (i in 0 until maxLength) {
            val partA = partsA.getOrElse(i) { 0 }
            val partB = partsB.getOrElse(i) { 0 }

            if (partA != partB) {
                return partA.compareTo(partB)
            }
        }

        return 0
    }

    private fun cleanVersion(version: String): String {
        val trimmed = version
            .trim()
            .removePrefix("v")
            .removePrefix("V")

        return trimmed
            .split("-", "+", " ")
            .firstOrNull()
            .orEmpty()
    }

    fun isOlderThan(current: String, target: String): Boolean {
        return compare(current, target) < 0
    }
}

class RemoteConfigManager(
    private val context: Context
) {

    private companion object {
        const val TAG = "RemoteConfigManager"

        const val KEY_LATEST_VERSION = "latest_version"
        const val KEY_MIN_SUPPORTED_VERSION = "minimum_supported_version"
        const val KEY_FORCE_UPDATE = "force_update"
        const val KEY_UPDATE_MESSAGE = "update_message"
        const val KEY_UPDATE_URL = "update_url"
        const val KEY_MAINTENANCE_MODE = "maintenance_mode"
        const val KEY_MAINTENANCE_MESSAGE = "maintenance_message"

        const val DEFAULT_MIN_SUPPORTED_VERSION = "2.0.1"

        const val DEFAULT_UPDATE_MESSAGE =
            "A new version of TripPlanAI is available."

        const val DEFAULT_UPDATE_URL =
            "https://github.com/Ishaanj2007/triplanai-app/releases"

        const val DEFAULT_MAINTENANCE_MESSAGE =
            "TripPlanAI is temporarily unavailable. Please check back shortly."
    }

    private val _configState = MutableStateFlow(
        AppConfigState()
    )

    val configState: StateFlow<AppConfigState> =
        _configState.asStateFlow()

    private var remoteConfig: FirebaseRemoteConfig? = null

    init {
        initFirebaseRemoteConfig()
    }

    private fun initFirebaseRemoteConfig() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val config = FirebaseRemoteConfig.getInstance()

            val configSettings =
                FirebaseRemoteConfigSettings.Builder()
                    // Production: fetch at most once per hour.
                    // Use 0 temporarily while testing Remote Config.
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()

            config.setConfigSettingsAsync(configSettings)

            val defaults = mapOf<String, Any>(
                KEY_LATEST_VERSION to BuildConfig.VERSION_NAME,
                KEY_MIN_SUPPORTED_VERSION to DEFAULT_MIN_SUPPORTED_VERSION,
                KEY_FORCE_UPDATE to false,
                KEY_UPDATE_MESSAGE to DEFAULT_UPDATE_MESSAGE,
                KEY_UPDATE_URL to DEFAULT_UPDATE_URL,
                KEY_MAINTENANCE_MODE to false,
                KEY_MAINTENANCE_MESSAGE to DEFAULT_MAINTENANCE_MESSAGE
            )

            config.setDefaultsAsync(defaults)

            remoteConfig = config

            readCurrentValues()

        } catch (e: Exception) {
            Log.w(
                TAG,
                "Firebase Remote Config initialization failed: ${e.message}"
            )
        }
    }

    fun fetchAndActivate(
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val config = remoteConfig

        if (config == null) {
            updateCheckFeedback()
            onComplete?.invoke(false)
            return
        }

        _configState.value = _configState.value.copy(
            isCheckingForUpdate = true,
            checkStatusFeedback = null
        )

        config.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    readCurrentValues()
                }

                updateCheckFeedback()

                onComplete?.invoke(task.isSuccessful)
            }
            .addOnFailureListener { exception ->

                Log.w(
                    TAG,
                    "Remote Config fetch failed: ${exception.message}"
                )

                // Keep the last successfully fetched/default values.
                updateCheckFeedback()

                onComplete?.invoke(false)
            }
    }

    private fun readCurrentValues() {
        val config = remoteConfig ?: return

        try {
            val latestVersion =
                config.getString(KEY_LATEST_VERSION)
                    .ifBlank { BuildConfig.VERSION_NAME }

            val minimumSupportedVersion =
                config.getString(KEY_MIN_SUPPORTED_VERSION)
                    .ifBlank { DEFAULT_MIN_SUPPORTED_VERSION }

            val forceUpdate =
                config.getBoolean(KEY_FORCE_UPDATE)

            val updateMessage =
                config.getString(KEY_UPDATE_MESSAGE)
                    .ifBlank { DEFAULT_UPDATE_MESSAGE }

            val updateUrl =
                config.getString(KEY_UPDATE_URL)
                    .ifBlank { DEFAULT_UPDATE_URL }

            val maintenanceMode =
                config.getBoolean(KEY_MAINTENANCE_MODE)

            val maintenanceMessage =
                config.getString(KEY_MAINTENANCE_MESSAGE)
                    .ifBlank { DEFAULT_MAINTENANCE_MESSAGE }

            _configState.value = _configState.value.copy(
                latestVersion = latestVersion,
                minSupportedVersion = minimumSupportedVersion,
                forceUpdate = forceUpdate,
                updateMessage = updateMessage,
                updateUrl = updateUrl,
                maintenanceMode = maintenanceMode,
                maintenanceMessage = maintenanceMessage
            )

        } catch (e: Exception) {
            Log.w(
                TAG,
                "Error reading Remote Config values: ${e.message}"
            )
        }
    }

    private fun updateCheckFeedback() {
        val state = _configState.value

        val feedback =
            when {
                state.maintenanceMode ->
                    "TripPlanAI is currently under maintenance"

                isUpdateAvailable() ->
                    "Update available: v${state.latestVersion}"

                else ->
                    "You are using the latest version (v${BuildConfig.VERSION_NAME})"
            }

        _configState.value = state.copy(
            isCheckingForUpdate = false,
            lastCheckTimestamp = System.currentTimeMillis(),
            checkStatusFeedback = feedback
        )
    }

    fun isUpdateAvailable(): Boolean {
        return VersionComparator.isOlderThan(
            BuildConfig.VERSION_NAME,
            _configState.value.latestVersion
        )
    }

    fun isUpdateMandatory(): Boolean {
        val currentVersion = BuildConfig.VERSION_NAME
        val state = _configState.value

        val belowMinimum =
            VersionComparator.isOlderThan(
                currentVersion,
                state.minSupportedVersion
            )

        val forcedUpdate =
            state.forceUpdate && isUpdateAvailable()

        return belowMinimum || forcedUpdate
    }

    fun isMaintenanceMode(): Boolean {
        return _configState.value.maintenanceMode
    }
}