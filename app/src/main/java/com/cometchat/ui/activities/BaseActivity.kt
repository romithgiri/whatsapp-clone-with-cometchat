package com.cometchat.ui.activities

import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.R
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.CustomMessage
import com.cometchat.pro.models.MediaMessage
import com.cometchat.pro.models.TextMessage
import com.cometchat.utils.LogUtils
import com.google.gson.Gson
import org.json.JSONObject

open class BaseActivity: AppCompatActivity() {
    private val TAG = "BaseActivity"
    private val realTimeMessageArrival = "realTimeMessageArrival"
    private val receiveIncomingCallListenerIDLandingActivity = "receiveIncomingCallListenerIDLandingActivity"

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        realTimeMessageArrivalListener()
    }

    private fun realTimeMessageArrivalListener(){
        CometChat.addMessageListener(realTimeMessageArrival, object : CometChat.MessageListener(){
            override fun onTextMessageReceived(message: TextMessage?) {
                val jsonObject = JSONObject(Gson().toJson(message!!.receiver))
                val isTextExist = jsonObject.has("receiverId")
                if(isTextExist){
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                    LogUtils.info(TAG, "markAsDelivered======>>>> : 1")
                    markAsDelivered(
                        message.id,
                        jsonObject.get("receiverId").toString(),
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Media message received successfully: ${Gson().toJson(message)}")
                val jsonObject = JSONObject(Gson().toJson(message!!.receiver))
                val isTextExist = jsonObject.has("receiverId")
                if(isTextExist){
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                    markAsDelivered(
                        message.id,
                        jsonObject.get("receiverId").toString(),
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onCustomMessageReceived(message: CustomMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Custom message received successfully: ${Gson().toJson(message)}")
                val jsonObject = JSONObject(Gson().toJson(message!!.receiver))
                val isTextExist = jsonObject.has("receiverId")
                if(isTextExist){
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                    markAsDelivered(
                        message.id,
                        jsonObject.get("receiverId").toString(),
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }
        })
    }

    private fun markAsDelivered(
        msgId: Int,
        receiverId: String,
        receiverTypeUser: String,
        senderUID: String
    ) {
        LogUtils.info(TAG, "markAsDelivered======>>>> : 2: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")

        CometChat.markAsDelivered(msgId, receiverId, receiverTypeUser, senderUID,
            object : CometChat.CallbackListener<Void?>() {
                override fun onSuccess(unused: Void?) {
                    LogUtils.info(TAG, "markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")

                    LogUtils.info(TAG, "markAsDelivered : " + "Success")
                }

                override fun onError(e: CometChatException) {
                    LogUtils.info(TAG, "markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")

                    LogUtils.error(TAG, "markAsDelivered : " + e.message)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeCallListener(receiveIncomingCallListenerIDLandingActivity);
    }
}