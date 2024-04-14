package com.terfess.samp_launcher.core.SAMP.Components

import android.os.AsyncTask

abstract class ExtendedAsyncTask : AsyncTask<Void?, Void?, Void?>() {
    var AfterCancelled: Runnable? = null
    abstract fun Cancel(AfterFinished: Runnable?)
    abstract fun GetTask(): DefaultTask?
    protected abstract fun Cleanup(IsCancelled: Boolean)
}