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
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

/**
 * Этот класс используется для предоставления приложению доступа к MQTT
 * @property prefs настройки приложения
 * @property context контекст приложения
 * @property connectionStatus статус соединения с брокером
 * @property dynamic принимаемая аудиозапись
 *
 * @author Ipatov Nikita
 * @since 1.0
 */
class MQTTService @Inject constructor(
    val prefs: SharedPreferences,
    @ApplicationContext val context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private var serverID = prefs.getString("server_address", "0.0.0.0") ?: "0.0.0.0"
    private var serverPort = prefs.getString("port", "1883")?.toInt() ?: 1883
    private var clientID = prefs.getString("username", "") ?: ""
    private var password = prefs.getString("password", "") ?: ""
    private var micTopic = prefs.getString("mic_topic_name", "") ?: ""
    private var dynTopic = prefs.getString("dyn_topic_name", "") ?: ""
    private var mqttClient = buildClient()
    val connectionStatus = MutableLiveData(context.getString(R.string.connection_status_not_connected))
    val dynamic = MutableLiveData<ByteArray>()

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
        connect()
    }

    /**
     * Этот метод используется для отправки аудиозаписи на сервер.
     * Данные сервера хранятся в SharedPreferences.
     *
     * @param payload данные в бинарном формате
     */
    fun send(payload: ByteArray){
        if(connectionStatus.value == context.getString(R.string.connection_status_connected)){
            mqttClient.publishWith()
                .topic(micTopic)
                .payload(payload)
                .qos(MqttQos.EXACTLY_ONCE)
                .send()
        }
    }

    /**
     * Этот метод должен вызываться при заверешении работы с сервисом.
     */
    fun shutDown(){
        mqttClient.unsubscribeWith()
            .topicFilter(dynTopic)
            .send()
        mqttClient.disconnect()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        mqttClient = when(key){
            "server_address" -> {
                serverID = "mqtt://" + prefs.getString("server_address", "0.0.0.0")
                buildClient()
            }
            "port" -> {
                serverPort = prefs.getString("port", "1883")?.toInt() ?: 1883
                buildClient()
            }
            "username" -> {
                clientID = prefs.getString("username", "") ?: ""
                buildClient()
            }
            "password" -> {
                password = prefs.getString("password", "") ?: ""
                buildClient()
            }
            "mic_topic_name" -> {
                micTopic = prefs.getString("mic_topic_name", "") ?: ""
                mqttClient
            }
            "din_topic_name" -> {
                dynTopic = prefs.getString("dyn_topic_name", "") ?: ""
                subToAudioinput()
                mqttClient
            }

            else -> mqttClient
        }
        connect()
    }

    /**
     * Этот метод используется для установления связи с MQTT сервером.
     */
    private fun connect(){
        connectionStatus.postValue(context.getString(R.string.connection_status_connection))
        mqttClient.connect().whenComplete{
            con, throwable ->
            val message = if(con.reasonCode == Mqtt5ConnAckReasonCode.SUCCESS)
                R.string.connection_status_connected
            else
                R.string.connection_status_not_connected
            connectionStatus.postValue(
                context.getString(message)
            )
            throwable?.let {
                Log.e("mqtt", it.message.toString())
            }
            if(con.reasonCode == Mqtt5ConnAckReasonCode.SUCCESS)
                subToAudioinput()
        }
    }

    /**
     * Этот метод используется для подписки на аудиосообщения брокера.
     */
    private fun subToAudioinput(){
        mqttClient.subscribeWith()
            .topicFilter(dynTopic)
            .qos(MqttQos.EXACTLY_ONCE)
            .callback {
                it.payload.ifPresent {
                    dynamic.postValue(it.array())
                }
            }
            .send()
    }

    /**
     * Этот метод используется для построения MQTT клиента.
     */
    private fun buildClient(): Mqtt5AsyncClient {
        val builder =  Mqtt5Client.builder()
            .serverHost(serverID)
            .serverPort(serverPort)
            .identifier(UUID.randomUUID().toString())
        if(clientID.isNotBlank() and password.isNotBlank())
            builder
                .simpleAuth()
                .username(clientID)
                .password(password.toByteArray())
                .applySimpleAuth()
        return builder.buildAsync()
    }
}