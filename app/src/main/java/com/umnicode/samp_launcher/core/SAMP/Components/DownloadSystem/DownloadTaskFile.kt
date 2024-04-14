package com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem

import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus
import java.io.File
import java.net.URL

class DownloadTaskFile internal constructor(var url: URL?) {
    @JvmField
    var OutputFilename: File? = null
    @JvmField
    var OutputResult = TaskFileStatus.NONE
}