package com.umnicode.samp_launcher

fun interface PermissionRequestCallback {
    fun Finished(IsGranted: Boolean)
}