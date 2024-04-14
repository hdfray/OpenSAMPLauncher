package com.umnicode.samp_launcher.core

open interface ServerResolveCallback {
    fun OnFinish(OutConfig: ServerConfig?)
    fun OnPingFinish(OutConfig: ServerConfig?)
}