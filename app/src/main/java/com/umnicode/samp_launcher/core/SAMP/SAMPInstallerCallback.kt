package com.umnicode.samp_launcher.core.SAMP

import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus

interface SAMPInstallerCallback {
    fun OnStatusChanged(Status: SAMPInstallerStatus?)
    fun OnDownloadProgressChanged(Status: TaskStatus?)
    fun OnExtractProgressChanged(Status: TaskStatus?)
    fun OnInstallFinished(Status: InstallStatus?)
}