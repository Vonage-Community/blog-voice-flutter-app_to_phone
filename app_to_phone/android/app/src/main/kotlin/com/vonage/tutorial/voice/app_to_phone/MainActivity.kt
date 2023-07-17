package com.vonage.tutorial.voice.app_to_phone

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import com.vonage.android_core.VGClientConfig
import com.vonage.clientcore.core.api.ClientConfigRegion
import com.vonage.voice.api.CallId
import com.vonage.voice.api.VoiceClient
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private lateinit var client: VoiceClient
    private var onGoingCallID: CallId? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        initClient()
        addFlutterChannelListener()
    }

    private fun initClient() {
        client = VoiceClient(this)
        client.setConfig(VGClientConfig(ClientConfigRegion.US))

        client.setSessionErrorListener {
            notifyFlutter(SdkState.ERROR)
        }

        client.setReconnectingListener {
            notifyFlutter(SdkState.WAIT)
        }
    }

    private fun addFlutterChannelListener() {
        flutterEngine?.dartExecutor?.binaryMessenger?.let {
            MethodChannel(it, "com.vonage").setMethodCallHandler { call, result ->

                when (call.method) {
                    "loginUser" -> {
                        val token = requireNotNull(call.argument<String>("token"))
                        loginUser(token)
                        result.success("")
                    }
                    "makeCall" -> {
                        makeCall()
                        result.success("")
                    }
                    "endCall" -> {
                        endCall()
                        result.success("")
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        }
    }

    private fun loginUser(token: String) {
        client.createSession(token) { err, sessionId ->
            when(err) {
                null -> notifyFlutter(SdkState.LOGGED_IN)
                else -> notifyFlutter(SdkState.ERROR) // handle error

            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun makeCall() {
        notifyFlutter(SdkState.WAIT)

        client.serverCall(mapOf("to" to "PHONE_NUMBER")) {
                err, outboundCall ->
            when {
                err != null -> {
                    notifyFlutter(SdkState.ERROR)
                } else -> {
                    onGoingCallID = outboundCall
                    notifyFlutter(SdkState.ON_CALL)
                }
            }
        }
    }

    private fun endCall() {
        notifyFlutter(SdkState.WAIT)

        onGoingCallID?.let {
            client.hangup(it) {
                    err ->
                when {
                    err != null -> {
                        notifyFlutter(SdkState.ERROR)
                    } else -> {
                        notifyFlutter(SdkState.LOGGED_IN)
                        onGoingCallID = null
                    }
                }
            }
        }
    }

    private fun notifyFlutter(state: SdkState) {
        Handler(Looper.getMainLooper()).post {
            flutterEngine?.dartExecutor?.binaryMessenger?.let {
                MethodChannel(it, "com.vonage")
                    .invokeMethod("updateState", state.toString())
            }
        }
    }
}

enum class SdkState {
    LOGGED_OUT,
    LOGGED_IN,
    WAIT,
    ON_CALL,
    ERROR
}