package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem

import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus

class ExtractTaskCallbackOwner {
    var Task: ExtractTask? = null
}

interface ExtractTaskCallback {
    fun OnStarted()
    fun OnChecksFinished() {}
    fun OnFinished(IsCanceled: Boolean)
    fun OnFileExtractStarted(File: ExtractTaskFile?)
    fun OnFileExtractFinished(File: ExtractTaskFile?, Status: TaskFileStatus?)
    fun OnProgressChanged(Status: TaskStatus?)

    // Get task method
    fun Task(): ExtractTask? {
        return Owner.Task
    }

    companion object {
        val Owner = ExtractTaskCallbackOwner()
    }
}