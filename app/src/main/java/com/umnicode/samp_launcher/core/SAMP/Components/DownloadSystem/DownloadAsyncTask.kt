package com.terfess.samp_launcher.core.SAMP.Components.DownloadSystem

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.terfess.samp_launcher.core.SAMP.Components.DefaultTask
import com.terfess.samp_launcher.core.SAMP.Components.ExtendedAsyncTask
import com.terfess.samp_launcher.core.SAMP.Components.TaskFileStatus
import com.terfess.samp_launcher.core.SAMP.Components.TaskStatus
import com.terfess.samp_launcher.core.Utils.RemoveFile
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

internal class DownloadAsyncTask(var Task: DownloadTask) : ExtendedAsyncTask() {
    var Connection: URLConnection? = null
    var ReadingFromBuffer = false
    override fun doInBackground(vararg params: Void?): Void? {
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnStarted() }

        // Check for internet connection
        if (!IsInternetAvailable(Task.Param_PingTimeout)) {
            Log.println(Log.ERROR, "DownloadSystem", "Internet isn't available")
            BroadcastTaskFinished()
            return null
        }

        // If directory doesn't exist - try to create it
        if (!Task.OutDirectory.exists()) {
            if (Task.OutDirectory.mkdirs()) {
                Log.println(
                    Log.INFO,
                    "DownloadSystem",
                    "Output directory doesn't exist, so it was created successfully"
                )
            } else {
                Log.println(
                    Log.ERROR, "DownloadSystem",
                    "Failed to create output directory - " + Task.OutDirectory.toString()
                )
                BroadcastTaskFinished()
                return null
            }
        }

        // Fire event after all init checks are finished
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnChecksFinished() }
        while (Task.FileIndex < Task.Files.size) {
            var Count = 0
            try {
                // Fire event
                Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFileDownloadStarted() }
                val file = Task.Files[Task.FileIndex]
                Connection = file!!.url!!.openConnection()
                this.Connection!!.connect()

                // Check for cancel
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                        isCancelled
                    } else {
                        TODO("VERSION.SDK_INT < CUPCAKE")
                    }
                ) return null

                // Init status
                Task.Status = TaskStatus(0f, -1.0f, Task.FileIndex + 1, Task.Files.size)

                // Fire events
                BroadcastProgressChanged() // Force update status

                // Getting file length
                Task.Status!!.FullSize = Connection!!.getContentLength().toFloat()

                // Get filename of file (if server provides it, otherwise set it to default str)
                var Filename: String
                val contentDispositionRaw = Connection!!.getHeaderField("Content-Disposition")
                if (contentDispositionRaw != null && contentDispositionRaw.contains("filename=")) {
                    Filename = contentDispositionRaw.split("filename=".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                    // Remove all next params in header
                    if (Filename.contains(";")) {
                        Filename = Filename.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                    }

                    // If filename contains spaces ( => quotes ) replace it with _ and remove quotes
                    Filename = Filename.replace(' ', '_')
                    Filename = Filename.replace("\"", "")
                } else {
                    Filename =
                        "_download_" + (Task.FileIndex + 1) + "_" + Task.Files.size // Gen default str. Example: ( _download_1_2 )
                }
                file.OutputFilename = File(Task.OutDirectory, Filename).absoluteFile

                // We don't check the result of file creation because we will do it later ( when open output-stream)
                if (!file.OutputFilename!!.exists()) file.OutputFilename!!.createNewFile() else if (!Task.Flag_OverrideIfExist) { // Check for flag
                    FinishFileDownload(TaskFileStatus.SUCCESSFUL) // Fire event
                    ++Task.FileIndex
                    continue  // Skip this file, it's already downloaded
                }

                // Setup streams
                val Input: InputStream = BufferedInputStream(file.url!!.openStream(), 8192) // input
                val Output: OutputStream = FileOutputStream(file.OutputFilename) // output

                // Check for cancel ( opening streams can be very long operation )
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                        isCancelled
                    } else {
                        TODO("VERSION.SDK_INT < CUPCAKE")
                    }
                ) return null
                Handler(Looper.getMainLooper()).post { Task.Callback!!.OnBufferReadingStarted() } // broadcast event
                ReadingFromBuffer = true
                val Data = ByteArray(1024)
                var Counter = 0
                while (Input.read(Data).also { Count = it } != -1) {
                    // Check for cancel
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                            this.isCancelled
                        } else {
                            TODO("VERSION.SDK_INT < CUPCAKE")
                        }
                    ) return null
                    Task.Status!!.Done += Count.toFloat()
                    Output.write(Data, 0, Count)

                    // Optimization
                    if (Counter == Task.Param_UpdateProgressFrequency) { // Update progress every 64kb
                        Counter = 0
                        BroadcastProgressChanged()
                    } else {
                        Counter++
                    }
                }

                // When finished, broadcast event not considering optimization counter
                if (Counter != Task.Param_UpdateProgressFrequency) BroadcastProgressChanged()
                ReadingFromBuffer = false

                // Flushing output
                Output.flush()

                // Closing streams
                Output.close()
                Input.close()

                // Check for cancel
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                        this.isCancelled
                    } else {
                        TODO("VERSION.SDK_INT < CUPCAKE")
                    }
                ) return null

                // Broadcast finish event
                FinishFileDownload(TaskFileStatus.SUCCESSFUL)
            } catch (e: Exception) {
                Log.e("DownloadSystem", "Error downloading - " + e.message) // Send message to log
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                        !this.isCancelled
                    } else {
                        TODO("VERSION.SDK_INT < CUPCAKE")
                    }
                ) FinishFileDownload(TaskFileStatus.ERROR)
            }
            ++Task.FileIndex
        }

        // Loop increment value after last cycle, so we fix it here
        Task.FileIndex--

        // After last file downloaded we finish task
        BroadcastTaskFinished()
        return null
    }

    // Overrides
    override fun onCancelled() {
        super.onCancelled()
        FinishFileDownload(TaskFileStatus.CANCELED)
        Cleanup(true)
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFinished(true) }

        // Container event
        if (AfterCancelled != null) {
            Handler(Looper.getMainLooper()).post { AfterCancelled!!.run() }
        }
    }

    override fun Cancel(AfterFinished: Runnable?) {
        this.AfterCancelled = AfterCancelled

        // Cancel async-task
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            cancel(true)
        }

        // Close socket connection ( if possible and needed )
        if (Connection != null && !ReadingFromBuffer) {
            if (Connection is HttpURLConnection) {
                try {
                    Thread { (Connection as HttpURLConnection).disconnect() }.start() // Does it safe?
                } catch (e: Exception) {
                    Log.println(Log.ERROR, "DownloadSystem", "Error when cancelling - $e")
                }
            }
        }
    }



    override fun GetTask(): DefaultTask {
        return Task
    }

    // Utils
    override fun Cleanup(IsCancelled: Boolean) {
        if (IsCancelled && Task.Flag_RemoveAllFilesWhenCancelled) {
            for (file in Task.Files) {
                file!!.OutputResult = TaskFileStatus.CANCELED
                RemoveFile(file.OutputFilename)
            }
        }
        if (Task.Flag_ResetTaskAfterFinished || IsCancelled && Task.Flag_ResetTaskWhenCancelled) {
            Task.Reset() // Reset task
        }
    }

    private fun FinishFileDownload(Status: TaskFileStatus) {
        if (Status != TaskFileStatus.SUCCESSFUL) {
            // If flag set - remove failed file from storage
            if (Task.Flag_RemoveFailedFile) {
                RemoveFile(Task.Files[Task.FileIndex]!!.OutputFilename)
            }
        }

        // Set current file status
        Task.Files[Task.FileIndex]!!.OutputResult = Status
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFileDownloadFinished(Status) }
    }

    private fun BroadcastProgressChanged() {
        Handler(Looper.getMainLooper()).post {
            Task.Callback!!.OnProgressChanged(
                Task.Status
            )
        }
    }

    private fun BroadcastTaskFinished() {
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFinished(false) }
    }

    private fun IsInternetAvailable(Timeout: Int): Boolean {
        return try {
            val url = URL("https://google.com")
            val connection = url.openConnection()
            connection.connectTimeout = Timeout
            val stream = connection.getInputStream()
            stream.close()
            true
        } catch (e: Exception) {
            Log.e("DownloadSystem", "Ping failed - " + e.message)
            false
        }
    }
}