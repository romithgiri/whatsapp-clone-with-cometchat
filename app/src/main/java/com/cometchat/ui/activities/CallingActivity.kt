package com.cometchat.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.R
import com.cometchat.databinding.ActivityCallingBinding
import com.cometchat.dto.CallInitiator
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CallSettings
import com.cometchat.pro.core.CallSettings.CallSettingsBuilder
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.core.CometChat.OngoingCallListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson


class CallingActivity : AppCompatActivity() {
    private val TAG = "CallingActivity"
    private lateinit var binding: ActivityCallingBinding
    private lateinit var sessionId: String
    private var callObjectData: Call? = null
    private var callStatusListenerID = "callStatusListenerID"
    private val realTimeMessageArrivalID = "realTimeMessageArrivalCallingActivity"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerCallingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val receiverID = intent.getStringExtra("receiverID")
        val receiverType = intent.getStringExtra("conversationType")
        val receiverName = intent.getStringExtra("name")
        val callType = intent.getStringExtra("callType")!!
        val operationType = intent.getStringExtra("operationType")!!

        if (operationType == AppConstant.OperationType.IncomingCall){
            val obj = intent.getStringExtra("callObjectData")
            if (obj == null) {
                Utils.showToast(this, "Unable to receive incoming call")
                finish()
            }else{
                //to monitor call state
                receiveIncomingCallListener()
                callObjectData = Gson().fromJson(obj, Call::class.java)
                sessionId = callObjectData!!.sessionId
                binding.viewIncomingCallSdk.visibility = View.VISIBLE
                binding.viewOutgoingCallSdkNestedScrollView.visibility = View.GONE
                binding.viewOutgoingCallSdk.visibility = View.GONE
                val tempObj = Gson().fromJson(Gson().toJson(callObjectData!!.callInitiator), CallInitiator::class.java )
                println("tempObj.name ============<<<< ${tempObj.name}")
                binding.tvCallerName.text = tempObj.name
            }
        }else{
            binding.viewIncomingCallSdk.visibility = View.VISIBLE
            binding.btnCancelOutgoingCall.visibility = View.VISIBLE
            binding.btnAcceptIncomingCall.visibility = View.GONE
            binding.btnDeclineIncomingCall.visibility = View.GONE
            binding.tvCallerName.text = "Calling to $receiverName"
            val call = Call(receiverID!!, receiverType, callType)
            makeACall(call, this, binding.viewOutgoingCallSdk)
        }

        binding.materialToolbar.title = if(callType == CometChatConstants.CALL_TYPE_VIDEO){
            binding.ivCallTypeIcon.setImageResource(R.drawable.ic_video_call)
            "Video Calling"
        }else{
            binding.ivCallTypeIcon.setImageResource(R.drawable.ic_voice_call)
            "Voice Calling"
        }

        binding.materialToolbar.setNavigationOnClickListener {
            disconnectCall()
            finish()
        }

        binding.btnAcceptIncomingCall.setOnClickListener {
            binding.viewIncomingCallSdk.visibility = View.GONE
            binding.viewOutgoingCallSdkNestedScrollView.visibility = View.VISIBLE
            binding.viewOutgoingCallSdk.visibility = View.VISIBLE
            acceptCall()
        }

        binding.btnDeclineIncomingCall.setOnClickListener {
            rejectIncomingCallOrCancelOutgoingCall(CometChatConstants.CALL_STATUS_REJECTED)
        }

        binding.btnCancelOutgoingCall.setOnClickListener {
            rejectIncomingCallOrCancelOutgoingCall(CometChatConstants.CALL_STATUS_CANCELLED)
        }

        binding.btnCloseCallingActivity.setOnClickListener {
            finish()
        }
        //val sessionID = Call(receiverID, receiverType, type).sessionId

        realTimeMessageArrivalListener()
    }

    private fun makeACall(call: Call, activity: Activity, relativeLayout: RelativeLayout){
        CometChat.initiateCall(call, object :CometChat.CallbackListener<Call>(){
            override fun onSuccess(p0: Call?) {
                LogUtils.info(TAG, "Voice call=====>>> Call initiated successfully:: receiverID: ${call.receiverUid} --- receiverType: ${call.receiverType} sessionID: ${p0?.sessionId}")
                sessionId = p0?.sessionId!!
                callStatus(sessionId, activity, relativeLayout)
            }

            override fun onError(p0: CometChatException?) {
                LogUtils.error(TAG, "Call initialization failed with exception: " + p0?.message)
            }

        })
    }

    private fun acceptCall(){
        val sessionID: String = sessionId
        val callView: RelativeLayout = binding.viewOutgoingCallSdk
        val activity: Activity = this

        CometChat.acceptCall(sessionID, object :CometChat.CallbackListener<Call>(){
            override fun onSuccess(p0: Call?) {
                LogUtils.info(TAG, "Call accepted successfully: " + p0?.toString())
                startCall(sessionID, activity, callView)
            }

            override fun onError(p0: CometChatException?) {
                LogUtils.error(TAG, "Call acceptance failed with exception: " + p0?.message)
            }
        })
    }

    private fun callStatus(sessionID: String, activity: Activity, relativeLayout: RelativeLayout) {
        CometChat.addCallListener(callStatusListenerID, object :CometChat.CallListener(){
            override fun onOutgoingCallAccepted(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call accepted: " + p0?.toString())
                Utils.showToast(this@CallingActivity, "Call connecting")
                changeUIForOutGoingCall()
                startCall(sessionId, activity, relativeLayout)
            }

            override fun onIncomingCallReceived(p0: Call?) {
                LogUtils.info(TAG, "Incoming call: " + p0?.toString())
            }

            override fun onIncomingCallCancelled(p0: Call?) {
                LogUtils.info(TAG, "Incoming call cancelled: " + p0?.toString())
                Utils.showToast(this@CallingActivity, "Call cancelled")
                /*startActivity(Intent(this@CallingActivity, LandingActivity::class.java))
                finishAffinity()*/
            }

            override fun onOutgoingCallRejected(p0: Call?) {
                Utils.showToast(this@CallingActivity, "Call rejected")
                LogUtils.info(TAG, "Outgoing call rejected: " + p0?.toString())
                finish()
            }

        })
    }

    private fun startCall(sessionID: String, activity: Activity, relativeLayout: RelativeLayout){
        val callSettings: CallSettings = CallSettingsBuilder(this@CallingActivity, relativeLayout)
            .setSessionId(sessionId)
            .showAudioModeButton(true)
            .showEndCallButton(true)
            .showMuteAudioButton(true)
            .startWithVideoMuted(true)
            .startWithAudioMuted(true)
            .build()

        CometChat.startCall(callSettings, object:  CometChat.OngoingCallListener {
            override fun onUserJoined(user: User?) {
                LogUtils.info(TAG, "onUserJoined: Name "+user!!.getName());
            }

            override fun onUserLeft(user: User?) {
                LogUtils.info(TAG, "onUserLeft: "+user!!.getName());
            }

            override fun onError(e: CometChatException?) {
                LogUtils.info(TAG, "onError: "+ e!!.message);
            }

            override fun onCallEnded(call: Call?) {
                LogUtils.info(TAG, "onCallEnded: "+ call.toString())
                binding.btnCloseCallingActivity.visibility = View.VISIBLE
                binding.btnDeclineIncomingCall.visibility = View.GONE
                binding.btnAcceptIncomingCall.visibility = View.GONE
                binding.btnCancelOutgoingCall.visibility = View.GONE
                binding.tvCallerName.text = "Call completed"
                finish()
            }

            override fun onUserListUpdated(p0: MutableList<User>?) {
                LogUtils.info(TAG, "onUserListUpdated: "+ p0.toString());
            }

            override fun onAudioModesUpdated(p0: MutableList<AudioMode>?) {
                LogUtils.info(TAG, "onAudioModesUpdated: "+ p0.toString());
            }

            override fun onRecordingStarted(p0: User?) {
                LogUtils.info(TAG, "onRecordingStarted: "+ p0.toString());
            }

            override fun onRecordingStopped(p0: User?) {
                LogUtils.info(TAG, "onRecordingStopped: "+ p0.toString());
            }

            override fun onUserMuted(p0: User?, p1: User?) {
                LogUtils.info(TAG, "onUserMuted: "+ p0.toString());
            }

            override fun onCallSwitchedToVideo(p0: String?, p1: User?, p2: User?) {
                LogUtils.info(TAG, "onCallSwitchedToVideo: "+ p0.toString());
            }
        })
    }

    private fun rejectIncomingCallOrCancelOutgoingCall(action: String){
        val sessionID:String = sessionId
        val status:String = action

        CometChat.rejectCall(sessionID,status,object:CometChat.CallbackListener<Call>(){
            override fun onSuccess(p0: Call?) {
                LogUtils.info(TAG, "Call rejected successfully with status: " + p0?.callStatus)
                Utils.showToast(this@CallingActivity, "Call $action")
                finish()
            }

            override fun onError(p0: CometChatException?) {
                LogUtils.error(TAG, "Call rejection failed with exception: " + p0?.message)
            }
        })
    }

    private fun receiveIncomingCallListener(){
        CometChat.addCallListener(receiveIncomingCallListenerID, object :CometChat.CallListener(){
            override fun onOutgoingCallAccepted(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call accepted: " + p0?.toString())
            }
            override fun onIncomingCallReceived(p0: Call?) {
                LogUtils.info(TAG, "Incoming call: ${Gson().toJson(p0)}")
            }

            override fun onIncomingCallCancelled(p0: Call?) {
                LogUtils.info(TAG, "Incoming call cancelled: " + p0?.toString())
                CometChat.removeCallListener(receiveIncomingCallListenerID)
                finish()
            }

            override fun onOutgoingCallRejected(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call rejected: " + p0?.toString())
            }

        })
    }

    private fun disconnectCall(){
        LogUtils.info(TAG, "=========>>> disconnectCall method called")
        CometChat.endCall(sessionId, object : CallbackListener<Call?>() {
            override fun onSuccess(call: Call?) {
                // handle end call success
                Utils.showToast(this@CallingActivity, "Call disconnect successfully")
            }
            override fun onError(e: CometChatException) {
                // handled end call error
                LogUtils.error(TAG, "Error: =====>>> ${e.message}")
                Utils.showToast(this@CallingActivity, "Unable to disconnect call")
            }
        })
    }

    private fun changeUIForOutGoingCall(){
        binding.viewIncomingCallSdk.visibility = View.GONE
        binding.viewOutgoingCallSdkNestedScrollView.visibility = View.VISIBLE
        binding.viewOutgoingCallSdk.visibility = View.VISIBLE
    }

    private fun realTimeMessageArrivalListener(){
        CometChat.addMessageListener(realTimeMessageArrivalID, object :CometChat.MessageListener(){
            override fun onTextMessageReceived(message: TextMessage?) {
                /*val jsonObject = JSONObject(Gson().toJson(message!!.receiver))
                val isTextExist = jsonObject.has("receiverId")
                jsonObject.get("receiverId").toString()*/
                if(message!!.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                    markAsDelivered(
                        message.id,
                        message.sender.uid,
                        message.receiverType,
                        CometChat.getLoggedInUser().uid
                    )
                }else{
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                    markAsDelivered(
                        message.id,
                        CometChat.getLoggedInUser().uid,
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Media message received successfully: ${Gson().toJson(message)}")
                if(message!!.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
                    markAsDelivered(
                        message.id,
                        message.sender.uid,
                        message.receiverType,
                        CometChat.getLoggedInUser().uid
                    )
                }else{
                    markAsDelivered(
                        message.id,
                        CometChat.getLoggedInUser().uid,
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onCustomMessageReceived(message: CustomMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Custom message received successfully: ${Gson().toJson(message)}")
                if(message!!.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
                    markAsDelivered(
                        message.id,
                        message.sender.uid,
                        message.receiverType,
                        CometChat.getLoggedInUser().uid
                    )
                }else{
                    markAsDelivered(
                        message.id,
                        CometChat.getLoggedInUser().uid,
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
                }

                override fun onError(e: CometChatException) {
                    LogUtils.error(TAG, "markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeCallListener(callStatusListenerID)
        CometChat.removeMessageListener(realTimeMessageArrivalID)
    }
}