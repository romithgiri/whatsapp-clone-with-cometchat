package com.cometchat.ui.fragment.conversation

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.adapter.HadConversationAdapter
import com.cometchat.databinding.FragmentHadConversationBinding
import com.cometchat.dto.ConversationWithDTO
import com.cometchat.dto.HadConversationItemCardDTO
import com.cometchat.dto.RealTimeUpdateForHadConversationItemCardDTO
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.core.ConversationsRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.utils.EventBusOperation
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson
import org.json.JSONObject

class HadConversationFragment:  Fragment() {
    val TAG = "HadConversationFragment"

    private lateinit var loader: AlertDialog
    private lateinit var adapter: HadConversationAdapter
    private lateinit var binding: FragmentHadConversationBinding
    private var data: MutableList<HadConversationItemCardDTO> = arrayListOf()
    private lateinit var dataMutableLiveData: MutableLiveData<MutableList<HadConversationItemCardDTO>>
    private lateinit var conversationRequestBuilder: ConversationsRequest

    private val realTimeMessageArrival = "realTimeMessageArrivalHadConversationFragment"
    private val realTimeOnlineUsersListenerID = "realTimeOnlineUsersListenerHadConversationFragment"

    companion object{
        var realTimeUpdateManagerList: MutableList<RealTimeUpdateForHadConversationItemCardDTO> = arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHadConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //EventBus().register(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*dataMutableLiveData.observe(requireActivity()) {

        }*/
    }

    override fun onResume() {
        super.onResume()
        loader = Utils.showLoader(requireActivity())
        /*loader.show()
        data.clear()
        realTimeUpdateManagerList.clear()*/
        realTimeMessageArrivalListener()
        realTimeOnlineUsersListener()
        LogUtils.info(TAG, "chat history: 1.1 ==========>>>> 1: onResumeCalled")

        adapter = HadConversationAdapter(requireActivity().applicationContext, data) {
            if(it.eventBusOperation == EventBusOperation.DELETE_CONVERSATION){
                val type = if(it.conversationID!!.contains("group", true)){
                    CometChatConstants.RECEIVER_TYPE_GROUP
                }else{
                    CometChatConstants.RECEIVER_TYPE_USER
                }
                CometChat.deleteConversation(
                    it.uid!!,
                    type,
                    object : CallbackListener<String?>() {
                        override fun onSuccess(s: String?) {
                            LogUtils.info(TAG, "Conversation Deleted: $s")
                            Utils.showToast(activity!!, "Conversation deleted")
                            getChatList()
                        }
                        override fun onError(e: CometChatException) {
                            LogUtils.error(TAG, "Error: ${e.message}")
                        }
                    }
                )
            }
        }
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    //Toast.makeText(requireContext(), "Reached to end ==>> going for next 15", Toast.LENGTH_LONG).show()
                    //getChatList()
                    LogUtils.info(TAG, "chat history: 1.1 ==========>>>> 2: dataSize: ${data.size}")
                }
            }
        })
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        binding.recyclerview.adapter = adapter

        conversationRequestBuilder = ConversationsRequest
            .ConversationsRequestBuilder()
            .setLimit(15)
            .build()

        getChatList()
    }

    private fun getChatList(){
        loader.show()
        conversationRequestBuilder = ConversationsRequest
            .ConversationsRequestBuilder()
            .setLimit(50)
            .build()

        LogUtils.info(TAG, "chat history: 1.1 ==========>>>> 3: dataSize: ${data.size}")
        conversationRequestBuilder.fetchNext(object : CometChat.CallbackListener<List<Conversation>>() {
            override fun onSuccess(p0: List<Conversation>?) {
                data.clear()
                realTimeUpdateManagerList.clear()

                loader.dismiss()
                LogUtils.info(TAG, "chat history: 1.1 ==========>>>>  4: $p0")

                if(p0.isNullOrEmpty() && data.isNullOrEmpty()){
                    binding.tvNoConversation.visibility = View.VISIBLE
                    binding.recyclerview.visibility = View.GONE
                }else{
                    binding.tvNoConversation.visibility = View.GONE
                    binding.recyclerview.visibility = View.VISIBLE
                    p0?.forEach {
                        LogUtils.info(TAG, "chat history: 1.1 ==========>>>>  5: ${Gson().toJson(it)}")
                        val conversationWithObject = Gson().fromJson("${Gson().toJson(it.conversationWith)}", ConversationWithDTO::class.java)
                        val uid = if(isChatUserTypeChat(it.conversationType)){
                            conversationWithObject.uid
                        }else{
                            conversationWithObject.guid
                        }
                        val conversationId = it.conversationId
                        val avatar = if(isChatUserTypeChat(it.conversationType)){
                            conversationWithObject.avatar
                        }else{
                            conversationWithObject.icon
                        }
                        val name = conversationWithObject.name
                        val isOnline = if (isChatUserTypeChat(it.conversationType)){
                            conversationWithObject.status == "online"
                        }else{
                            false
                        }

                        val isTyping= false
                        val conversationType = it.conversationType
                        val hasJoined = conversationWithObject.hasJoined ?: false

                        var lastMessageText = ""
                        if(it.lastMessage.type == "text"){
                            val jsonObject = JSONObject(Gson().toJson(it.lastMessage))
                            val isTextExist = jsonObject.has("text")
                            if (isTextExist){
                                lastMessageText = jsonObject.get("text") as String
                                LogUtils.info(TAG, "===============>>>>>> text msg: $lastMessageText")
                            }
                        }else if(it.lastMessage.type == "audio"){
                            lastMessageText = "Audio"
                        }else{
                            LogUtils.info(TAG, "ASD====>>>>> id:$uid --- ${it.lastMessage.type} ")
                            lastMessageText = "Attachment"
                        }

                        val lastMessageSenderId = it.lastMessage.sender.uid
                        val lastMessageType = it.lastMessage.type
                        val lastMessageDeliveredAtTimeStamp= it.lastMessage.deliveredAt
                        val lastMessageDeliveredToMeAtStamp= it.lastMessage.deliveredToMeAt
                        val lastMessageSentAtAtStamp= it.lastMessage.sentAt
                        val lastMessageReadAtStamp= it.lastMessage.readAt
                        val unreadMessageCount = it.unreadMessageCount

                        val obj = HadConversationItemCardDTO(
                            uid,
                            conversationId,
                            avatar,
                            name,
                            isOnline,
                            isTyping,
                            conversationType,
                            hasJoined,
                            lastMessageText,
                            lastMessageSenderId,
                            lastMessageType,
                            lastMessageDeliveredAtTimeStamp,
                            lastMessageDeliveredToMeAtStamp,
                            lastMessageSentAtAtStamp,
                            lastMessageReadAtStamp,
                            unreadMessageCount
                        )
                        data.add(obj)
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onError(p0: CometChatException?) {
                loader.dismiss()
                LogUtils.error(TAG, "chat history: 1.1 ==========>>>> 6: ${p0?.message}")
            }
        })
    }

    private fun realTimeMessageArrivalListener(){
        CometChat.addMessageListener(realTimeMessageArrival, object :CometChat.MessageListener(){
            override fun onTextMessageReceived(message: TextMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Text message received successfully: ${Gson().toJson(message)}")
                //updateRecyclerView(message)
                getChatList()

                //for mark as delivered
                /*val jsonObject = JSONObject(Gson().toJson(message!!.receiver))
                val isTextExist = jsonObject.has("receiver")
                jsonObject.get("receiverId").toString()*/
                if(message != null){
                    markAsDelivered(
                        message.id,
                        message.receiverUid,
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Media message received successfully: ${Gson().toJson(message)}")
                //for mark as delivered
                if(message != null){
                    markAsDelivered(
                        message.id,
                        message.receiverUid,
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onCustomMessageReceived(message: CustomMessage?) {
                LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> Custom message received successfully: ${Gson().toJson(message)}")
                //for mark as delivered
                if(message != null){
                    markAsDelivered(
                        message.id,
                        message.receiverUid,
                        message.receiverType,
                        message.sender.uid
                    )
                }
            }

            override fun onTypingEnded(typingIndicator: TypingIndicator?) {
                LogUtils.info(TAG,"onTyping >>>>> Ended: ${Gson().toJson(typingIndicator)}")
                typingIndicatorHandler(typingIndicator)
            }

            override fun onTypingStarted(typingIndicator: TypingIndicator?) {
                LogUtils.info(TAG,"onTyping >>>>> Started: ${Gson().toJson(typingIndicator)}")
                typingIndicatorHandler(typingIndicator)
            }
        })
    }

    private fun isChatUserTypeChat(type: String): Boolean {
        return type == CometChatConstants.CONVERSATION_TYPE_USER
    }

    private fun typingIndicatorHandler(typingIndicator: TypingIndicator?) {
        LogUtils.info(TAG, "realTimeUpdateManagerList size ====>>>>>>: ${realTimeUpdateManagerList.size} ---- SIZE: ${data.size}")
        if(realTimeUpdateManagerList.size != 0){
            val obj = realTimeUpdateManagerList.find {
                if(typingIndicator?.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP){
                    it.data?.uid == typingIndicator.receiverId
                }else{
                    it.data?.uid == typingIndicator?.sender?.uid
                }
            }
            if (obj != null){
                val index = realTimeUpdateManagerList.indexOf(obj)
                val oldOnlineAndTypingStatus = realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text
                if(typingIndicator?.typingStatus == "started"){
                    realTimeUpdateManagerList[index].holder.groupOnlineAndTyping.visibility = View.VISIBLE
                    realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.visibility = View.VISIBLE
                    realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text = "Typing..."
                    realTimeUpdateManagerList[index].holder.sendMessageText.visibility = View.GONE
                    realTimeUpdateManagerList[index].holder.sendMessageFileIcon.visibility = View.GONE
                    realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.visibility = View.GONE
                }else{
                    LogUtils.info(TAG, "realTimeOnlineUsersListener ==>>> offline: 3: ${obj.data?.name}")
                    val index = realTimeUpdateManagerList.indexOf(obj)
                    if (oldOnlineAndTypingStatus == "Offline"){
                        realTimeUpdateManagerList[index].holder.groupOnlineAndTyping.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageText.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text = "Offline"
                        realTimeUpdateManagerList[index].holder.sendMessageText.text = realTimeUpdateManagerList[index].data?.lastMessageText
                        realTimeUpdateManagerList[index].holder.sendMessageFileIcon.visibility = if(realTimeUpdateManagerList[index].data?.lastMessageType == "text"){
                            View.GONE
                        }else{
                            View.VISIBLE
                        }
                        realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.visibility = if (CometChat.getLoggedInUser().uid == realTimeUpdateManagerList[index].data?.lastMessageSenderId){
                            //for delivery tick mark
                            if (realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp == 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_sent)
                            }else if(realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp != 0.toLong() && realTimeUpdateManagerList[index].data?.lastMessageReadAtStamp == 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_delivered)
                            }else if(realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp != 0.toLong() && realTimeUpdateManagerList[index].data?.lastMessageReadAtStamp != 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_read)
                            }
                            View.VISIBLE
                        }else{
                            //for delivery tick mark
                            View.GONE
                        }
                    }else{
                        realTimeUpdateManagerList[index].holder.groupOnlineAndTyping.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text = "Online"
                        realTimeUpdateManagerList[index].holder.sendMessageText.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageFileIcon.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun markAsDelivered(
        msgId: Int,
        receiverId: String,
        receiverTypeUser: String,
        senderUID: String
    ) {
        CometChat.markAsDelivered(msgId, receiverId, receiverTypeUser, senderUID,
            object : CallbackListener<Void?>() {
                override fun onSuccess(unused: Void?) {
                    LogUtils.info(TAG, "realTimeMessageArrivalListener======>>>> markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
                }
                override fun onError(e: CometChatException) {
                    LogUtils.error(TAG, "markAsDelivered : " + e.message)
                }
            }
        )
    }

    private fun realTimeOnlineUsersListener(){
        CometChat.addUserListener(realTimeOnlineUsersListenerID, object :CometChat.UserListener(){
            override fun onUserOffline(user: User?) {
                LogUtils.info(TAG, "realTimeOnlineUsersListener ==>>> offline: 1: ${user?.name}")
                if(realTimeUpdateManagerList.size != 0){
                    val obj = realTimeUpdateManagerList.find {
                        it.data?.uid.toString() == user?.uid
                    }
                    LogUtils.info(TAG, "realTimeOnlineUsersListener ==>>> offline: 2: ${obj?.data?.name}")
                    if(obj != null){
                        LogUtils.info(TAG, "realTimeOnlineUsersListener ==>>> offline: 3: ${obj.data?.name}")
                        val index = realTimeUpdateManagerList.indexOf(obj)
                        realTimeUpdateManagerList[index].holder.groupOnlineAndTyping.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageText.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text = "Offline"
                        realTimeUpdateManagerList[index].holder.sendMessageText.text = realTimeUpdateManagerList[index].data?.lastMessageText
                        realTimeUpdateManagerList[index].holder.sendMessageFileIcon.visibility = if(realTimeUpdateManagerList[index].data?.lastMessageType == "text"){
                            View.GONE
                        }else{
                            View.VISIBLE
                        }
                        realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.visibility = if (CometChat.getLoggedInUser().uid == realTimeUpdateManagerList[index].data?.lastMessageSenderId){
                            //for delivery tick mark
                            if (realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp == 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_sent)
                            }else if(realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp != 0.toLong() && realTimeUpdateManagerList[index].data?.lastMessageReadAtStamp == 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_delivered)
                            }else if(realTimeUpdateManagerList[index].data?.lastMessageDeliveredAtTimeStamp != 0.toLong() && realTimeUpdateManagerList[index].data?.lastMessageReadAtStamp != 0.toLong()){
                                realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_read)
                            }
                            View.VISIBLE
                        }else{
                            //for delivery tick mark
                            View.GONE
                        }
                    }
                }
            }
            override fun onUserOnline(user: User?) {
                LogUtils.info(TAG, "realTimeOnlineUsersListener ==>>> online: ${user?.name}")
                if(realTimeUpdateManagerList.size != 0){
                    val obj = realTimeUpdateManagerList.find {
                        it.data?.uid.toString() == user?.uid
                    }
                    if(obj != null){
                        val index = realTimeUpdateManagerList.indexOf(obj)
                        realTimeUpdateManagerList[index].holder.groupOnlineAndTyping.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.visibility = View.VISIBLE
                        realTimeUpdateManagerList[index].holder.onlineAndTypingStatus.text = "Online"
                        realTimeUpdateManagerList[index].holder.sendMessageText.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageFileIcon.visibility = View.GONE
                        realTimeUpdateManagerList[index].holder.sendMessageStatusReportIcon.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //EventBus().unregister(activity)
        CometChat.removeUserListener(realTimeOnlineUsersListenerID)
        CometChat.removeMessageListener(realTimeMessageArrival)
    }
}