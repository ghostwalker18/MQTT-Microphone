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

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Этот класс используется для получения и проигрывания аудиозаписи с сервера.
 * @property service транспортный сервис
 * @property context контекст приложения
 * @property isPlaying воспроизводится ли аудио в текущий момент.
 *
 * @author Ipatov Nikita
 * @since 1.0
 */
class VoicePlayer @Inject constructor(
    val service: MQTTService,
    @ApplicationContext val context: Context
) {
    private val fileName = context.cacheDir.absolutePath + "/received_audio.3gp"
    private val inputFile = File(fileName)
    private val player: MediaPlayer = MediaPlayer()
    val isPlaying = MutableLiveData(false)

    init {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        player.setOnCompletionListener {
            isPlaying.postValue(false)
        }
    }

    /**
     * Этот метод используется для начала работы сервиса.
     */
    fun startWorking(owner: LifecycleOwner){
        service.dynamic.observe(owner) {
            if (inputFile.exists()) inputFile.delete()
            inputFile.writeBytes(it)
            player.reset()
            player.setDataSource(fileName)
            player.prepare()
            isPlaying.postValue(true)
            player.start()
        }
    }

    /**
     * Этот метод используется для остановки воспроизведения.
     */
    fun stopPlaying(){
        isPlaying.postValue(false)
        if (player.isPlaying)
            player.stop()
        val file = File(fileName)
        if (inputFile.exists()) file.delete()
    }

    /**
     * Этот метод используется для освобождения ресурсов.
     */
    fun shutDown(){
        player.release()
        if (inputFile.exists()) inputFile.delete()
    }
}