package br.com.fabriciolima.momentus.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InAppUpdateManager"

data class UpdateProgress(
    val bytesDownloaded: Long,
    val totalBytesToDownload: Long
)

@Singleton
class InAppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    private val _installStatus = MutableStateFlow<Int>(InstallStatus.UNKNOWN)
    val installStatus: StateFlow<Int> = _installStatus.asStateFlow()

    private val _updateProgress = MutableStateFlow<UpdateProgress?>(null)
    val updateProgress: StateFlow<UpdateProgress?> = _updateProgress.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        _installStatus.value = state.installStatus()
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                _updateProgress.value = UpdateProgress(bytesDownloaded, totalBytesToDownload)
            }
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Update downloaded. Ready to be installed.")
                _updateProgress.value = null
            }
            else -> {
                _updateProgress.value = null
            }
        }
    }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    suspend fun checkForUpdate(): AppUpdateInfo? {
        return try {
            val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isFlexibleUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (isUpdateAvailable && isFlexibleUpdateAllowed) {
                Log.d(TAG, "Update available.")
                appUpdateInfo
            } else {
                Log.d(TAG, "No update available or flexible update not allowed.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for update.", e)
            null
        }
    }

    fun startUpdateFlow(updateInfo: AppUpdateInfo, activity: Activity) {
        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            activity,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            UPDATE_REQUEST_CODE
        )
        Log.d(TAG, "Flexible update flow started.")
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        Log.d(TAG, "Update installation completed.")
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 123
    }
}
