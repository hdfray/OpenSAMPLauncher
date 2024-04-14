package com.umnicode.samp_launcher

import android.app.Application
import android.content.Context
import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller


class LauncherApplication : Application() {
    private var _context: Context? = null
    val context: Context
        get() = _context!!

    val userConfig: UserConfig by lazy {
        UserConfig(
            applicationContext,
            applicationContext.getString(R.string.user_config_name)
        )
    }

    val Installer: SAMPInstaller by lazy {
        SAMPInstaller(context)
    }

    override fun onCreate() {
        super.onCreate()
        _context = applicationContext
    }
}
