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

import javax.inject.Inject

/**
 * Этот класс используется для записи и отпраки звука на сервер.
 *
 * @author Ipatov Nikita
 * @since 1.0
 */
class VoiceRecorder @Inject constructor(val service: MQTTService) {

    private var record: ByteArray = ByteArray(4)

    /**
     * Этот метод используется для начала записи звука.
     */
    fun startRecord(){

    }

    /**
     * Этот метод используется для окончания записи звука и отправки данных на сервер.
     */
    fun sendRecord(){
        service.send(record)
    }
}