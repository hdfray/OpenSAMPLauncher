package com.terfess.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile

import com.terfess.samp_launcher.core.SAMP.Components.TaskFileStatus
import java.io.File

class ExtractTaskFile {
    @JvmField
    var OutputResult = TaskFileStatus.NONE
    var OutputDirectory: File? = null
    @JvmField
    var Filepath: File?
    var CreateContainingDirectory: Boolean

    constructor(Filepath: File?, OutputDirectory: File?, CreateContainingDirectory: Boolean) {
        this.Filepath = Filepath
        this.OutputDirectory = OutputDirectory
        this.CreateContainingDirectory = CreateContainingDirectory
    }

    constructor(Init: ExtractTaskFileInit) {
        Filepath = Init.Filepath
        OutputDirectory = Init.OutDirectory
        CreateContainingDirectory = Init.CreateContainingFolder
    }
}