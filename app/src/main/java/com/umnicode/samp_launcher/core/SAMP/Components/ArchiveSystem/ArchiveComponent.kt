package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem

import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit
import com.umnicode.samp_launcher.core.SAMP.Components.AsyncTaskContainer
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import com.umnicode.samp_launcher.core.Utils.GetFileLastExtension
import java.io.File

object ArchiveComponent {
    fun CreateTask(
        FilepathList: ArrayList<ExtractTaskFileInit>,
        Callback: ExtractTaskCallback
    ): ExtractTask {
        // Create default task
        return ExtractTask(0, FilepathList, TaskStatus.CreateEmpty(FilepathList.size), Callback)
    }

    @JvmStatic
    fun RunTask(Task: ExtractTask): AsyncTaskContainer {
        return AsyncTaskContainer(ExtractAsyncTask(Task))
    }

    fun GetTypeOfArchive(Archive: File?): ArchiveType {
        val Extension = GetFileLastExtension(
            Archive!!
        )
        return if (Extension == "zip") {
            ArchiveType.ZIP
        } else {
            ArchiveType.UNSUPPORTED
        }
    }
}