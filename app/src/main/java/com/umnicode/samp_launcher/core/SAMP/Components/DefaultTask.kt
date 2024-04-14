package com.umnicode.samp_launcher.core.SAMP.Components

abstract class DefaultTask {
    var FileIndex = 0
    @JvmField
    var Status: TaskStatus? = null
    var Flag_RemoveAllFilesWhenCancelled = false
    var Flag_RemoveFailedFile = true
    var Flag_ResetTaskWhenCancelled = true
    var Flag_ResetTaskAfterFinished = true
    var Flag_OverrideIfExist = false
    var Param_UpdateProgressFrequency = 64 // Every 64 KB
    abstract fun Reset()
}