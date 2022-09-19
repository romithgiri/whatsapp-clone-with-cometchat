package com.cometchat.ui.activities

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cometchat.R
import com.cometchat.databinding.ActivityReceiverProfileViewBinding
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.utils.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso

class ReceiverProfileViewActivity : AppCompatActivity() {
    private val TAG = ""
    private lateinit var binding: ActivityReceiverProfileViewBinding
    private lateinit var loader: AlertDialog

    private val realTimeMessageArrivalID = "realTimeMessageArrivalReceiverProfileViewActivity"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerReceiverProfileViewActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverProfileViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Utils.showLoader(this)

        loader.show()

        val uid = intent.getStringExtra("uid")!!
        val avatar = intent.getStringExtra("avatar")
        val conversationType = intent.getStringExtra("conversationType")

        binding.viewProfileToolbar.setNavigationOnClickListener {
            finish()
        }

        /*binding.viewProfileToolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menu_save_dp -> {
                    if(PermissionHelper.checkStoragePermissions(this)){

                    }else{
                        PermissionHelper.requestPermissionForStorage(this, PermissionHelper.storagePermissionCodeForSaveFile)
                    }
                }
            }
            true
        }*/

        if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
            CometChat.getGroup(uid, object :CometChat.CallbackListener<Group>(){
                override fun onSuccess(p0: Group?) {
                    LogUtils.info(TAG, "User details fetched for user: " + Gson().toJson(p0))
                    loader.dismiss()
                    if(p0?.icon.isNullOrEmpty()) { //url.isEmpty()
                        Picasso.get()
                            .load(R.drawable.ic_person)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profilePhotoIv);

                    }else{
                        Picasso.get()
                            .load(p0?.icon)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profilePhotoIv); //this is your ImageView
                    }

                    binding.viewProfileToolbar.title = p0?.name
                    binding.tvId.text = p0?.guid
                    binding.tvStatus.text = p0?.groupType
                    binding.countGrp.text = "Members Count"
                    binding.tvLastActive.text = p0?.membersCount.toString()
                }

                override fun onError(p0: CometChatException?) {
                    loader.dismiss()
                    LogUtils.error(TAG, "User details fetching failed with exception: " + p0?.message)

                }
            })
        }else{
            CometChat.getUser(uid, object :CometChat.CallbackListener<User>(){
                override fun onSuccess(p0: User?) {
                    LogUtils.info(TAG, "User details fetched for user: " + Gson().toJson(p0))
                    loader.dismiss()
                    if(avatar.isNullOrEmpty()) { //url.isEmpty()
                        Picasso.get()
                            .load(R.drawable.ic_person)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profilePhotoIv);

                    }else{
                        Picasso.get()
                            .load(avatar)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profilePhotoIv); //this is your ImageView
                    }
                    binding.viewProfileToolbar.title = p0?.name

                    binding.tvId.text = p0?.uid
                    binding.tvStatus.text = p0?.status
                    if(p0?.lastActiveAt == null){
                        binding.tvLastActive.text = "Null"
                    }else{
                        binding.tvLastActive.text = DateUtils.getFormattedTimeChatLog(p0.lastActiveAt)
                    }
                }

                override fun onError(p0: CometChatException?) {
                    loader.dismiss()
                    LogUtils.error(TAG, "User details fetching failed with exception: " + p0?.message)

                }
            })
        }

        receiveIncomingCallListener()
        realTimeMessageArrivalListener()
    }

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.storagePermissionCodeForSaveFile -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    LogUtils.info(TAG, "Storage permission not granted")
                    if (requestCode == PermissionHelper.storagePermissionCodeForFilePicker) {

                    } else {

                    }
                } else {
                    LogUtils.info(TAG, "Storage permission granted")
                }
            }
        }
    }*/

    private fun receiveIncomingCallListener(){
        CometChat.addCallListener(receiveIncomingCallListenerID, object :CometChat.CallListener(){
            override fun onOutgoingCallAccepted(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call accepted: " + p0?.toString())

            }
            override fun onIncomingCallReceived(p0: Call?) {
                LogUtils.info(TAG, "Incoming call: ${Gson().toJson(p0)}")
                val intent = Intent(this@ReceiverProfileViewActivity, CallingActivity::class.java)
                intent.putExtra("callType", p0?.type)
                intent.putExtra("operationType", AppConstant.OperationType.IncomingCall)
                intent.putExtra("callObjectData", Gson().toJson(p0))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            override fun onIncomingCallCancelled(p0: Call?) {
                LogUtils.info(TAG, "Incoming call cancelled: " + p0?.toString())
            }

            override fun onOutgoingCallRejected(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call rejected: " + p0?.toString())
            }
        })
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
        CometChat.removeMessageListener(realTimeMessageArrivalID);
        CometChat.removeCallListener(receiveIncomingCallListenerID);
    }

}