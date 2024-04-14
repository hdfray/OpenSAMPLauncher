package com.terfess.samp_launcher.core.SAMP.Components.DownloadSystem

import com.terfess.samp_launcher.core.SAMP.Components.AsyncTaskContainer
import com.terfess.samp_launcher.core.SAMP.Components.TaskStatus
import com.terfess.samp_launcher.core.Utils.DeepCloneObject
import java.io.File
import java.net.MalformedURLException
import java.net.URL

object DownloadComponent {
    fun CreateTask(
        URL: ArrayList<String>,
        Directory: File,
        Callback: DownloadTaskCallback
    ): DownloadTask {
        val URL_list = ArrayList<URL?>()
        for (url_str in URL) {
            try {
                URL_list.add(URL(url_str))
            } catch (ignore: MalformedURLException) {
            }
        }

        // Create task
        return DownloadTask(0, URL_list, Directory, TaskStatus.CreateEmpty(URL_list.size), Callback)
    }

    fun CreateTaskFromFiles(Indexes: ArrayList<Int>, OriginalTask: DownloadTask): DownloadTask? {
        // Warning: Slow algorithm
        val NewTask = DeepCloneObject(OriginalTask)
        if (NewTask != null) {
            NewTask.Files = ArrayList()
            for (i in OriginalTask.Files.indices) {
                if (!Indexes.contains(i)) {
                    NewTask.Files.removeAt(i)
                }
            }
        }
        return NewTask
    }

    @JvmStatic
    fun RunTask(Task: DownloadTask): AsyncTaskContainer {
        return AsyncTaskContainer(DownloadAsyncTask(Task))
    }
}