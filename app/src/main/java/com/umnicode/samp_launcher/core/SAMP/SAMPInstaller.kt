package com.umnicode.samp_launcher.core.SAMP

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.umnicode.samp_launcher.MainActivity
import com.umnicode.samp_launcher.PermissionRequestCallback
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ArchiveComponent
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTask
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskCallback
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit
import com.umnicode.samp_launcher.core.SAMP.Components.AsyncTaskContainer
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadComponent
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskCallback
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPPackageStatus
import com.umnicode.samp_launcher.core.Utils
import java.io.File
import java.util.Arrays

class SAMPInstaller(context: Context) {
    private var Status: SAMPInstallerStatus = SAMPInstallerStatus.NONE
    private var LastInstallStatus: InstallStatus = InstallStatus.NONE
    private val _Context: Context
    var Callbacks: ArrayList<SAMPInstallerCallback>
    private var APK_Filepath: File
    private var Data_Filepath: File? = null
    private val downloadTask: DownloadTask
    private var downloadTaskContainer: AsyncTaskContainer? = null
    private var extractTask: ExtractTask? = null
    private var extractTaskContainer: AsyncTaskContainer? = null

    init {
        Callbacks = ArrayList()
        ChangeStatus(SAMPInstallerStatus.NONE)
        APK_Filepath = File("")
        _Context = context

        // Setup download component
        val resources = context.resources
        val defaultDir = GetDefaultDownloadDirectory(resources)
        val URL = ArrayList<String>(
            Arrays.asList<String>(
                resources.getString(R.string.SAMP_apk_url),
                resources.getString(R.string.SAMP_data_url)
            )
        )
        downloadTask = DownloadComponent.CreateTask(URL, defaultDir,
            object : DownloadTaskCallback {
                override fun OnStarted() {}
                override fun OnFinished(IsCanceled: Boolean) {
                    // Check does all files downloaded successfully
                    if (!IsCanceled) {
                        for (file in this.Task()?.Files!!) {
                            if (file?.OutputResult !== TaskFileStatus.SUCCESSFUL) {
                                FinishInstall(InstallStatus.DOWNLOADING_ERROR) // Finish install with error
                                return
                            }
                        }

                        // Set APK_Filepath, we will use it on WAITING_FOR_APK_INSTALL stage
                        APK_Filepath = Task()?.Files?.get(0)?.OutputFilename!!

                        // Setup path to data dir
                        val DataDir: File = File(
                            Environment.getExternalStorageDirectory().toString() + "/Android/data"
                        )

                        // Try to extract data file
                        val TaskFiles: ArrayList<ExtractTaskFileInit> =
                            ArrayList<ExtractTaskFileInit>()
                        TaskFiles.add(
                            ExtractTaskFileInit(
                                Task()?.Files?.get(1)?.OutputFilename!!,
                                DataDir, false
                            )
                        )
                        extractTask?.SetFilesFromInit(TaskFiles)

                        // Run new container
                        ChangeStatus(SAMPInstallerStatus.EXTRACTING) // Change status
                        extractTask?.Reset() // Reset task after previous install
                        extractTaskContainer = ArchiveComponent.RunTask(extractTask!!)

                        // Remove container
                        downloadTaskContainer = null
                    }
                }

                override fun OnChecksFinished() {
                    ChangeStatus(SAMPInstallerStatus.DOWNLOADING)
                }

                override fun OnFileDownloadStarted() {}
                override fun OnFileDownloadFinished(Status: TaskFileStatus?) {}
                override fun OnProgressChanged(Status: TaskStatus?) {
                    // Notify callbacks
                    for (Callback in Callbacks) {
                        val mainHandler: Handler = Handler(Looper.getMainLooper())
                        val callbackRunnable =
                            Runnable { Callback.OnDownloadProgressChanged(Status) }
                        mainHandler.post(callbackRunnable)
                    }
                }
            })
        extractTask = ArchiveComponent.CreateTask(
            ArrayList<ExtractTaskFileInit>(),
            object : ExtractTaskCallback {
                override fun OnStarted() {
                    ChangeStatus(SAMPInstallerStatus.EXTRACTING)
                }

                override fun OnFinished(IsCanceled: Boolean) {
                    if (!IsCanceled) {
                        for (file in Task()?.Files!!) {
                            if (file?.OutputResult !== TaskFileStatus.SUCCESSFUL) {
                                FinishInstall(InstallStatus.EXTRACTING_ERROR) // Finish install with error
                                return
                            }
                        }
                        Data_Filepath = this.Task()?.Files?.get(0)?.Filepath
                        ChangeStatus(SAMPInstallerStatus.WAITING_FOR_APK_INSTALL)

                        // Remove extract container
                        extractTaskContainer = null
                    }
                }

                override fun OnFileExtractStarted(File: ExtractTaskFile?) {}
                override fun OnFileExtractFinished(
                    File: ExtractTaskFile?,
                    Status: TaskFileStatus?
                ) {
                }

                override fun OnProgressChanged(Status: TaskStatus?) {
                    // Notify callbacks
                    for (Callback in Callbacks) {
                        val mainHandler: Handler = Handler(Looper.getMainLooper())
                        val callbackRunnable =
                            Runnable { Callback.OnExtractProgressChanged(Status) }
                        mainHandler.post(callbackRunnable)
                    }
                }
            })
    }

    fun OpenInstalledAPK() {
        if (!APK_Filepath.exists()) {
            FinishInstall(InstallStatus.APK_NOT_FOUND)
            return
        }
        Log.i("SAMPInstaller", "Try to open downloaded APK")

        // Get file type
        val MIME: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            Utils.GetFileLastExtension(
                APK_Filepath
            )
        )
        val uri: Uri = FileProvider.getUriForFile(
            _Context,
            "${_Context.packageName}.provider",
            APK_Filepath
        )

        // Install APK
        val Install = Intent(Intent.ACTION_VIEW)

        // Setup flags
        Install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        Install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        Install.setDataAndType(uri, MIME)
        _Context.startActivity(Install)
    }

    // Install management
    fun Install(context: Context) {
        if (IsInstalled(context.packageManager, context.resources) == SAMPPackageStatus.FOUND ||
            Status != SAMPInstallerStatus.NONE
        ) {
            return
        }

        // Install with default download task (APK and cache )
        InstallImpl(context, downloadTask)
    }

    fun InstallOnlyCache(context: Context) {
        if (IsCacheInstalled() || Status != SAMPInstallerStatus.NONE) {
            return
        }
        downloadTask.Reset()
        val FilesI = ArrayList<Int>()
        FilesI.add(11)
        val CacheTask: DownloadTask? = DownloadComponent.CreateTaskFromFiles(FilesI, downloadTask)
        if (CacheTask != null) {
            InstallImpl(context, CacheTask)
        }
    }

    fun CancelInstall() {
        StopInstall(SAMPInstallerStatus.CANCELING_INSTALL, InstallStatus.CANCELED)
    }

    fun ReCheckInstallResources(context: MainActivity) {
        if (GetStatus() != SAMPInstallerStatus.NONE && GetStatus() != SAMPInstallerStatus.CANCELING_INSTALL) {
            val Status: SAMPPackageStatus = IsInstalled(context.packageManager, context.resources)

            // If SAMP is already installed
            if (Status == SAMPPackageStatus.FOUND && this.Status != SAMPInstallerStatus.NONE) {
                // Stop running installation ( with successful status )
                StopInstall(SAMPInstallerStatus.CANCELING_INSTALL, InstallStatus.SUCCESSFUL)
                return
            }
            if (GetStatus() == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL ||
                GetStatus() == SAMPInstallerStatus.EXTRACTING
            ) {

                // Check does APK exist anymore ( UndefinedBehavior but... )
                if (!APK_Filepath.exists()) {
                    FinishInstall(InstallStatus.APK_NOT_FOUND)
                    return
                }
            }
            Log.i("SAMPInstaller", "ReCheckInstall is successful")
        }
    }

    // Utils
    private fun InstallImpl(context: Context, Task: DownloadTask) { // Without checks
        ChangeStatus(SAMPInstallerStatus.PREPARING)

        // Check permissions
        val Activity: MainActivity = context as MainActivity
        Activity.RequestStoragePermission { IsGranted: Boolean ->
            if (IsGranted) {
                APK_Filepath = File("")

                // Reset task and run it
                downloadTask.Reset()
                downloadTaskContainer = DownloadComponent.RunTask(downloadTask)
            } else {
                FinishInstall(InstallStatus.STORAGE_PERMISSIONS_DENIED) // Error
            }
        }
    }

    // General function for stop
    private fun StopInstall(WhileStoppingStatus: SAMPInstallerStatus, TargetStatus: InstallStatus) {
        if (Status == SAMPInstallerStatus.NONE) return
        var IsStoppingContainers = false

        // Stop downloading ( = stop container )
        if (downloadTaskContainer != null) {
            ChangeStatus(WhileStoppingStatus)
            IsStoppingContainers = true
            downloadTaskContainer!!.Cancel(Runnable {
                downloadTaskContainer = null
                CheckStopState(TargetStatus)
            })
        }

        // Stop extract container
        if (extractTaskContainer != null) {
            ChangeStatus(WhileStoppingStatus)
            IsStoppingContainers = true
            extractTaskContainer!!.Cancel(Runnable {
                extractTaskContainer = null
                CheckStopState(TargetStatus)
            })
        }
        if (IsStoppingContainers) CheckStopState(TargetStatus)
    }

    private fun CheckStopState(TargetStatus: InstallStatus) {
        if (downloadTaskContainer == null && extractTaskContainer == null) {
            FinishInstall(TargetStatus)
        }
    }

    private fun Cleanup() {
        val DownloadDirectory = GetDefaultDownloadDirectory(_Context.resources)
        if (DownloadDirectory.exists()) {
            if (!DownloadDirectory.delete()) {
                Log.e("SAMPInstaller", "Cleanup - failed to remove download directory")
            }
        }
    }

    private fun FinishInstall(Status: InstallStatus) {
        Cleanup()
        ChangeStatus(SAMPInstallerStatus.NONE)
        BroadcastInstallFinished(Status)
    }

    private fun BroadcastInstallFinished(Status: InstallStatus) {
        LastInstallStatus = Status
        for (Callback in Callbacks) {
            Handler(Looper.getMainLooper()).post(Runnable {
                Callback.OnInstallFinished(
                    LastInstallStatus
                )
            })
        }
    }

    private fun ChangeStatus(Status: SAMPInstallerStatus) {
        this.Status = Status
        for (globalCallback in Callbacks) {
            Handler(Looper.getMainLooper()).post(Runnable { globalCallback.OnStatusChanged(Status) })
        }
    }

    // Getters
    fun GetStatus(): SAMPInstallerStatus {
        return Status
    }

    fun GetCurrentTaskStatus(): TaskStatus? {
        return if (Status == SAMPInstallerStatus.EXTRACTING) extractTask?.Status else downloadTask.Status
    }

    fun GetLastInstallStatus(): InstallStatus {
        return LastInstallStatus
    }

    fun IsStoragePermissionGranted(context: Context): Boolean {
        return (context as MainActivity).IsStoragePermissionsGranted()
    }

    companion object {
        // Static tools
        fun GetDefaultDirectory(resources: Resources): File {
            return File(
                Environment.getExternalStorageDirectory().toString() + '/' +
                        resources.getString(R.string.app_root_directory_name)
            )
        }

        fun GetDefaultDownloadDirectory(resources: Resources): File {
            return File(
                GetDefaultDirectory(resources),
                resources.getString(R.string.SAMP_download_directory_name)
            )
        }

        fun IsInstalled(Manager: PackageManager, resources: Resources): SAMPPackageStatus {
            return try {
                Manager.getPackageInfo(resources.getString(R.string.SAMP_package_name), 0)
                if (!IsCacheInstalled()) SAMPPackageStatus.CACHE_NOT_FOUND else SAMPPackageStatus.FOUND
            } catch (ex: PackageManager.NameNotFoundException) {
                SAMPPackageStatus.NOT_FOUND
            }
        }

        fun IsCacheInstalled(): Boolean {
            return File(
                Environment.getExternalStorageDirectory().toString() +
                        "/Android/data/com.rockstargames.gtasa/files"
            ).exists()
        } //TODO: Export to directory function
    }
}