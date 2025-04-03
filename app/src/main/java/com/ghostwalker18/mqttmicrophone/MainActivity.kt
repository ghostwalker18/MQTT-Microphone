/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ghostwalker18.mqttmicrophone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import com.ghostwalker18.mqttmicrophone.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Этот класс представляет собой основной экран приложения.
 * * @author Ipatov Nikita
 *  * @since 1.0
 *
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var notStarted = true
    @Inject lateinit var recorder: VoiceRecorder
    @Inject lateinit var player: VoicePlayer
    @Inject lateinit var mqttService: MQTTService
    @Inject lateinit var prefs: SharedPreferences

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            val toast = Toast.makeText(this,
                resources.getText(R.string.permission_for_audio_record), Toast.LENGTH_SHORT
            )
            toast.show()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        setupRecordMode()
        mqttService.connectionStatus.observe(this){
            status -> binding.connectionStatus.text = status
        }
        player.startWorking(this)
        player.isPlaying.observe(this) {
            if(it){
                binding.mic.icon = AppCompatResources.getDrawable(this, R.drawable.baseline_stop_96)
                binding.mic.setOnClickListener {
                    player.stopPlaying()
                }
                binding.speaking.text = getString(R.string.recieved_answer)
                binding.speaking.visibility = View.VISIBLE
            }
            else{
                binding.mic.icon = AppCompatResources.getDrawable(this, R.drawable.baseline_mic_96)
                setupRecordMode()
                binding.speaking.text = getString(R.string.speaking)
                binding.speaking.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else ->  super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        try {
            player.stopPlaying()
            player.shutDown()
            mqttService.shutDown()
        } catch (_: Exception){/*Not required*/}
        super.onStop()
    }

    /**
     * Этот метод настраивает режим аудиозаписи.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecordMode(){
        when(prefs.getString("record_mode", "hold")){
            "hold" -> {
                binding.mic.setOnTouchListener { _, motionEvent ->
                    when(motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            binding.speaking.visibility = View.VISIBLE
                            recorder.startRecord()
                        }
                        MotionEvent.ACTION_UP -> {
                            binding.speaking.visibility = View.GONE
                            recorder.sendRecord()
                        }
                    }
                    true
                }
            }
            "tap" -> {
                binding.mic.setOnClickListener {
                    binding.speaking.visibility = if (notStarted) View.VISIBLE else View.GONE
                    if(notStarted){
                        recorder.startRecord()
                    } else {
                        recorder.sendRecord()
                    }
                    notStarted = !notStarted
                }
            }
        }
    }
}