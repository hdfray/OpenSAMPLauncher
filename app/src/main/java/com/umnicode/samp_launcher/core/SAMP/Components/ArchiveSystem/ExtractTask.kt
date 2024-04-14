package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem

import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit
import com.umnicode.samp_launcher.core.SAMP.Components.DefaultTask
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus

class ExtractTask internal constructor(
    FileIndex: Int,
    FilepathList: ArrayList<ExtractTaskFileInit>,
    Status: TaskStatus?,
    Callback: ExtractTaskCallback
) : DefaultTask() {
    @JvmField
    var Files: ArrayList<ExtractTaskFile?>? = null
    var Callback: ExtractTaskCallback? = null

    init {
        this.FileIndex = FileIndex
        this.Status = Status

        // Set files
        SetFilesFromInit(FilepathList)

        // Set callback owner
        SetCallback(Callback)
    }

    fun SetFilesFromInit(List: ArrayList<ExtractTaskFileInit>) {
        Files = ArrayList()
        for (Init in List) {
            Files!!.add(ExtractTaskFile(Init))
        }
        Status?.FilesNumber = Files!!.size
    }

    fun SetCallback(Callback: ExtractTaskCallback) {
        if (Callback !== this.Callback) {
            ExtractTaskCallback.Companion.Owner.Task = null // Remove owner from old callback
            this.Callback = Callback
            ExtractTaskCallback.Companion.Owner.Task = this // Set this as a owner in new callback
        }
    }

    override fun Reset() {
        // Reset
        FileIndex = 0
        for (c in Files!!.indices) {
            Files!![c] = ExtractTaskFile(
                Files!![c]!!.Filepath, Files!![c]!!.OutputDirectory,
                Files!![c]!!.CreateContainingDirectory
            )
        }
    }
}