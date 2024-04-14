package com.umnicode.samp_launcher

import android.content.Context
import com.google.gson.Gson
import com.umnicode.samp_launcher.core.ServerConfig

class UserConfig internal constructor(
    private val _Context: Context,
    private val ConfigName: String
) {
    var IsSetup = false
    var Nickname: String? = ""
    var PingTimeout = 0
    var ServerList = ArrayList<ServerConfig>()

    init {
        Load(ConfigName)
    }

    fun Reload() {
        Load(ConfigName)
    }

    fun Load(ConfigName: String?) {
        val Prefs = _Context.getSharedPreferences(ConfigName, Context.MODE_PRIVATE)
        IsSetup = Prefs.getBoolean("IsSetup", false)
        Nickname = Prefs.getString("Nickname", "")
        PingTimeout = Prefs.getInt("PingTimeout", 3000)

        // Load list
        ServerList.clear()
        val gson = Gson()
        try {
            ServerList = gson.fromJson(Prefs.getString("Servers", "[]"), ServerList.javaClass)
        } catch (ignore: Exception) {
        }
    }

    fun Save() {
        SaveAs(ConfigName)
    }

    fun SaveAs(ConfigName: String?) {
        val Prefs = _Context.getSharedPreferences(ConfigName, Context.MODE_PRIVATE)
        val PrefsEditor = Prefs.edit()
        PrefsEditor.putBoolean("IsSetup", IsSetup)
        PrefsEditor.putString("Nickname", Nickname)
        PrefsEditor.putInt("PingTimeout", PingTimeout)

        // Save list of servers
        val gson = Gson()
        val JsonStr = gson.toJson(ServerList)
        PrefsEditor.putString("Servers", JsonStr)
        PrefsEditor.apply()
    }
}