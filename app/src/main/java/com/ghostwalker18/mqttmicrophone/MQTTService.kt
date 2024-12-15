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
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject
import kotlin.jvm.Throws

/**
 * Этот класс используется для предоставления приложению доступа к MQTT
 *
 * @author Ipatov Nikita
 * @since 1.0
 */
class MQTTService @Inject constructor(val prefs: SharedPreferences,
                                      @ApplicationContext val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var serverID = prefs.getString("server_address", "0.0.0.0")
    private var serverPort = prefs.getInt("port", 1883)
    private var clientID = prefs.getString("username", "")
    private var password = prefs.getString("password", "")
    private var topic = prefs.getString("topic_name", "")
    private var mqttClient = MqttAndroidClient(context, serverID, clientID)
    val connectionStatus = MutableLiveData(context.getString(R.string.connection_status_not_connected))

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
        connect()
    }

    /**
     * Этот метод используется для отправки сообщения на сервер.
     * Данные сервера хранятся в SharedPreferences.
     *
     * @param payload данные в бинарном формате
     */
    @Throws(MqttException::class)
    fun send(payload: ByteArray){
        if(connectionStatus.value == context.getString(R.string.connection_status_connected)){
            val message = MqttMessage()
            message.payload = payload
            message.qos = 2
            message.isRetained = false
            mqttClient.publish(topic, message)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        mqttClient = when(key){
            "server_address" -> {
                serverID = prefs.getString("server_address", "0.0.0.0")
                MqttAndroidClient(context, serverID, clientID)
            }
            "port" -> {
                serverPort = prefs.getInt("port", 1883)
                MqttAndroidClient(context, serverID, clientID)
            }
            "username" -> {
                clientID = prefs.getString("client_id", "")
                MqttAndroidClient(context, serverID, clientID)
            }
            "password" -> {
                password = prefs.getString("password", "")
                MqttAndroidClient(context, serverID, clientID)
            }
            "topic_name" -> {
                topic = prefs.getString("topic_name", "")
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
        mqttClient.connect(context, object: IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                connectionStatus.postValue(context.getString(R.string.connection_status_connected))
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                connectionStatus.postValue(context.getString(R.string.connection_status_failed))
            }
        })
    }
}