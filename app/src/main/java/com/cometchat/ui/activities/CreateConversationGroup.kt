package com.cometchat.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.adapter.SelectedUsersForGroupConversationAdapter
import com.cometchat.databinding.ActivityCreateConversationGroupBinding
import com.cometchat.dto.SelectedUsersForGroupConversationModel
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.UsersRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson


class CreateConversationGroup : AppCompatActivity() {
    private val TAG = "CreateConversationGroup"
    private lateinit var binding: ActivityCreateConversationGroupBinding
    private lateinit var loader: AlertDialog
    private val realTimeMessageArrivalID = "realTimeMessageArrivalCreateConversationGroup"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerCreateConversationGroup"

    companion object{
        var selectionTrackerTempList: MutableList<SelectedUsersForGroupConversationModel> = arrayListOf()
    }

    private var mModelList: MutableList<SelectedUsersForGroupConversationModel> = arrayListOf()
    private var mAdapter: RecyclerView.Adapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateConversationGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Utils.showLoader(this)

        selectionTrackerTempList.clear()
        getUserListIntoRecyclerView()

        binding.materialToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnGrpNext.setOnClickListener {
            if(binding.etGroupName.text.isNullOrBlank()){
                Utils.showToast(this, "Please enter required group info")
            }else{
                binding.viewGroupInitInfo.visibility = View.GONE
                binding.recyclerviewCreateConversationGroup.visibility = View.VISIBLE
            }
        }

        binding.etGroupDescription.visibility = View.GONE

        binding.materialToolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.now_create_group -> {
                    if(binding.etGroupName.text.isNullOrBlank()){
                        Utils.showToast(this, "Please enter required group info")
                    } else if(selectionTrackerTempList.isNullOrEmpty()){
                        Utils.showToast(this, "Please select at least 1 group member")
                    }else{
                        initGroupCreatingTask()
                    }
                    //LogUtils.info(TAG, "==========>> selectionTrackerTempList: ${Gson().toJson(selectionTrackerTempList)}")
                }
            }
            true
        }

        receiveIncomingCallListener()
        realTimeMessageArrivalListener()
    }

    private fun getUserListIntoRecyclerView(){
        binding.recyclerviewCreateConversationGroup.layoutManager = LinearLayoutManager(this@CreateConversationGroup)
        loader.show()

        val loggedInUserInfo = CometChat.getLoggedInUser()

        LogUtils.info(TAG, "===>>>> loggedInUserInfo: ${Gson().toJson(loggedInUserInfo)}")

        val usersRequest = UsersRequest.UsersRequestBuilder()
            .setLimit(50)
            .build()

        LogUtils.info(TAG, "===>>>> usersRequest: ${Gson().toJson(usersRequest)}")

        usersRequest?.fetchNext(object : CometChat.CallbackListener<List<User>>(){
            override fun onSuccess(p0: List<User>?) {
                loader.dismiss()
                if(p0.isNullOrEmpty()){
                    binding.recyclerviewCreateConversationGroup.visibility = View.GONE
                    binding.viewGroupInitInfo.visibility = View.GONE
                    binding.noDataFound.visibility = View.VISIBLE
                }else{
                    LogUtils.info(TAG, "User list received: ${p0.size} --- data: ${Gson().toJson(p0)}")
                    p0.forEach {
                        LogUtils.info(TAG, "User list received: ${p0.size} --- data: 2: ${Gson().toJson(it)}")
                        mModelList.add(SelectedUsersForGroupConversationModel(it.uid, it.name, it.avatar))
                    }

                    mAdapter = SelectedUsersForGroupConversationAdapter(this@CreateConversationGroup, mModelList)
                    val manager = LinearLayoutManager(this@CreateConversationGroup)
                    binding.recyclerviewCreateConversationGroup.setHasFixedSize(true)
                    binding.recyclerviewCreateConversationGroup.layoutManager = manager
                    binding.recyclerviewCreateConversationGroup.adapter = mAdapter
                }
            }

            override fun onError(p0: CometChatException?) {
                loader.dismiss()
                LogUtils.error(TAG, "User list fetching failed with exception: " + p0?.message)
            }
        })
    }

    private fun initGroupCreatingTask(){
        loader.show()
        val guid = System.currentTimeMillis().toString()
        val groupName = binding.etGroupName.text.toString()
        val groupDescription = binding.etGroupDescription.text.toString()
        val groupType = CometChatConstants.GROUP_TYPE_PUBLIC
        val groupPassword = "123456"
        val groupIcon = "https://reciperunner.com/wp-content/uploads/2020/02/Lemon-Herb-Chicken-Potato-Skillet-Photo-scaled.jpg"

        val group = Group(guid, groupName, groupType, null)

        CometChat.createGroup(group,object :CometChat.CallbackListener<Group>(){
            override fun onSuccess(p0: Group?) {
                LogUtils.info(TAG, "Group created successfully: " + p0?.toString())
                addTheSelectedGroupMembers(guid)
            }
            override fun onError(p0: CometChatException?) {
                loader.dismiss()
                LogUtils.error(TAG, "Group creation failed with exception: " + p0?.message)
                Utils.showToast(this@CreateConversationGroup, "Group creation failed. ${p0?.details}")
            }
        })
    }

    private fun addTheSelectedGroupMembers(guid: String){
        val member:MutableList<GroupMember> = arrayListOf()
        selectionTrackerTempList.forEach{
            member.add(GroupMember(it.uid, CometChatConstants.SCOPE_PARTICIPANT))
        }
        CometChat.addMembersToGroup(guid, member, null, object :CometChat.CallbackListener<HashMap<String,String>>(){
            override fun onSuccess(p0: HashMap<String, String>?) {
                loader.dismiss()
                LogUtils.info(TAG, "Group created successfully")
                Utils.showToast(this@CreateConversationGroup, "Group created successfully")
                finish()
            }
            override fun onError(p0: CometChatException?) {
                loader.dismiss()
                LogUtils.error(TAG, "Group creation failed with exception: " + p0?.message)
                Utils.showToast(this@CreateConversationGroup, "Group creation failed")
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
                val intent = Intent(this@CreateConversationGroup, CallingActivity::class.java)
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