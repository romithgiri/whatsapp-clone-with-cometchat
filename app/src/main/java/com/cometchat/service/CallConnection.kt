package com.cometchat.service

import android.content.Intent
import android.os.Build
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.ui.activities.CallingActivity
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils


@RequiresApi(api = Build.VERSION_CODES.M)
class CallConnection(var service: MyConnectionService, call: Call) : Connection() {
    var TAG = "CallConnection"
    var call: Call = call

    override fun onCallAudioStateChanged(state: CallAudioState) {
        LogUtils.info(TAG, "onCallAudioStateChanged$state")
    }

    override fun onDisconnect() {
        LogUtils.info(TAG, "onDisconnect")
        super.onDisconnect()
        destroyConnection()
        LogUtils.info(TAG, "onDisconnect")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL, "Missed"))
        if (CometChat.getActiveCall() != null) onDisconnect(CometChat.getActiveCall())
    }

    private fun onDisconnect(call: Call) {
        LogUtils.info(TAG, "onDisconnect Call: \$call")
        CometChat.rejectCall(call.sessionId, CometChatConstants.CALL_STATUS_CANCELLED,
            object : CallbackListener<Call?>() {
                override fun onSuccess(call: Call?) {
                    LogUtils.info(TAG, "onSuccess: reject")
                }

                override fun onError(e: CometChatException) {
                    Toast.makeText(
                        service, "Unable to end call due to \${p0?.code}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    fun destroyConnection() {
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, "Rejected"))
        LogUtils.info(TAG, "destroyConnection")
        super.destroy()
    }

    override fun onAnswer(videoState: Int) {
        LogUtils.info(TAG, "onAnswerVideo: ")
        if (call.sessionId != null) {
            CometChat.acceptCall(call.sessionId, object : CallbackListener<Call?>() {
                override fun onSuccess(call: Call?) {
                    LogUtils.info(TAG, "onSuccess: accept")
                    service.sendBroadcast(getCallIntent("StartCall"))
                    destroyConnection()
                }

                override fun onError(e: CometChatException) {
                    destroyConnection()
                    Toast.makeText(service, "Error " + e.message, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onShowIncomingCallUi() {
        LogUtils.info(TAG, "onShowIncomingCallUi: ")
    }

    override fun onAnswer() {
        LogUtils.info(TAG, "onAnswer" + call.sessionId)
        if (call.sessionId != null) {
            CometChat.acceptCall(call.sessionId, object : CallbackListener<Call?>() {
                override fun onSuccess(call: Call?) {
                    LogUtils.info(TAG, "onSuccess: accept")
                    service.sendBroadcast(getCallIntent("StartCall"))
                    destroyConnection()
                }

                override fun onError(e: CometChatException) {
                    destroyConnection()
                    e.printStackTrace()
                }
            })
        }
    }

    override fun onHold() {
        LogUtils.info(TAG, "onHold")
    }

    override fun onUnhold() {
        LogUtils.info(TAG, "onUnhold")
    }

    override fun onReject() {
        LogUtils.info(TAG, "onReject" + call.sessionId)
        if (call.sessionId != null) {
            CometChat.rejectCall(
                call.sessionId,
                CometChatConstants.CALL_STATUS_REJECTED,
                object : CallbackListener<Call?>() {
                    override fun onSuccess(call: Call?) {
                        LogUtils.error(TAG, "onSuccess: reject")
                        destroyConnection()
                        setDisconnected(DisconnectCause(DisconnectCause.REJECTED, "Rejected"))
                    }

                    override fun onError(e: CometChatException) {
                        destroyConnection()
                        LogUtils.error(TAG, "onErrorReject: " + e.message)
                    }
                })
        }
    }

    fun onOutgoingReject() {
        LogUtils.info(TAG, "onDisconnect")
        destroyConnection()
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, "REJECTED"))
    }

    private fun getCallIntent(title: String): Intent {
        val callIntent = Intent(service, CallingActivity::class.java)
        callIntent.putExtra(AppConstant.IntentStrings.SESSION_ID, call.sessionId)
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        callIntent.action = title
        return callIntent
    }

    init {
        LogUtils.info(TAG, "CallConnection: $call")
    }
}