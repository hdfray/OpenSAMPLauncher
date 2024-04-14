package com.terfess.samp_launcher.core.SAMP.Components

class AsyncTaskContainer(private val AsyncTask: ExtendedAsyncTask) {
    init {
        AsyncTask.execute() // Run task
    }

    fun Cancel(OnFinish: Runnable?) {
        AsyncTask.Cancel(OnFinish) // Cancel task
    }

    fun GetTask(): DefaultTask? {
        return AsyncTask.GetTask()
    }
}