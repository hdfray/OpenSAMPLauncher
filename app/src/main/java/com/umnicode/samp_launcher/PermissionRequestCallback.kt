package com.terfess.samp_launcher

fun interface PermissionRequestCallback {
    fun Finished(IsGranted: Boolean)
}