package com.terfess.samp_launcher.ui.widgets.playbutton

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.terfess.samp_launcher.LauncherApplication
import com.terfess.samp_launcher.R
import com.terfess.samp_launcher.core.SAMP.Components.TaskStatus
import com.terfess.samp_launcher.core.SAMP.Enums.InstallStatus
import com.terfess.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus
import com.terfess.samp_launcher.core.SAMP.Enums.SAMPPackageStatus
import com.terfess.samp_launcher.core.SAMP.SAMPInstaller
import com.terfess.samp_launcher.core.SAMP.SAMPInstallerCallback
import com.terfess.samp_launcher.core.ServerConfig
import com.terfess.samp_launcher.core.Utils
import com.terfess.samp_launcher.ui.widgets.SAMP_InstallerView

class PlayButton : AppCompatButton {
    private var Action: PlayButtonAction? = null
    private var Config: ServerConfig? = null
    private var OnSAMPLaunch: SAMPLaunchCallback? = null
    private var _Context: Context? = null
    private var Callback: SAMPInstallerCallback? = null

    constructor(context: Context) : super(context) {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        Init(context)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // Unregister callback from installer
        GetApplication().Installer?.Callbacks?.remove(Callback)
    }

    fun SetServerConfig(Config: ServerConfig) {
        this.Config = Config
        val resources = _Context!!.resources
        if (!this.isInEditMode) {
            // Check for SAMP ( but do not update action, because we check for this in init() and installer listener )
            if (SAMPInstaller.IsInstalled(
                    _Context!!.packageManager,
                    resources
                ) != SAMPPackageStatus.FOUND
            ) {
                return
            }

            // Check does server config is correct
            if (ServerConfig.IsStatusError(Config.Status)) {
                UpdateAction(PlayButtonAction.SHOW_SERVER_INCORRECT)
                return
            }
            UpdateAction(PlayButtonAction.LAUNCH_SAMP)
        } else {
            this.text = "[In editor preview]" // Set preview text for editor
        }
    }

    fun GetAction(): PlayButtonAction? {
        return Action
    }

    fun SetOnSAMPLaunchCallback(Callback: SAMPLaunchCallback) {
        OnSAMPLaunch = Callback
    }

    private fun Init(context: Context) {
        _Context = context
        SetServerConfig(ServerConfig())

        // Bind on clicked
        setOnClickListener {
            if (Action == PlayButtonAction.INSTALL_SAMP) {
                SAMP_InstallerView.InstallThroughAlert(_Context!!)
            } else if (Action == PlayButtonAction.INSTALL_SAMP_APK) {
                (_Context!!.applicationContext as LauncherApplication).Installer.OpenInstalledAPK()
            } else if (Action == PlayButtonAction.INSTALL_SAMP_CACHE) {
                (_Context!!.applicationContext as LauncherApplication).Installer.InstallOnlyCache(
                    _Context!!
                )
            } else if (Action == PlayButtonAction.LAUNCH_SAMP) {
                OnSAMPLaunch!!.Launch()
            } else {

            }
        }

        // Force update
        GetApplication().Installer?.GetStatus()?.let { UpdateActionByInstallerStatus(it, true) }

        // Create callback
        Callback = object : SAMPInstallerCallback {
            override fun OnStatusChanged(Status: SAMPInstallerStatus?) {
                if (Status != null) {
                    UpdateActionByInstallerStatus(Status, false)
                }
            }

            override fun OnDownloadProgressChanged(Status: TaskStatus?) {
                if (Action == PlayButtonAction.SHOW_DOWNLOAD_STATUS) {
                    if (Status != null) {
                        UpdateDownloadTaskStatus(Status)
                    }
                }
            }

            override fun OnExtractProgressChanged(Status: TaskStatus?) {
                if (Action == PlayButtonAction.SHOW_EXTRACT_STATUS) {
                    if (Status != null) {
                        UpdateExtractTaskStatus(Status)
                    }
                }
            }

            override fun OnInstallFinished(Status: InstallStatus?) {
                if (Status == InstallStatus.SUCCESSFUL) {
                    UpdateAction(PlayButtonAction.LAUNCH_SAMP)
                } else {
                    UpdateAction(PlayButtonAction.INSTALL_SAMP)
                }
            }
        }

        // Bind to installer Status change
        (_Context!!.applicationContext as LauncherApplication).Installer?.Callbacks?.add(Callback as SAMPInstallerCallback)
    }

    // Utils
    private fun GetApplication(): LauncherApplication {
        return _Context!!.applicationContext as LauncherApplication
    }

    private fun UpdateDownloadTaskStatus(Status: TaskStatus) {
        this.text = String.format(
            _Context!!.resources.getString(R.string.play_button_show_download_status),
            Status.File,
            Status.FilesNumber
        )
    }

    private fun UpdateExtractTaskStatus(Status: TaskStatus) {
        this.text = String.format(
            _Context!!.resources.getString(R.string.play_button_show_extract_status),
            Utils.BytesToMB(Status.Done),
            Utils.BytesToMB(Status.FullSize)
        )
    }

    private fun UpdateActionByInstallerStatus(Status: SAMPInstallerStatus, ProceedNone: Boolean) {
        if (Status == SAMPInstallerStatus.DOWNLOADING) {
            UpdateAction(PlayButtonAction.SHOW_DOWNLOAD_STATUS)
        } else if (Status == SAMPInstallerStatus.EXTRACTING) {
            UpdateAction(PlayButtonAction.SHOW_EXTRACT_STATUS)
        } else if (Status == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL) {
            UpdateAction(PlayButtonAction.INSTALL_SAMP_APK)
        } else if (Status == SAMPInstallerStatus.NONE) {
            if (ProceedNone) {
                val PkgStatus =
                    SAMPInstaller.IsInstalled(_Context!!.packageManager, _Context!!.resources)
                if (PkgStatus == SAMPPackageStatus.FOUND) {
                    UpdateAction(PlayButtonAction.LAUNCH_SAMP)
                } else if (PkgStatus == SAMPPackageStatus.CACHE_NOT_FOUND) {
                    UpdateAction(PlayButtonAction.INSTALL_SAMP_CACHE)
                } else {
                    UpdateAction(PlayButtonAction.INSTALL_SAMP)
                }
            }
        }
    }

    private fun UpdateAction(NewAction: PlayButtonAction) {
        val resources = _Context!!.resources

        // Active actions
        if (NewAction == PlayButtonAction.INSTALL_SAMP) {
            // Install SAMP cache
            this.text = resources.getString(R.string.play_button_install_SAMP)
            this.setTextColor(resources.getColor(R.color.colorError))
            this.isEnabled = true
        } else if (NewAction == PlayButtonAction.INSTALL_SAMP_APK) {
            // Install SAMP apk
            this.text = resources.getString(R.string.play_button_install_SAMP_APK)
            this.setTextColor(resources.getColor(R.color.colorOk))
            this.isEnabled = true
        } else if (NewAction == PlayButtonAction.INSTALL_SAMP_CACHE) {
            // If everything is ok - we can launch game by clicking button
            this.text = resources.getString(R.string.play_button_install_SAMP_CACHE)
            this.setTextColor(resources.getColor(R.color.colorError))
            this.isEnabled = true
        } else if (NewAction == PlayButtonAction.LAUNCH_SAMP) {
            // If everything is ok - we can launch game by clicking button
            this.text = resources.getString(R.string.play_button_launch_SAMP)
            this.setTextColor(resources.getColor(R.color.colorOk))
            this.isEnabled = true
        } else if (NewAction == PlayButtonAction.SHOW_SERVER_INCORRECT) {
            this.text = resources.getString(R.string.play_button_server_incorrect)
            this.setTextColor(resources.getColor(R.color.colorError))
            this.isEnabled = false
        } else if (NewAction == PlayButtonAction.SHOW_EXTRACT_STATUS) {
            GetApplication().Installer?.GetCurrentTaskStatus()?.let { UpdateExtractTaskStatus(it) }
            this.setTextColor(resources.getColor(R.color.colorError))
            this.isEnabled = false
        } else if (NewAction == PlayButtonAction.SHOW_DOWNLOAD_STATUS) {
            GetApplication().Installer?.GetCurrentTaskStatus()?.let { UpdateDownloadTaskStatus(it) }
            this.setTextColor(resources.getColor(R.color.colorError))
            this.isEnabled = false
        }
        Action = NewAction
    }
}