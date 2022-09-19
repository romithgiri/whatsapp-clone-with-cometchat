package com.cometchat.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.adapter.SelectedUsersForUnblockAdapter
import com.cometchat.databinding.ActivityBlockedUsersBinding
import com.cometchat.dto.SelectedUsersForGroupConversationModel
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.BlockedUsersRequest
import com.cometchat.pro.core.BlockedUsersRequest.BlockedUsersRequestBuilder
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.CustomMessage
import com.cometchat.pro.models.MediaMessage
import com.cometchat.pro.models.TextMessage
import com.cometchat.pro.models.User
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson


class BlockedUsers : AppCompatActivity() {
    private val TAG = "BlockedUsers"
    private lateinit var binding: ActivityBlockedUsersBinding

    private lateinit var loader: AlertDialog
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mModelList: MutableList<SelectedUsersForGroupConversationModel> = arrayListOf()

    private val realTimeMessageArrivalID = "realTimeMessageArrivalBlockedUsers"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerBlockedUsers"

    companion object{
        var unblockTempList: MutableList<SelectedUsersForGroupConversationModel> = arrayListOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = Utils.showLoader(this)

        getUserListIntoRecyclerView()

        binding.noDataFound.visibility = View.VISIBLE
        binding.recyclerviewBlockedUsers.visibility = View.GONE

        binding.materialToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.materialToolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menu_unblock -> {
                    if(unblockTempList.isNullOrEmpty()){
                        Utils.showToast(this, "Please select at least one user for unblock")
                    }else{
                        initUnblockTask()
                    }
                }
            }
            true
        }

        receiveIncomingCallListener()
        realTimeMessageArrivalListener()
    }

    private fun getUserListIntoRecyclerView(){
        binding.recyclerviewBlockedUsers.layoutManager = LinearLayoutManager(this@BlockedUsers)
        loader.show()
        val blockedUsersRequest = BlockedUsersRequestBuilder()
            .setLimit(100)
            .setDirection(BlockedUsersRequest.DIRECTION_BLOCKED_BY_ME)
            .build()

        blockedUsersRequest.fetchNext(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                loader.dismiss()
                if (users.isNullOrEmpty()){
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerviewBlockedUsers.visibility = View.GONE
                }else{
                    binding.noDataFound.visibility = View.GONE
                    binding.recyclerviewBlockedUsers.visibility = View.VISIBLE
                    LogUtils.info(TAG, "User list received: ${users.size} --- data: ${Gson().toJson(users)}")
                    users.forEach {
                        LogUtils.info(TAG, "User list received: ${users.size} --- data: 2: ${Gson().toJson(it)}")
                        mModelList.add(SelectedUsersForGroupConversationModel(it.uid, it.name, it.avatar))
                    }
                    mAdapter = SelectedUsersForUnblockAdapter(this@BlockedUsers, mModelList)
                    val manager = LinearLayoutManager(this@BlockedUsers)
                    binding.recyclerviewBlockedUsers.setHasFixedSize(true)
                    binding.recyclerviewBlockedUsers.layoutManager = manager
                    binding.recyclerviewBlockedUsers.adapter = mAdapter
                }
            }

            override fun onError(e: CometChatException) {
                loader.dismiss()
                binding.noDataFound.visibility = View.VISIBLE
                binding.recyclerviewBlockedUsers.visibility = View.GONE
                binding.noDataFound.text = "${e.message}"
                LogUtils.error(TAG, "Error: ====>>>: ${e.message}")
            }
        })
    }

    private fun initUnblockTask(){
        loader.show()
        val uids = ArrayList<String>()
        unblockTempList.forEach {
            uids.add(it.uid)
        }
        CometChat.unblockUsers(uids, object:CometChat.CallbackListener<HashMap<String, String>>() {
            override fun onSuccess(resultMap: HashMap<String, String>) {
                // Handle unblock users success.
                loader.dismiss()
                Utils.showToast(this@BlockedUsers, "User unblocked successfully")
                finish()
            }
            override fun onError(e: CometChatException) {
                loader.dismiss()
                Utils.showToast(this@BlockedUsers, "Fail to unblock user. ${e.message}")
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
                val intent = Intent(this@BlockedUsers, CallingActivity::class.java)
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