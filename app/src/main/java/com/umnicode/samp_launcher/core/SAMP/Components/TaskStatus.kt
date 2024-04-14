package com.umnicode.samp_launcher.core.SAMP.Components

class TaskStatus(Done: Float, FullSize: Float, File: Int, FliesNumber: Int) {
    var Done = 0f
    var FullSize = -1.0f
    var File = 0
    var FilesNumber = 0

    init {
        this.Done = Done
        this.FullSize = FullSize
        this.File = File
        FilesNumber = FliesNumber
    }

    companion object {
        fun CreateEmpty(FilesCount: Int): TaskStatus {
            return if (FilesCount != 0) {
                TaskStatus(0f, -1.0f, 1, FilesCount)
            } else TaskStatus(0f, -1.0f, 0, 0)
        }
    }
}