package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.umnicode.samp_launcher.core.SAMP.Components.DefaultTask
import com.umnicode.samp_launcher.core.SAMP.Components.ExtendedAsyncTask
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus
import com.umnicode.samp_launcher.core.Utils.GetFileNameWithoutExtension
import com.umnicode.samp_launcher.core.Utils.RemoveFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Collections
import java.util.Objects
import java.util.zip.ZipFile

internal class ExtractAsyncTask(val Task: ExtractTask) : ExtendedAsyncTask() {
    override fun doInBackground(vararg params: Void?): Void? {
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnStarted() }
        while (Task.FileIndex < Task.Files!!.size) {
            val file = Task.Files!![Task.FileIndex]

            // Check for cancel
            if (this.isCancelled) return null

            // File extract start event
            Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFileExtractStarted(file) }

            // Init status
            Task.Status = TaskStatus(0f, -1.0f, Task.FileIndex + 1, Task.Files!!.size)

            // Checks
            if (!file!!.Filepath!!.exists() || !file.Filepath!!.isFile) {
                Log.e("ExtractSystem", "Incorrect filepath - " + file.Filepath.toString())
                FinishFileExtract(TaskFileStatus.ERROR)
                ++Task.FileIndex
                continue
            }
            if (file.OutputDirectory!!.exists() && !file.OutputDirectory!!.isDirectory) {
                Log.e(
                    "ExtractSystem",
                    "OutputDirectory is not a directory - " + file.OutputDirectory.toString()
                )
                FinishFileExtract(TaskFileStatus.ERROR)
                ++Task.FileIndex
                continue
            }

            // Broadcast event
            Handler(Looper.getMainLooper()).post { Task.Callback!!.OnChecksFinished() }

            // Setup out directory
            var OutDir: File?

            // Create containing folder if needed
            OutDir = if (file.CreateContainingDirectory) {
                File(
                    file.OutputDirectory, GetFileNameWithoutExtension(
                        file.Filepath!!, false
                    )
                )
            } else file.OutputDirectory

            // Create dirs ( if they don't exist )
            if (!OutDir!!.exists()) {
                if (!OutDir.mkdirs()) {
                    Log.e(
                        "ExtractSystem",
                        "Failed to create dirs - " + File(OutDir, "dummy.value").toString()
                    )
                    FinishFileExtract(TaskFileStatus.ERROR)
                    ++Task.FileIndex
                    continue
                } else {
                    Log.i("ExtractSystem", "Created directories for path - $OutDir") // Log
                }
            }

            // Check for cancel
            if (this.isCancelled) return null

            // Determine archive type
            val Type = ArchiveComponent.GetTypeOfArchive(file.Filepath)
            var Result = TaskFileStatus.SUCCESSFUL

            // Extract archive depend on its type
            if (Type == ArchiveType.ZIP) {
                try {
                    // Extract zip
                    val Zip = ZipFile(file.Filepath)
                    val Entries = Collections.list(Zip.entries())

                    // Init done/full size
                    Task.Status!!.Done = 0f
                    for (zipEntry in Entries) { // Iterate over zip file to get size of entries
                        if (!zipEntry.isDirectory) Task.Status!!.FullSize += zipEntry.size.toFloat()
                    }

                    // Notify that we initiated status ( done/full size )
                    BroadcastProgressChanged()
                    for (Entry in Entries) {
                        // Check for cancel
                        if (this.isCancelled) return null
                        val Path = File(OutDir, Entry.name)
                        if (Entry.isDirectory) {
                            // Create new dirs ( if path don't exist )
                            if (!Path.exists() && !Path.mkdirs()) {
                                Result = TaskFileStatus.ERROR
                                throw Exception("Can't create directory - $Path")
                            }
                        } else {
                            // Skip current entry if flag set
                            if (Path.exists() && Path.length() == Entry.size && !Task.Flag_OverrideIfExist) {
                                // Add entry size to done ( because we skip entry )
                                Task.Status!!.Done += Entry.size.toFloat()
                                BroadcastProgressChanged()
                                continue
                            }
                            try {
                                // Open streams
                                val Input = Zip.getInputStream(Entry) // Already unzipped file

                                // Copy content from zip to output file
                                val Output: OutputStream = FileOutputStream(Path)
                                val Buffer = ByteArray(1024)
                                var Len: Int
                                var Counter = 0
                                while (Input.read(Buffer).also { Len = it } > 0) {
                                    if (this.isCancelled) return null

                                    // Write buffer to out stream
                                    Output.write(Buffer, 0, Len)
                                    Task.Status!!.Done += Len.toFloat()

                                    // Basic optimization trick
                                    if (Counter == Task.Param_UpdateProgressFrequency) { // As default this value set to 64 KB
                                        BroadcastProgressChanged()
                                        Counter = 0
                                    } else {
                                        Counter++
                                    }
                                }

                                // Close output stream
                                Output.flush()
                                Output.close()

                                // Close input streams
                                Input.close()

                                // Update status after timer
                                if (Counter != Task.Param_UpdateProgressFrequency) BroadcastProgressChanged()
                            } catch (ex: IOException) {
                                Log.e(
                                    "ZipToolkit",
                                    "Can't write file - " + Path.toString() + "; Reason - " + ex.message
                                )
                                Result = TaskFileStatus.ERROR
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ZipToolkit", Objects.requireNonNull(e.message)!!)
                }
            }

            // File extract finished
            FinishFileExtract(Result)
            ++Task.FileIndex
        }
        BroadcastOnFinished(false)
        return null
    }

    override fun onCancelled() {
        super.onCancelled()
        FinishFileExtract(TaskFileStatus.CANCELED)
        Cleanup(true)
        BroadcastOnFinished(true)

        // Container event
        AfterCancelled?.run()
    }

    override fun Cancel(AfterFinished: Runnable?) {
        this.AfterCancelled = AfterCancelled
        cancel(true)
    }

    override fun GetTask(): DefaultTask? {
        return null
    }

    override fun Cleanup(IsCancelled: Boolean) {
        // Default implementation of clean-up. Very similar to DownloadAsyncTask implementation
        if (IsCancelled && Task.Flag_RemoveAllFilesWhenCancelled) {
            for (file in Task.Files!!) {
                file!!.OutputResult = TaskFileStatus.CANCELED
                RemoveFile(file.OutputDirectory)
            }
        }
        if (Task.Flag_ResetTaskAfterFinished || IsCancelled && Task.Flag_ResetTaskWhenCancelled) {
            Task.Reset() // Reset task
        }
    }

    // Utils
    private fun FinishFileExtract(Status: TaskFileStatus) {
        if (Status != TaskFileStatus.SUCCESSFUL) {
            // If flag set - remove failed file from storage
            if (Task.Flag_RemoveFailedFile) {
                RemoveFile(Task.Files!![Task.FileIndex]!!.OutputDirectory)
            }
        }

        // Set current file status
        val Current = Task.Files!![Task.FileIndex]
        Current!!.OutputResult = Status
        Handler(Looper.getMainLooper()).post {
            Task.Callback!!.OnFileExtractFinished(
                Current,
                Status
            )
        }
    }

    private fun BroadcastProgressChanged() {
        Handler(Looper.getMainLooper()).post {
            Task.Callback!!.OnProgressChanged(
                Task.Status
            )
        }
    }

    private fun BroadcastOnFinished(IsCancelled: Boolean) {
        Handler(Looper.getMainLooper()).post { Task.Callback!!.OnFinished(IsCancelled) }
    }
}