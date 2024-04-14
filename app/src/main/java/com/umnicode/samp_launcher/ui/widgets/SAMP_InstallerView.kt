package com.umnicode.samp_launcher.ui.widgets

import android.animation.Animator
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPPackageStatus
import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller
import com.umnicode.samp_launcher.core.SAMP.SAMPInstallerCallback
import com.umnicode.samp_launcher.core.Utils

internal interface ButtonAnimCallback {
    fun beforeAnim()
    fun onFinished()
}

class SAMP_InstallerView : LinearLayout {
    private val BUTTON_ANIM_SPEED = 0.2f // By 1 px
    private var _Context: Context? = null
    private var RootView: View? = null
    private var InitialButtonY = 0f
    private var IsOnLayoutFired = false
    private var Callback: SAMPInstallerCallback? = null
    var EnableAnimations = true

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
        GetApplication().Installer.Callbacks.remove(Callback)
    }

    private fun Init(context: Context) {
        _Context = context
        RootView = inflate(context, R.layout.samp_installer_view, this)
        val Application = GetApplication()

        // Create callback
        Callback = object : SAMPInstallerCallback {
            override fun OnStatusChanged(Status: SAMPInstallerStatus?) {
                Update(context, Application.Installer, false)
            }

            override fun OnDownloadProgressChanged(Status: TaskStatus?) {
                if (Status != null) {
                    UpdateDownloadTaskStatus(Status, context.resources)
                }
            }

            override fun OnExtractProgressChanged(Status: TaskStatus?) {
                if (Status != null) {
                    UpdateDownloadTaskStatus(Status, context.resources)
                }
            }

            override fun OnInstallFinished(Status: InstallStatus?) {}
        }

        // Bind installer Status changing
        Application.Installer.Callbacks.add(Callback as SAMPInstallerCallback)

        // Setup progress bar
        val ProgressBar = RootView!!.findViewById<ProgressBar>(R.id.installer_download_progress)
        ProgressBar.max = 100
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        // Get button initial position
        if (!IsOnLayoutFired) {
            InitialButtonY = RootView!!.findViewById<View>(R.id.installer_button).y

            // Force-update status
            GetApplication().Installer.let { Update(_Context, it, true) }
            IsOnLayoutFired = true
        }
    }

    private fun Update(context: Context?, Installer: SAMPInstaller, IsInit: Boolean) {
        val Status = Installer.GetStatus()

        // Get handles
        val Text = RootView!!.findViewById<TextView>(R.id.installer_status_text)
        val BarLayout = RootView!!.findViewById<RelativeLayout>(R.id.installer_progress_bar_layout)
        val Button = RootView!!.findViewById<Button>(R.id.installer_button)
        val resources = context!!.resources

        // Update UI
        Text.visibility = VISIBLE
        println(Status.toString() + " - " + Installer.GetLastInstallStatus()) //TODO:
        if (Status == SAMPInstallerStatus.DOWNLOADING) {
            ShowProcessState(Button, Text, BarLayout, IsInit, resources)

            // Force update download status (used to show current status before it's updated )
            Installer.GetCurrentTaskStatus()?.let { UpdateDownloadTaskStatus(it, resources) }
        } else if (Status == SAMPInstallerStatus.EXTRACTING) {
            ShowProcessState(Button, Text, BarLayout, IsInit, resources)

            // Force update extract status (used to show current status before it's updated )
            Installer.GetCurrentTaskStatus()?.let { UpdateExtractTaskStatus(it, resources) }
        } else if (Status == SAMPInstallerStatus.PREPARING) {
            // Setup label
            Text.setTextColor(ContextCompat.getColor(context, R.color.colorNone))
            Text.text = resources.getString(R.string.installer_status_preparing)

            // Setup button
            Button.text = resources.getString(R.string.installer_button_cancel)
            Button.visibility = VISIBLE
            BindButtonAsCancel(Button)
            HideBarLayout(BarLayout, Button, IsInit, true)
        } else if (Status == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL) {
            // Setup label
            Text.setTextColor(ContextCompat.getColor(context, R.color.colorOk))
            Text.text = resources.getString(R.string.installer_status_waiting_for_apk_install)

            // Setup button
            Button.text = resources.getString(R.string.installer_button_waiting_for_apk_install)
            Button.visibility = VISIBLE
            BindButtonAsApkInstall(Button)
            HideBarLayout(BarLayout, Button, IsInit, false)
        } else { // No install running ( CANCELING_INSTALL || NONE )
            val PkgStatus = SAMPInstaller.IsInstalled(context.packageManager, resources)
            if (PkgStatus == SAMPPackageStatus.FOUND) { // SAMP installed => do nothing TODO: Export
                Text.text = resources.getString(R.string.installer_status_none_SAMP_found)
                Text.setTextColor(ContextCompat.getColor(context, R.color.colorOk))
                BarLayout.visibility = INVISIBLE
                Button.visibility = INVISIBLE
            } else {
                Text.setTextColor(ContextCompat.getColor(context, R.color.colorError))

                // Check for previous install errors
                //TODO: Permissions
                if (Installer.GetLastInstallStatus() == InstallStatus.DOWNLOADING_ERROR) {
                    // Set error message and button text
                    Text.text = resources.getString(R.string.install_status_downloading_error)
                    Button.text = resources.getString(R.string.installer_button_retry)
                } else if (Installer.GetLastInstallStatus() == InstallStatus.EXTRACTING_ERROR) {
                    // Also we don't bind button callback because it's the same for all branches
                    Text.text = resources.getString(R.string.install_status_extracting_error)
                    Button.text = resources.getString(R.string.installer_button_retry)
                } else if (Installer.GetLastInstallStatus() == InstallStatus.APK_NOT_FOUND) {
                    Text.text = resources.getString(R.string.install_status_apk_not_found)
                    Button.text = resources.getString(R.string.installer_button_retry)
                } else {
                    // If there are no errors, promote to install SAMP
                    Text.text = resources.getString(R.string.installer_status_none_SAMP_not_found)
                    Button.text = resources.getString(R.string.installer_button_install)
                }

                // Check does cache found
                if (PkgStatus == SAMPPackageStatus.CACHE_NOT_FOUND) {
                    // If there are no errors, promote to install SAMP
                    Text.text =
                        resources.getString(R.string.installer_status_none_SAMP_cache_not_found)
                    Button.text = resources.getString(R.string.installer_button_install_cache)
                    Button.setOnClickListener { v: View? ->
                        // Install SAMP
                        GetApplication().Installer.InstallOnlyCache(context)
                    }
                } else {
                    Button.setOnClickListener { v: View? ->
                        // Install SAMP
                        GetApplication().Installer.Install(context)
                    }
                }
                HideBarLayout(
                    BarLayout,
                    Button,
                    IsInit,
                    Status == SAMPInstallerStatus.CANCELING_INSTALL
                )
            }
        }
    }

    // UI
    private fun HideBarLayout(
        BarLayout: RelativeLayout,
        Button: Button,
        IsInit: Boolean,
        DisableButton: Boolean
    ) {
        // Hide bar layout and button with animation
        MoveButtonTo(
            InitialButtonY - BarLayout.height, BUTTON_ANIM_SPEED, IsInit,
            object : ButtonAnimCallback {
                override fun beforeAnim() {
                    BarLayout.visibility = INVISIBLE
                }

                override fun onFinished() {
                    Button.isEnabled = !DisableButton
                }
            })
    }

    private fun BindButtonAsCancel(Btn: Button) {
        // Set click listener
        Btn.setOnClickListener { v: View? ->
            // Cancel SAMP installation
            GetApplication().Installer.CancelInstall()
        }
    }

    private fun BindButtonAsApkInstall(Btn: Button) {
        Btn.setOnClickListener { v: View? ->
            // Open downloaded APK file
            GetApplication().Installer.OpenInstalledAPK()
        }
    }

    // States
    private fun ShowProcessState(
        Button: Button,
        Text: TextView,
        BarLayout: RelativeLayout,
        IsInit: Boolean,
        resources: Resources
    ) {
        // Show progress bar, setup text color etc
        // And setup button
        Button.text = resources.getString(R.string.installer_button_cancel)
        Button.visibility = VISIBLE
        BindButtonAsCancel(Button)

        // Set text color
        Text.setTextColor(ContextCompat.getColor(context, R.color.colorNone))

        // Setup progressBar layout and play animation
        if (BarLayout.visibility == INVISIBLE) {
            MoveButtonTo(
                InitialButtonY, BUTTON_ANIM_SPEED, IsInit,
                object : ButtonAnimCallback {
                    // Speed is measured in ms/1px
                    override fun beforeAnim() {}
                    override fun onFinished() {
                        // Show progress bar and text on it
                        BarLayout.visibility = VISIBLE
                    }
                })
        }
    }

    // Utils
    private fun UpdateDownloadTaskStatus(Status: TaskStatus, resources: Resources) {
        val Text = RootView!!.findViewById<TextView>(R.id.installer_status_text)

        // Setup values
        Text.text = String.format(
            resources.getString(R.string.installer_status_downloading),
            Status.File,
            Status.FilesNumber
        )
        UpdateProgressBar(Status, resources)
    }

    private fun UpdateExtractTaskStatus(Status: TaskStatus, resources: Resources) {
        val Text = RootView!!.findViewById<TextView>(R.id.installer_status_text)

        // Setup values
        Text.text = String.format(
            resources.getString(R.string.installer_status_extracting),
            Status.File,
            Status.FilesNumber
        )
        UpdateProgressBar(Status, resources) // Update progress bar
    }

    private fun UpdateProgressBar(Status: TaskStatus, resources: Resources) {
        val ProgressBar = RootView!!.findViewById<ProgressBar>(R.id.installer_download_progress)
        val ProgressBarText =
            RootView!!.findViewById<TextView>(R.id.installer_download_progress_text)
        if (Status.FullSize != -1.0f) { // We have both params - full size and done (in bytes)
            val Percents = Status.Done / Status.FullSize
            ProgressBar.progress = (Percents * 100).toInt()
            ProgressBarText.text = String.format(
                resources.getString(R.string.installer_progress_bar_full),
                Utils.BytesToMB(Status.Done),
                Utils.BytesToMB(Status.FullSize)
            )
        } else { // We have only count of proceeded bytes
            ProgressBar.progress = 0
            ProgressBarText.text = String.format(
                resources.getString(R.string.installer_progress_bar_only_done),
                Utils.BytesToMB(Status.Done)
            )
        }
    }

    private fun MoveButtonTo(
        y: Float,
        Speed: Float,
        IsInit: Boolean,
        Callback: ButtonAnimCallback
    ) {
        val button = RootView!!.findViewById<Button>(R.id.installer_button)
        val ButtonAnim = button.animation
        ButtonAnim?.cancel()

        // Convert speed to duration
        var Duration: Long = 0
        if (EnableAnimations && !IsInit) Duration = (Math.abs(y - button.y) / Speed).toLong()
        button.isEnabled = false
        Callback.beforeAnim()
        button.animate().setDuration(Duration).y(y).setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                button.isEnabled = true
                Callback.onFinished()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun GetApplication(): LauncherApplication {
        return _Context!!.applicationContext as LauncherApplication
    }

    companion object {
        // Alert
        fun InstallThroughAlert(context: Context) {
            // Start install before create alert
            (context.applicationContext as LauncherApplication).Installer.Install(context)

            // Create server view
            val InstallerView = SAMP_InstallerView(context)
            //InstallerView.EnableAnimations = false; // Disable animations

            // Create builder
            val builder = AlertDialog.Builder(context)
            builder.setTitle("")
            builder.setCancelable(false)

            // Set custom layout
            builder.setView(InstallerView.RootView)

            // Add close button
            builder.setPositiveButton("In background") { dialog: DialogInterface, which: Int -> dialog.dismiss() }

            // create and show the alert dialog
            val dialog = builder.create()
            dialog.show()

            // Bind event ( if user cancel or finish install - change text )
            (context.applicationContext as LauncherApplication).Installer.Callbacks.add(object :
                SAMPInstallerCallback {
                override fun OnStatusChanged(Status: SAMPInstallerStatus?) {
                    if (Status == SAMPInstallerStatus.CANCELING_INSTALL) {
                        dialog.dismiss()
                    }
                }

                override fun OnDownloadProgressChanged(Status: TaskStatus?) {}
                override fun OnExtractProgressChanged(Status: TaskStatus?) {}
                override fun OnInstallFinished(Status: InstallStatus?) {
                    builder.setPositiveButton("Close") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                }
            })
        }
    }
}