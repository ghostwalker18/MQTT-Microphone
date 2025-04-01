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
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioSource
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject


/**
 * Этот класс используется для записи и отпраки звука на сервер.
 * @property service транспортный сервис
 * @property context контекст приложения
 *
 * @author Ipatov Nikita
 * @since 1.0
 */
class VoiceRecorder @Inject constructor(
    val service: MQTTService,
    @ApplicationContext val context: Context
) {

    private var recorder: MediaRecorder? = null
    private val fileName = context.cacheDir.absolutePath + "/record.3gp"

    /**
     * Этот метод используется для начала записи звука.
     */
    fun startRecord(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED){
            recorder = if (Build.VERSION.SDK_INT < 31) MediaRecorder() else MediaRecorder(context)
            val outputFile = File(fileName)
            if (outputFile.exists())
                outputFile.delete()
            recorder?.apply {
                setAudioSource(AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }
        }
    }

    /**
     * Этот метод используется для окончания записи звука и отправки данных на сервер.
     */
     fun sendRecord(){
        recorder?.stop()
        recorder?.release()
        val outputFile = File(fileName)
        service.send(outputFile.readBytes())
        outputFile.delete()
    }
}