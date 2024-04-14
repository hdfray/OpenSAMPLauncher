package com.umnicode.samp_launcher.ui.home


import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.UserConfig
import com.umnicode.samp_launcher.core.ServerConfig
import com.umnicode.samp_launcher.core.ServerResolveCallback
import com.umnicode.samp_launcher.core.ServerView
import com.umnicode.samp_launcher.ui.widgets.playbutton.PlayButton

class HomeFragment : Fragment() {
    private lateinit var rootView: View

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceStatus: Bundle?
    ): View {
        // Get shared preferences
        val sharedPreferences: SharedPreferences? = this.context?.getSharedPreferences("HomeFragment", Context.MODE_PRIVATE);
        val preferencesEditor: SharedPreferences.Editor? = sharedPreferences?.edit();

        this.rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Set nickname text
        val nicknameText: TextView = this.rootView.findViewById(R.id.nickname)

        val launcherApplication: LauncherApplication = activity?.application as LauncherApplication;
        nicknameText.text = launcherApplication.userConfig?.Nickname;

        // Set port filter
        val portEditText: EditText = this.rootView.findViewById(R.id.port);
        portEditText.filters = Array(1) { PortFilter() };

        // Bind ip and port text edit fields
        val ipEditText: EditText = this.rootView.findViewById(R.id.ip);
        val passwordEditText:EditText = this.rootView.findViewById(R.id.password);

        // Restore from preferences
        if (sharedPreferences != null){
            ipEditText.setText(sharedPreferences.getString(R.id.ip.toString(), ""));
            portEditText.setText(sharedPreferences.getString(R.id.port.toString(), ""));
            passwordEditText.setText(sharedPreferences.getString(R.id.password.toString(), ""));
        }

        ipEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                preferencesEditor?.putString(R.id.ip.toString(), s.toString());
                preferencesEditor?.apply();
            }

            override fun afterTextChanged(s: Editable?) {
                updateServerConfig();
            }
        });
        portEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                preferencesEditor?.putString(R.id.port.toString(), s.toString());
                preferencesEditor?.apply();
            }

            override fun afterTextChanged(s: Editable?) {
                updateServerConfig();
            }
        });
        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                preferencesEditor?.putString(R.id.password.toString(), s.toString());
                preferencesEditor?.apply();
            }

            override fun afterTextChanged(s: Editable?) {
                updateServerConfig();
            }
        });

        // Setup play button
        val playButton = rootView.findViewById<Button>(R.id.play_btn)


        this.updateServerConfig();
        return this.rootView;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateServerConfig(){
        val ipEdit:EditText = this.rootView.findViewById(R.id.ip);
        val portEdit:EditText = this.rootView.findViewById(R.id.port);
        val userConfig: UserConfig? = (activity?.application as LauncherApplication).userConfig;

        val IP:String = ipEdit.text.toString();
        val port:Int;

        if (portEdit.text.isNotEmpty()){
            port = portEdit.text.toString().toInt();
        }else{
            port = 0;
        }

        // Resolve server
        userConfig?.PingTimeout?.let {
            ServerConfig.Resolve(IP, port, it, this.context, object : ServerResolveCallback {
                override fun OnFinish(OutConfig: ServerConfig?)  {
                    // Update ServerView
                    val serverView: ServerView = rootView.findViewById(R.id.server_view);
                    serverView.SetServer(OutConfig!!);

                    val playButton: PlayButton = rootView.findViewById<Button>(R.id.play_btn) as PlayButton
                    playButton.SetServerConfig(OutConfig);
                }

                override fun OnPingFinish(OutConfig: ServerConfig?) {
                    // Update ServerView (again)
                    val serverView: ServerView = rootView.findViewById(R.id.server_view);
                    serverView.SetServer(OutConfig!!);
                }
            })
        };
    }
}