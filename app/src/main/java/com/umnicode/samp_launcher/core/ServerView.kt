package com.umnicode.samp_launcher.core

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.SAMP.Enums.ServerStatus

class ServerView : LinearLayout {
    private var RootView: View? = null
    private var Config: ServerConfig? = null
    private var Show_IP_Port: Boolean = false
    private var HideInfoWhenServerStatusError: Boolean = false

    constructor(context: Context) : super(context) {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        Init(context)
    }

    fun SetServer(Config: ServerConfig) {
        val statusText: TextView = RootView!!.findViewById(R.id.server_status)
        val nameText: TextView = RootView!!.findViewById(R.id.server_name)
        val webUrlText: TextView = RootView!!.findViewById(R.id.server_weburl)
        val ipPortText: TextView = RootView!!.findViewById(R.id.server_ip_port)
        val versionText: TextView = RootView!!.findViewById(R.id.server_version)
        val playersText: TextView = RootView!!.findViewById(R.id.server_players)
        val timeText: TextView = RootView!!.findViewById(R.id.server_time)
        val mapText: TextView = RootView!!.findViewById(R.id.server_map)
        val modeText: TextView = RootView!!.findViewById(R.id.server_mode)
        val languageText: TextView = RootView!!.findViewById(R.id.server_language)
        val passwordText: TextView = RootView!!.findViewById(R.id.server_password)
        val resources: Resources = RootView!!.getResources()

        // Set status text
        if (Config.Status == ServerStatus.PENDING) {
            statusText.setText(resources.getString(R.string.server_status_pending))
        } else if (Config.Status == ServerStatus.ONLINE) {
            statusText.setText(resources.getString(R.string.server_status_online))
        } else if (Config.Status == ServerStatus.OFFLINE) {
            statusText.setText(resources.getString(R.string.server_status_offline))
        } else if (Config.Status == ServerStatus.NOT_FOUND) {
            statusText.setText(resources.getString(R.string.server_status_not_found))
        } else if (Config.Status == ServerStatus.FAILED_TO_FETCH) {
            statusText.setText(resources.getString(R.string.server_status_failed_to_fetch))
        } else if (Config.Status == ServerStatus.INCORRECT_IP) {
            statusText.setText(resources.getString(R.string.server_status_incorrect_ip))
        }

        // Set status color
        if (ServerConfig.Companion.IsStatusError(Config.Status)) {
            statusText.setTextColor(resources.getColor(R.color.colorError))
        } else if (ServerConfig.Companion.IsStatusNone(Config.Status)) {
            statusText.setTextColor(resources.getColor(R.color.colorNone))
        } else if (ServerConfig.Companion.IsStatusOk(Config.Status)) {
            statusText.setTextColor(resources.getColor(R.color.colorOk))
        }

        // Set server name label
        nameText.setText(CheckProperty(Config.Name, resources))

        // Set web-url ( server site ) label
        webUrlText.setText(CheckProperty(Config.WebURL, resources))

        // Set version label
        versionText.setText(CheckProperty(Config.Version, resources))

        // Set ip/port string
        ipPortText.setText(
            String.format(
                resources.getString(R.string.server_port_ip),
                Config.IP,
                Config.Port
            )
        )

        // Set online players count label
        playersText.setText(
            String.format(
                resources.getString(R.string.server_players),
                Config.OnlinePlayers,
                Config.MaxPlayers
            )
        )

        // Set time label
        timeText.setText(
            String.format(
                resources.getString(R.string.server_time),
                CheckProperty(Config.Time, resources)
            )
        )

        // Set map label
        mapText.setText(
            String.format(
                resources.getString(R.string.server_map),
                CheckProperty(Config.Map, resources)
            )
        )

        // Set mode label
        modeText.setText(
            String.format(
                resources.getString(R.string.server_mode),
                CheckProperty(Config.Mode, resources)
            )
        )

        // Set language label
        languageText.setText(
            String.format(
                resources.getString(R.string.server_language),
                CheckProperty(Config.Language, resources)
            )
        )

        // Set password label
        passwordText.setText(
            String.format(
                resources.getString(R.string.server_password),
                CheckProperty(Config.Password, resources)
            )
        )
        this.Config = Config

        // Update rules
        UpdateRule_HideInfoWhenServerStatusError()
    }

    fun GetServerConfig(): ServerConfig? {
        return Config
    }

    fun SetShowIpPortStatus(IsEnabled: Boolean, UpdateAnyway: Boolean) {
        if (Show_IP_Port != IsEnabled || UpdateAnyway) {
            val ipPortText: TextView = RootView!!.findViewById(R.id.server_ip_port)
            if (!IsEnabled) {
                ipPortText.setVisibility(INVISIBLE) // Hide
            } else {
                ipPortText.setVisibility(VISIBLE) // Show
            }
            Show_IP_Port = IsEnabled
        }
    }

    fun GetShowIpPortStatus(): Boolean {
        return Show_IP_Port
    }

    fun SetHideInfoWhenServerStatusErrorStatus(IsEnabled: Boolean, UpdateAnyway: Boolean) {
        if (HideInfoWhenServerStatusError != IsEnabled || UpdateAnyway) {
            HideInfoWhenServerStatusError = IsEnabled
            UpdateRule_HideInfoWhenServerStatusError()
        }
    }

    fun GetHideInfoWhenServerStatusErrorStatus(): Boolean {
        return HideInfoWhenServerStatusError
    }

    private fun Init(context: Context) {
        RootView = inflate(context, R.layout.server_view, this)
        SetShowIpPortStatus(false, true) // Hide ip:port label
        SetHideInfoWhenServerStatusErrorStatus(true, true) // Hide info labels when error status
    }

    private fun UpdateRule_HideInfoWhenServerStatusError() {
        val InfoLayout: LinearLayout = RootView!!.findViewById(R.id.info_layout)
        if (HideInfoWhenServerStatusError && (Config != null) && ServerConfig.Companion.IsStatusError(
                Config!!.Status
            )
        ) {
            InfoLayout.setVisibility(INVISIBLE)
        } else {
            InfoLayout.setVisibility(VISIBLE)
        }
    }

    companion object {
        private fun CheckProperty(Property: String?, Res: Resources): String? {
            var Property: String? = Property
            if ((Property == "")) Property = Res.getString(R.string.none_string)
            return Property
        }
    }
}