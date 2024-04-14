package com.umnicode.samp_launcher.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.umnicode.samp_launcher.core.SAMP.Enums.ServerStatus
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.Locale

class ServerConfig {
    var IP = ""
    var Port = 0
    var Name = ""
    var Password = ""
    var Version = ""
    var WebURL = ""
    var Time = ""
    var OnlinePlayers = 0
    var MaxPlayers = 0
    var Mode = ""
    var Map = ""
    var Language = ""
    var Status = ServerStatus.NONE

    constructor()
    constructor(Status: ServerStatus) {
        this.Status = Status
    }

    companion object {
        fun IsIPCorrect(IP: String): Boolean {
            if (IP.isEmpty()) return false
            try {
                val Parts = IP.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (Parts.size != 4) return false
                for (str: String in Parts) {
                    val i = str.toInt()
                    if (i < 0 || i > 255) return false
                }
                return true
            } catch (nfe: NumberFormatException) {
                return false
            }
        }

        // Utils
        private fun SafeJsonGet(Name: String, Object: JsonObject): String {
            var Str = Object[Name].asString
            if (Str == null) Str = ""
            return Str
        }

        private fun SafeJsonToInt(PropName: String, Object: JsonObject): Int {
            try {
                val Str = Object[PropName].asString
                if (Str != null) {
                    return Str.toInt()
                }
            } catch (ignore: NumberFormatException) {
                Log.println(Log.ERROR, "ServerConfig", "NumberFormatException in $PropName")
            }
            return 0
        }

        fun Resolve(
            IP: String,
            Port: Int,
            PingTimeout: Int,
            context: Context?,
            Callback: ServerResolveCallback
        ) {
            if (IsIPCorrect(IP)) {
                // Big thanks guys from sacnr.com for their public ip; TODO: Write custom request system
                val url = String.format(
                    Locale.UK,
                    "http://monitor.sacnr.com/api/?IP=%s&Port=%d&Action=info&Format=JSON",
                    IP,
                    Port
                )
                val queue = Volley.newRequestQueue(context)
                val request = StringRequest(
                    Request.Method.GET, url,
                    object : Response.Listener<String?> {
                        override fun onResponse(response: String?) {
                            if (response == null) {
                                Handler(Looper.getMainLooper()).post({
                                    Callback.OnFinish(
                                        ServerConfig(ServerStatus.FAILED_TO_FETCH)
                                    )
                                })
                            } else {
                                if ((response == "Unknown Server ID")) { // Check for not found error
                                    Handler(Looper.getMainLooper()).post({
                                        Callback.OnFinish(
                                            ServerConfig(ServerStatus.NOT_FOUND)
                                        )
                                    })
                                } else {
                                    val Config = ServerConfig()

                                    // Parse request as JSON ( we set target format as JSON early )
                                    try {
                                        val Object = JsonParser.parseString(response).asJsonObject

                                        // Get props name from object
                                        Config.IP = IP
                                        Config.Port = Port
                                        Config.Name = Object["Hostname"].asString
                                        val Password = SafeJsonGet("Password", Object)
                                        if ((Password == "0") || (Password == "")) {
                                            Config.Password = ""
                                        } else {
                                            Config.Password = Password
                                        }
                                        Config.Version = SafeJsonGet("Version", Object)
                                        Config.WebURL = SafeJsonGet("WebURL", Object)
                                        Config.Time = SafeJsonGet("Time", Object)

                                        // Players count
                                        Config.OnlinePlayers = SafeJsonToInt("Players", Object)
                                        Config.MaxPlayers = SafeJsonToInt("MaxPlayers", Object)
                                        Config.Map = SafeJsonGet("Map", Object)
                                        Config.Mode = SafeJsonGet("Gamemode", Object)
                                        Config.Language = SafeJsonGet("Language", Object)
                                        Config.Status = ServerStatus.PENDING
                                    } catch (ex: JsonParseException) {
                                        Log.println(
                                            Log.ERROR,
                                            "ServerConfig",
                                            "Error parse - $response"
                                        )
                                        Config.Status = ServerStatus.FAILED_TO_FETCH
                                    }
                                    Handler(Looper.getMainLooper()).post({ Callback.OnFinish(Config) }) // Finish event

                                    // Ping
                                    val ping = Thread(object : Runnable {
                                        override fun run() {
                                            try {
                                                val Socket = Socket()
                                                Socket.connect(
                                                    InetSocketAddress(
                                                        InetAddress.getByName(
                                                            IP
                                                        ), 80
                                                    ), PingTimeout
                                                )
                                                Config.Status = ServerStatus.ONLINE
                                            } catch (ex: UnknownHostException) {
                                                Config.Status = ServerStatus.OFFLINE
                                            } catch (ex: IOException) {
                                                Config.Status = ServerStatus.OFFLINE
                                            }

                                            // Run on main thread
                                            Handler(context!!.mainLooper).post(Runnable {
                                                Handler(Looper.getMainLooper()).post(
                                                    { Callback.OnPingFinish(Config) })
                                            })
                                        }
                                    })
                                    ping.start()
                                }
                            }
                        }
                    }, object : Response.ErrorListener {
                        override fun onErrorResponse(error: VolleyError) {
                            Handler(Looper.getMainLooper()).post({
                                Callback.OnFinish(
                                    ServerConfig(
                                        ServerStatus.FAILED_TO_FETCH
                                    )
                                )
                            })
                        }
                    }
                )
                queue.add(request)
            } else {
                Handler(Looper.getMainLooper()).post({ Callback.OnFinish(ServerConfig(ServerStatus.INCORRECT_IP)) }) // Send server with FAILED_TO_FETCH status
            }
        }

        fun IsStatusError(Status: ServerStatus?): Boolean {
            return ((Status != ServerStatus.ONLINE) && (Status != ServerStatus.OFFLINE) && (Status != ServerStatus.PENDING))
        }

        fun IsStatusNone(Status: ServerStatus?): Boolean {
            return Status == ServerStatus.PENDING
        }

        fun IsStatusOk(Status: ServerStatus?): Boolean {
            return Status == ServerStatus.ONLINE
        }
    }
}