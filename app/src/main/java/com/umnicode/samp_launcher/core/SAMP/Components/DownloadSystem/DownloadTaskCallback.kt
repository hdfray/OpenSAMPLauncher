package com.terfess.samp_launcher.core.SAMP.Components.DownloadSystem

import com.terfess.samp_launcher.core.SAMP.Components.TaskFileStatus
import com.terfess.samp_launcher.core.SAMP.Components.TaskStatus

// Non-public wrapper for DownloadTask that own this callback
class DownloadTaskCallbackOwner {
    var Task: DownloadTask? = null
}

interface DownloadTaskCallback {
    fun OnStarted()
    fun OnChecksFinished() {}
    fun OnFinished(IsCanceled: Boolean)
    fun OnFileDownloadStarted()
    fun OnBufferReadingStarted() {}
    fun OnFileDownloadFinished(Status: TaskFileStatus?)
    fun OnProgressChanged(Status: TaskStatus?)

    // Get task method
    fun Task(): DownloadTask? {
        return Owner.Task
    }

    companion object {
        val Owner = DownloadTaskCallbackOwner()
    }
}