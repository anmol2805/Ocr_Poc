package com.anmol2805.ocrpoc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_result.text = intent.getStringExtra("image")
        tts = TextToSpeech(this, this)
        speak.setOnClickListener {
            intent.getStringExtra("image")?.let {
                tts!!.speak(it, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                Log.e("lang error", "language not found")
        }else{
            Log.e("init error","error")
        }
    }

    override fun onDestroy() {
        tts?.apply {
            stop()
            shutdown()
        }
        super.onDestroy()

    }
}