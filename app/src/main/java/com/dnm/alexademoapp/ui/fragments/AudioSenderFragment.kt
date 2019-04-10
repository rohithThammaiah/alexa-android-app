package com.dnm.alexademoapp.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.widget.Toast
import com.dnm.alexademoapp.BuildConfig
import com.dnm.alexademoapp.R
import com.dnm.alexademoapp.ui.BaseListenerFragment
import com.jamitlabs.alexaconnect.libraries.alexa.requestbody.DataRequestBody
import kotlinx.android.synthetic.main.activity_audio_sender.view.*
import com.jamitlabs.alexaconnect.libraries.speechutils.RawAudioRecorder
import okio.BufferedSink
import java.io.IOException


class AudioSenderFragment : BaseListenerFragment() {


    private var vw: View? = null

    private val TAG = "AudioSenderFragment";
    private val AUDIO_RATE = 16000
    val ARGUMENT_PROFILE_INFO = "profile_info"

    private val actionDownDrawable = R.drawable.ic_mic_black_24dp
    private val actionUpDrawable = R.drawable.ic_mic_none_black_56dp
    private val actionPermissionDenied = R.drawable.ic_mic_off_black_56dp

    private val permissionsRequestCode = 116
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    private var rawAudioRecorder: RawAudioRecorder? = null

    override fun getTitle(): String {
        return "Send Audio"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vw = View.inflate(context, R.layout.activity_audio_sender, null)


        vw?.microphone?.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startListening()
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    stopListening()
                    return@setOnTouchListener true
                }

                else -> {
                    return@setOnTouchListener false
                }
            }
        }

        vw?.microphone?.setIndicatorColor(R.color.colorAccent)


        return vw
    }



    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private val requestBody = object : DataRequestBody() {
        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            while (rawAudioRecorder != null && rawAudioRecorder?.isPausing != true) {
                if (rawAudioRecorder != null) {
                    val rmsdb = rawAudioRecorder?.rmsdb
                    if (sink != null && rawAudioRecorder != null) {
                        sink.write(rawAudioRecorder?.consumeRecording())

                    }
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "Received Audio")
                        Log.i(TAG, "RMSDB: $rmsdb")
                    }

                    activity?.runOnUiThread {
                        vw?.microphone?.setRmsdbLevel(rmsdb?:0.0f)
                    }
                }
                try {
                    Thread.sleep(25)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
            stopListening()
        }
    }

    override fun startListening() {
        //vw?.microphone?.setBackgroundResource(actionDownDrawable)
        if (rawAudioRecorder == null) {
            rawAudioRecorder = RawAudioRecorder(AUDIO_RATE)
        }
        rawAudioRecorder?.start()
        alexaManager.sendAudioRequest(requestBody, requestCallback)
    }

    private fun stopListening() {

        if (rawAudioRecorder != null) {
            rawAudioRecorder?.stop()
            rawAudioRecorder?.release()
            rawAudioRecorder = null
        }
    }

    private fun checkPermissions() {

        if (ContextCompat.checkSelfPermission(context!!, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, permissionsRequestCode)
        } else {

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when (requestCode) {
            permissionsRequestCode -> {
                for (i in 0 until permissions.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        if (i == permissions.size - 1) {

                        }
                    } else {
                        Toast.makeText(context, "Please Grant ${permissions[i]}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}