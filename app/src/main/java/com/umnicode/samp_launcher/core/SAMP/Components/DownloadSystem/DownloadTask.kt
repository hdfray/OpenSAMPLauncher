package com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem

import com.umnicode.samp_launcher.core.SAMP.Components.DefaultTask
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import java.io.File
import java.net.URL

class DownloadTask internal constructor(
    FileIndex: Int,
    URL_List: ArrayList<URL?>,
    OutDir: File,
    Status: TaskStatus?,
    Callback: DownloadTaskCallback
) : DefaultTask() {
    var OutDirectory: File
    @JvmField
    var Files: ArrayList<DownloadTaskFile?>
    var Callback: DownloadTaskCallback? = null
    var Param_PingTimeout = 500

    init {
        this.FileIndex = FileIndex
        Files = ArrayList()
        for (url in URL_List) {
            Files.add(DownloadTaskFile(url))
        }
        OutDirectory = OutDir
        this.Status = Status

        // Set callback owner
        SetCallback(Callback)
    }

    fun SetCallback(Callback: DownloadTaskCallback) {
        if (Callback !== this.Callback) {
            DownloadTaskCallback.Companion.Owner.Task = null // Remove owner from old callback
            this.Callback = Callback
            DownloadTaskCallback.Companion.Owner.Task = this // Set this as a owner in new callback
        }
    }

    override fun Reset() {
        // Reset
        FileIndex = 0
        for (c in Files.indices) {
            Files[c] = DownloadTaskFile(Files[c]!!.url)
        }
    }
}