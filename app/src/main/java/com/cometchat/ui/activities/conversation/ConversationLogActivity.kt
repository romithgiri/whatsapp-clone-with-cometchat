package com.cometchat.ui.activities.conversation

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.adapter.ConversationRowAdapter
import com.cometchat.adapter.GroupMemberMentionedAdapter
import com.cometchat.databinding.ActivityConversationLogBinding
import com.cometchat.dto.*
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.*
import com.cometchat.pro.core.GroupMembersRequest
import com.cometchat.pro.core.MessagesRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.ui.activities.CallingActivity
import com.cometchat.ui.activities.LandingActivity
import com.cometchat.ui.activities.ReceiverProfileViewActivity
import com.cometchat.utils.*
import com.cometchat.utils.DateUtils.getFormattedTimeChatLog
import com.cometchat.utils.PermissionHelper.checkStoragePermissions
import com.cometchat.utils.PermissionHelper.requestPermissionForStorage
import com.cometchat.utils.swipe.MessageSwipeController
import com.cometchat.utils.swipe.ResizeAnim
import com.cometchat.utils.swipe.SwipeControllerActions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*


class ConversationLogActivity : AppCompatActivity(), ConversationRowAdapter.OnReplyClickListener {
    private val TAG = "ConversationLogActivity"

    private lateinit var binding: ActivityConversationLogBinding

    private var lastUserStatusRef: String = ""
    private var typingIndicator: TypingIndicator ?= null

    private var smoothScrollToPosition = 0
    private var notificationType: String ?= null
    private var conversationType = CometChatConstants.RECEIVER_TYPE_USER
    private lateinit var messagesRequest: MessagesRequest

    lateinit var adapter: ConversationRowAdapter

    private lateinit var finalReceivedObj: HadConversationItemCardDTO
    private var lastSentMessageRef: MutableList<EventBusResponseDto> = arrayListOf()
    var goingOnConversationList: Deque<ConversationLogRowDto> = LinkedList()

    private lateinit var groupMemberMentionedAdapter: GroupMemberMentionedAdapter
    private var isSearchingForGroupUserNameStarted = false
    private var typedGroupUserName: MutableList<String> = arrayListOf()

    private val onlineAndOfflineID = "onlineAndOfflineConversationLogActivity"
    private val listenRealTimeReceiptsID = "listenRealTimeReceiptsConversationLogActivity"
    private val realTimeMessageListenerID = "realTimeMessageListenerIDConversationLogActivity"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerConversationLogActivity"
    private val establishAConnectionID = "establishAConnectionConversationLogActivity"

    private var repliedMessagePosition = -1
    private val animationDuration: Long = 300
    private var isRowConversationListWasLessThanTen = false
    private var selectedForReply: RepliedOnDto ?= null
    //private var tempGoingOnConversationList: Deque<ConversationLogRowDto> = LinkedList()

    private lateinit var locationManager: LocationManager
    private var locationGPS: Location? = null
    private lateinit var loader: AlertDialog
    var currentMessageHeight = 0

    companion object{
        lateinit var edittextChatLog: EditText

        var tempGroupMentionedUserList: MutableList<GroupMember> = arrayListOf()
        var groupMentionedUserListForMetaData: MutableList<GroupMentionedUserDto> = arrayListOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isRowConversationListWasLessThanTen = false
        loader = Utils.showLoader(this)
        edittextChatLog = binding.chatLogIncludedLayout.edittextChatLog

        val receivedObj = intent.getStringExtra("obj")
        finalReceivedObj = Gson().fromJson(receivedObj, HadConversationItemCardDTO::class.java)
        conversationType = intent.getStringExtra("conversationType")!!
        notificationType = intent.getStringExtra("notificationType")

        if(conversationType == CometChatConstants.RECEIVER_TYPE_GROUP){
            if(finalReceivedObj.hasJoined){
                binding.chatLogIncludedLayout.viewTextTyping.visibility = View.VISIBLE
                binding.chatLogIncludedLayout.disableGroupConversationView.visibility = View.GONE
            }else{
                binding.chatLogIncludedLayout.viewTextTyping.visibility = View.GONE
                binding.chatLogIncludedLayout.disableGroupConversationView.visibility = View.VISIBLE
            }
        }else {
            binding.chatLogIncludedLayout.viewTextTyping.visibility = View.VISIBLE
            binding.chatLogIncludedLayout.disableGroupConversationView.visibility = View.GONE
        }

        binding.materialToolbar.title = finalReceivedObj.name

        adapter = ConversationRowAdapter(this, goingOnConversationList)
        binding.recyclerviewChatLog.adapter = adapter
        adapter.setReplyClickListener(this)
        binding.chatLogIncludedLayout.myRecyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recyclerviewChatLog.layoutManager = linearLayoutManager
        val messageSwipeController = MessageSwipeController(this, object : SwipeControllerActions {
            override fun showReplyUI(position: Int) {
                repliedMessagePosition = position
                showSelectedMessageForReplyUI(goingOnConversationList.elementAt(position))
            }
        })
        val itemTouchHelper = ItemTouchHelper(messageSwipeController)
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewChatLog)

        binding.recyclerviewChatLog.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    //Toast.makeText(requireContext(), "Reached to end ==>> going for next 15", Toast.LENGTH_LONG).show()
                    //getChatList()
                    fetchMissedMessages(true)
                }
            }
        })

        binding.chatLogIncludedLayout.cancelButton.setOnClickListener {
            hideReplyLayout()
        }

        groupMemberMentionedAdapter = GroupMemberMentionedAdapter(this, tempGroupMentionedUserList)
        binding.chatLogIncludedLayout.myRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatLogIncludedLayout.myRecyclerView.adapter = groupMemberMentionedAdapter
        binding.chatLogIncludedLayout.myRecyclerView.visibility = View.GONE
        adapter.setReplyClickListener(this)

        binding.materialToolbar.setNavigationOnClickListener {
            if (notificationType != null){
                startActivity(Intent(this, LandingActivity::class.java))
                finish()
            }else{
                finish()
            }
        }

        binding.chatLogIncludedLayout.disableGroupConversationView.setOnClickListener {
            if(conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
                val guid = finalReceivedObj.uid
                val groupType = CometChatConstants.GROUP_TYPE_PUBLIC
                CometChat.joinGroup(guid!!, groupType, null, object:CometChat.CallbackListener<Group>(){
                    override fun onSuccess(p0: Group?) {
                        LogUtils.info(TAG, p0.toString())
                        binding.chatLogIncludedLayout.viewTextTyping.visibility = View.VISIBLE
                        binding.chatLogIncludedLayout.disableGroupConversationView.visibility = View.GONE
                    }
                    override fun onError(p0: CometChatException?) {
                        LogUtils.error(TAG, "Group joining failed with exception: " + p0?.message)
                        Utils.showToast(this@ConversationLogActivity, "Fail to join. ${p0?.message}")
                    }
                })
            }
        }

        binding.materialToolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menu_block -> {
                    val uid = finalReceivedObj.uid
                    if(uid.isNullOrEmpty() && conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
                        Utils.showToast(this, "Unable to block user")
                    }else{
                        initBlockTask(uid!!)
                    }
                }
                R.id.menu_profile_info -> {
                    val uid = finalReceivedObj.uid
                    val avatar = finalReceivedObj.avatar
                    val intent = Intent(this, ReceiverProfileViewActivity::class.java)
                    intent.putExtra("uid", uid)
                    intent.putExtra("avatar", avatar)
                    intent.putExtra("conversationType", conversationType)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                R.id.menu_make_voice_call -> {
                    val uid = finalReceivedObj.uid
                    val name = finalReceivedObj.name
                    val intent = Intent(this, CallingActivity::class.java)
                    intent.putExtra("receiverID", uid)
                    intent.putExtra("conversationType", conversationType)
                    intent.putExtra("name", name)
                    intent.putExtra("callType", CometChatConstants.CALL_TYPE_AUDIO)
                    intent.putExtra("operationType", AppConstant.OperationType.OutgoingCall)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                R.id.menu_make_video_call -> {
                    val uid = finalReceivedObj.uid
                    val name = finalReceivedObj.name
                    val intent = Intent(this, CallingActivity::class.java)
                    intent.putExtra("receiverID", uid)
                    intent.putExtra("conversationType", conversationType)
                    intent.putExtra("name", name)
                    intent.putExtra("callType", CometChatConstants.CALL_TYPE_VIDEO)
                    intent.putExtra("operationType", AppConstant.OperationType.OutgoingCall)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
            true
        }

        EventBus.getDefault().register(this);
        initListener()

        binding.chatLogIncludedLayout.sendButtonChatLog.setOnClickListener {
            performSendMessage()
        }

        edittextChatLog.addTextChangedListener(textWatcher)

        binding.chatLogIncludedLayout.ivAttachment.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_attachment_dialog, null)

            val btnGallery = view.findViewById<ImageFilterView>(R.id.send_attachment_gallery)
            val btnCamera = view.findViewById<ImageFilterView>(R.id.send_attachment_camera)
            val btnFile = view.findViewById<ImageFilterView>(R.id.send_attachment_file)
            val btnLocation = view.findViewById<ImageFilterView>(R.id.send_attachment_location)
            val btnCreatePoll = view.findViewById<ImageFilterView>(R.id.create_poll)

            if (finalReceivedObj.conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
                btnCreatePoll.visibility = View.VISIBLE
            }else{
                btnCreatePoll.visibility = View.GONE
            }

            btnGallery.setOnClickListener {
                dialog.dismiss()
                if (checkStoragePermissions(this)) {
                    chooseImageFromGallery()
                } else {
                    requestPermissionForStorage(
                        this,
                        PermissionHelper.storagePermissionCodeForImagePicker
                    )
                    Utils.showToast(this, "Please grant storage permission")
                }
            }

            btnFile.setOnClickListener {
                dialog.dismiss()
                if (checkStoragePermissions(this)) {
                    chooseDocument()
                } else {
                    requestPermissionForStorage(
                        this,
                        PermissionHelper.storagePermissionCodeForFilePicker
                    )
                    Utils.showToast(this, "Please grant storage permission")
                }
            }

            btnCamera.setOnClickListener {
                dialog.dismiss()
                if (PermissionHelper.checkCameraPermission(this)) {
                    captureImage()
                } else {
                    Utils.showToast(this, "Please grant camera permission")
                }
            }

            btnLocation.setOnClickListener {
                dialog.dismiss()
                if (PermissionHelper.isLocationLocationPermissionGranted(this)) {
                    shareCurrentLocation()
                } else {
                    PermissionHelper.requestForLocationPermission(this)
                    Utils.showToast(this, "Please grant camera permission")
                }
            }

            btnCreatePoll.setOnClickListener {
                dialog.dismiss()
                CustomPollDialog(this, finalReceivedObj.uid!!, finalReceivedObj.conversationType).show()
            }

            dialog.setCancelable(true)
            dialog.setContentView(view)
            dialog.show()
        }

        if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
            val guID:String= finalReceivedObj.uid!!
            CometChat.getGroup(guID, object :CometChat.CallbackListener<Group>(){
                override fun onSuccess(p0: Group?) {
                    lastUserStatusRef = "${p0?.membersCount} Members"
                    binding.materialToolbar.subtitle = "${p0?.membersCount} Members"
                    LogUtils.info(TAG, "Group details fetched successfully: ${Gson().toJson(p0)}")
                }
                override fun onError(p0: CometChatException?) {
                    LogUtils.info(TAG, "Group details fetching failed with exception: " +p0?.message)
                }
            })
        }

        binding.recyclerviewChatLog.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(-1)) {
                    //Toast.makeText(this@ConversationLogActivity, "Reached to end ==>> going for next 15", Toast.LENGTH_LONG).show()
                    fetchOldMessages(true)
                }
            }
        })
    }

    private fun initListener(){
        messagesRequest = if(conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
            MessagesRequest.MessagesRequestBuilder()
                .setGUID(finalReceivedObj.uid!!)
                .hideDeletedMessages(true)
                .setLimit(30)
                .build()
        }else{
            MessagesRequest.MessagesRequestBuilder()
                .setUID(finalReceivedObj.uid!!)
                .hideDeletedMessages(true)
                .setLimit(30)
                .build()
        }
        fetchOldMessages(false)
        LogUtils.info(TAG, "adapter.itemCount:::::::: ${adapter.itemCount}")
        binding.recyclerviewChatLog.scrollToPosition(adapter.itemCount - 1)
        listenRealTimeMessage()
        initNotifyTypingIndicator()
        listenTypingIndicator()
        listenUserOfflineOrOnlineState()
        listenRealTimeReceipts()
        receiveIncomingCallListener()
        establishAConnectionListener()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (notificationType != null){
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }else{
            finish()
        }
    }

    private fun fetchOldMessages(isPaginationCall: Boolean) {
        //loader.show()
        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(p0: List<BaseMessage>?) {
                if (!p0.isNullOrEmpty()) {
                    val p0AfterRevers = p0.reversed()
                    for (baseMessage in p0AfterRevers) {
                        arrangeReceivedMessages(baseMessage, AppConstant.FetchMessageType.OLD_MESSAGES)
                    }
                    if(!isPaginationCall){
                        binding.recyclerviewChatLog.scrollToPosition(adapter.itemCount - 1)
                    }
                    //loader.dismiss()
                }else{
                    //loader.dismiss()
                }
            }
            override fun onError(p0: CometChatException?) {
                //loader.dismiss()
                Utils.showToast(this@ConversationLogActivity, "${p0?.message}")
                LogUtils.info(TAG, "Message fetching failed with exception: " + p0?.message)
            }
        })
    }

    private fun performSendMessage() {
        val text = edittextChatLog.text.toString()
        if (text.isEmpty()) {
            Utils.showToast(this, "Message cannot be empty")
            return
        }

        if (binding.chatLogIncludedLayout.replyLayout.visibility == View.VISIBLE) {
            hideReplyLayout()
        }

        val metadata = JSONObject()
        val receiverID = finalReceivedObj.uid
        val messageText = edittextChatLog.text.toString()
        val receiverType = conversationType
        val textMessage = TextMessage(receiverID!!, messageText, receiverType)

        if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
            if (groupMentionedUserListForMetaData.isNotEmpty()){
                val tempGroupMentionedUserListForMetaData: MutableList<GroupMentionedUserDto> = arrayListOf()
                groupMentionedUserListForMetaData.forEach{
                    if(!messageText.contains("@${it.userName}", false)){
                        tempGroupMentionedUserListForMetaData.add(it)
                    }
                }
                tempGroupMentionedUserListForMetaData.forEach {
                    groupMentionedUserListForMetaData.remove(it)
                }
                if (groupMentionedUserListForMetaData.isNotEmpty()){
                    metadata.put("mentionedUser", "${Gson().toJson(groupMentionedUserListForMetaData)}")
                    textMessage.metadata = metadata
                }
            }
        }

        if(selectedForReply != null){
            metadata.put("repliedOn", "${Gson().toJson(selectedForReply)}")
            textMessage.metadata = metadata
        }

        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(textMessage: TextMessage) {
                LogUtils.info(TAG, "Message sent successfully: $textMessage")
                edittextChatLog.text.clear()
                selectedForReply = null
                goingOnConversationList.add(
                    ConversationLogRowDto(
                        this@ConversationLogActivity,
                        textMessage.text,
                        textMessage.id,
                        textMessage.sender.uid,
                        textMessage.sender.name,
                        getLoggedInUser().avatar,
                        textMessage.sentAt,
                        null,
                        textMessage.deliveredAt,
                        textMessage.readAt,
                        AppConstant.TextedBy.SENDER,
                        conversationType,
                        "",
                        -1,
                        textMessage.metadata
                    )
                )
                adapter.notifyItemInserted(goingOnConversationList.size)
                binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
            }

            override fun onError(e: CometChatException) {
                LogUtils.info(TAG, "Message sending failed with exception: " + e.message)
            }
        })
    }

    private fun listenRealTimeMessage() {
        CometChat.addMessageListener(realTimeMessageListenerID, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage?) {
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: listenRealTimeMessage onTextMessageReceived: ${Gson().toJson(message)}")
                if (message != null) {
                    if (CometChat.getLoggedInUser().uid == message.sender?.uid) {
                        goingOnConversationList.add(
                            ConversationLogRowDto(
                                this@ConversationLogActivity,
                                message.text,
                                message.id,
                                message.sender.uid,
                                message.sender.name,
                                getLoggedInUser().avatar,
                                message.sentAt,
                                null,
                                message.deliveredAt,
                                message.readAt,
                                AppConstant.TextedBy.SENDER,
                                conversationType,
                                "",
                                -1,
                                message.metadata
                            )
                        )
                        adapter.notifyItemInserted(goingOnConversationList.size)
                    }else{
                        CometChat.markAsRead(message)
                        goingOnConversationList.add(
                            ConversationLogRowDto(
                                this@ConversationLogActivity,
                                message.text,
                                message.id,
                                message.sender.uid,
                                message.sender.name,
                                getLoggedInUser().avatar,
                                message.sentAt,
                                null,
                                message.deliveredAt,
                                message.readAt,
                                AppConstant.TextedBy.RECEIVER,
                                conversationType,
                                "",
                                -1,
                                message.metadata
                            )
                        )
                        adapter.notifyItemInserted(goingOnConversationList.size)
                    }
                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                }
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: listenRealTimeMessage onMediaMessageReceived: ${Gson().toJson(message)}")
                if (message != null) {
                    CometChat.markAsRead(message)
                    goingOnConversationList.add(
                        ConversationLogRowDto(
                            this@ConversationLogActivity,
                            null,
                            message.id,
                            message.sender.uid,
                            message.sender.name,
                            getLoggedInUser().avatar,
                            message.sentAt,
                            message.attachment,
                            message.deliveredAt,
                            message.readAt,
                            AppConstant.TextedBy.RECEIVER,
                            conversationType,
                            "",
                            -1,
                            message.metadata
                        )
                    )
                    adapter.notifyItemInserted(goingOnConversationList.size)
                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                }
            }

            override fun onCustomMessageReceived(message: CustomMessage?) {
                LogUtils.info(TAG, "Poll ========>>>> onCustomMessageReceived: ${Gson().toJson(message)}")
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: listenRealTimeMessage onCustomMessageReceived: ${Gson().toJson(message)}")
                if (message != null) {
                    if (CometChat.getLoggedInUser().uid == message.sender?.uid) {
                        goingOnConversationList.add(
                            ConversationLogRowDto(
                                this@ConversationLogActivity,
                                null,
                                message.id,
                                message.sender.uid,
                                message.sender.name,
                                getLoggedInUser().avatar,
                                message.sentAt,
                                null,
                                message.deliveredAt,
                                message.readAt,
                                AppConstant.TextedBy.SENDER,
                                conversationType,
                                Gson().toJson(message),
                                -1,
                                message.metadata
                            )
                        )
                    }else{
                        CometChat.markAsRead(message)
                        goingOnConversationList.add(
                            ConversationLogRowDto(
                                this@ConversationLogActivity,
                                null,
                                message.id,
                                message.sender.uid,
                                message.sender.name,
                                getLoggedInUser().avatar,
                                message.sentAt,
                                null,
                                message.deliveredAt,
                                message.readAt,
                                AppConstant.TextedBy.RECEIVER,
                                conversationType,
                                Gson().toJson(message),
                                -1,
                                message.metadata
                            )
                        )
                    }

                    adapter.notifyItemInserted(goingOnConversationList.size)
                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                }
            }

            override fun onMessageEdited(p0: BaseMessage?) {
                super.onMessageEdited(p0)
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: onMessageEdited: 1: ${Gson().toJson(p0)}")
                if(p0 != null){
                    val getTheObj = goingOnConversationList.filter {
                        it.chatID == p0.id
                    }
                    if (!getTheObj.isNullOrEmpty()){
                        val getThePosition = goingOnConversationList.indexOf(
                            getTheObj[0]
                        )
                        getTheObj[0].metadata = p0.metadata
                        adapter.notifyItemChanged(getThePosition)
                    }else{
                        LogUtils.info(TAG, "Edited message is not present in chat history")
                    }
                }
            }
        })
    }

    private fun listenRealTimeReceipts() {
        CometChat.addMessageListener(listenRealTimeReceiptsID, object : MessageListener() {
            override fun onMessagesDelivered(messageReceipt: MessageReceipt) {
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: listenRealTimeReceipts onMessagesDelivered: ${Gson().toJson(messageReceipt)}")
                if (!lastSentMessageRef.isNullOrEmpty()){
                    lastSentMessageRef.forEach {
                        if(it.chatId == messageReceipt.messageId){
                            if (conversationType == CometChatConstants.CONVERSATION_TYPE_USER){
                                (it.view as ImageView).setImageResource(R.drawable.ic_check_delivered)
                            }
                        }
                    }
                }
            }

            override fun onMessagesRead(messageReceipt: MessageReceipt) {
                LogUtils.info(TAG, "real >>>>>>>>>>>>>>: listenRealTimeReceipts onMessagesRead: $messageReceipt")
                LogUtils.info(TAG, "rohit ===>>> 1: $messageReceipt")
                if (!lastSentMessageRef.isNullOrEmpty()){
                    LogUtils.info(TAG, "rohit ===>>> 2: $messageReceipt")
                    lastSentMessageRef.forEach {
                        LogUtils.info(TAG, "rohit ===>>> 3: ${it.chatId} --- ${messageReceipt.messageId}")
                        if(it.chatId == messageReceipt.messageId){
                            LogUtils.info(TAG, "rohit ===>>> 4: ${it.chatId} --- ${messageReceipt.messageId}")
                            if (conversationType == CometChatConstants.CONVERSATION_TYPE_USER){
                                LogUtils.info(TAG, "rohit ===>>> 5: ${it.chatId} --- ${messageReceipt.messageId}")
                                Handler().postDelayed({
                                    (it.view as ImageView).setImageResource(R.drawable.ic_check_read)
                                }, 1000)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun initNotifyTypingIndicator() {
        val uid = finalReceivedObj.uid
        LogUtils.info(TAG, "=======>>> conversationType: ${conversationType} --- uid: $uid")
        typingIndicator = TypingIndicator(uid!!, conversationType)
    }

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (after != 0) {
                if (smoothScrollToPosition == 0) {
                    if (edittextChatLog.hasFocus()) {
                        smoothScrollToPosition++
                        if(adapter.itemCount > 0) {
                            binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
                CometChat.startTyping(typingIndicator!!)
            } else {
                smoothScrollToPosition = 0
                CometChat.endTyping(typingIndicator!!)
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            try {
                if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP) {
                    if(start - before >= 0){
                        if (s[start - before].toString() == "@"){
                            isSearchingForGroupUserNameStarted = true
                        }else if (s[start - before].toString() == " "){
                            isSearchingForGroupUserNameStarted = false
                        }
                        if(isSearchingForGroupUserNameStarted){
                            typedGroupUserName = s.split("@") as MutableList<String>
                            if (typedGroupUserName.isNotEmpty()){
                                val tempTypedGroupUserName = typedGroupUserName[typedGroupUserName.size - 1]
                                val groupMembersRequest= GroupMembersRequest
                                    .GroupMembersRequestBuilder(finalReceivedObj.uid)
                                    .setSearchKeyword(tempTypedGroupUserName.removePrefix("@"))
                                    .build()
                                groupMembersRequest.fetchNext(object :
                                    CallbackListener<List<GroupMember?>>() {
                                    override fun onSuccess(list: List<GroupMember?>) {
                                        if(list.isNotEmpty() && typedGroupUserName.isNotEmpty()){
                                            tempGroupMentionedUserList = list as MutableList<GroupMember>
                                            groupMemberMentionedAdapter = GroupMemberMentionedAdapter(this@ConversationLogActivity, tempGroupMentionedUserList)
                                            binding.chatLogIncludedLayout.myRecyclerView.adapter = groupMemberMentionedAdapter
                                            binding.chatLogIncludedLayout.myRecyclerView.visibility = View.VISIBLE
                                        }else{
                                            binding.chatLogIncludedLayout.myRecyclerView.visibility = View.GONE
                                        }
                                    }
                                    override fun onError(e: CometChatException) {
                                        LogUtils.error(
                                            TAG,
                                            "Group Member list fetching failed with exception: " + e.message
                                        )
                                    }
                                })
                            }
                        }else{
                            typedGroupUserName.clear()
                            binding.chatLogIncludedLayout.myRecyclerView.visibility = View.GONE
                        }
                    }else{
                        typedGroupUserName.clear()
                        binding.chatLogIncludedLayout.myRecyclerView.visibility = View.GONE
                    }
                }
            }catch (e: Exception){
                LogUtils.error(TAG, "Error: onTextChanged =====>>> $e")
            }
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private fun listenTypingIndicator() {
        CometChat.addMessageListener("Typing", object : MessageListener() {
            override fun onTypingStarted(typingIndicator: TypingIndicator) {
                LogUtils.info(TAG, " Typing started ==>>>: ${Gson().toJson(typingIndicator)}")
                if(typingIndicator.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP){
                    if(typingIndicator.receiverId == finalReceivedObj.uid){
                        binding.materialToolbar.subtitle = "${typingIndicator.sender.name} is typing..."
                    }
                }else{
                    if(typingIndicator.receiverId == CometChat.getLoggedInUser().uid){
                        binding.materialToolbar.subtitle = "Typing..."
                    }
                }
            }

            override fun onTypingEnded(typingIndicator: TypingIndicator) {
                LogUtils.info(TAG, " Typing ended ==>>>: ${Gson().toJson(typingIndicator)}")
                if(typingIndicator.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP){
                    if(typingIndicator.receiverId == finalReceivedObj.uid){
                        binding.materialToolbar.subtitle = lastUserStatusRef
                    }
                }else{
                    if(typingIndicator.receiverId == CometChat.getLoggedInUser().uid){
                        binding.materialToolbar.subtitle = lastUserStatusRef
                    }
                }
            }
        })
    }

    private fun listenUserOfflineOrOnlineState() {
        CometChat.addUserListener(onlineAndOfflineID, object : UserListener() {
            override fun onUserOnline(user: User) {
                LogUtils.info(TAG, "====>>>> online status: online: ${Gson().toJson(user)}")
                if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
                    lastUserStatusRef = "Group"
                    binding.materialToolbar.subtitle = "Group"
                }else{
                    if(finalReceivedObj.uid == user.uid){
                        lastUserStatusRef = "Online"
                        binding.materialToolbar.subtitle = "Online"
                    }
                }
            }

            override fun onUserOffline(user: User) {
                LogUtils.info(TAG, "====>>>> online status: offline: ${Gson().toJson(user)}")
                if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
                    lastUserStatusRef = "Group"
                    binding.materialToolbar.subtitle = "Group"
                }else{
                    lastUserStatusRef = "Last seen ${getFormattedTimeChatLog(user.lastActiveAt)}"
                    binding.materialToolbar.subtitle = "Last seen ${getFormattedTimeChatLog(user.lastActiveAt)}"
                }
            }
        })
    }

    private fun sendImage(path: String, messageType: String) {
        val receiverID = finalReceivedObj.uid
        val messageType: String = messageType
        val receiverType: String = conversationType
        val filePath: String = path
        val mediaMessage = MediaMessage(receiverID, File(filePath), messageType, receiverType)
        CometChat.sendMediaMessage(
            mediaMessage,
            object : CometChat.CallbackListener<MediaMessage>() {
                override fun onSuccess(p0: MediaMessage?) {
                    LogUtils.info(TAG, "Media message sent successfully ===>>>: ${Gson().toJson(p0)}")
                    goingOnConversationList.add(
                        ConversationLogRowDto(
                            this@ConversationLogActivity,
                            null,
                            p0?.id!!,
                            p0.sender.uid,
                            p0.sender.name,
                            getLoggedInUser().avatar,
                            p0.sentAt,
                            p0.attachment,
                            p0.deliveredAt,
                            p0.readAt,
                            AppConstant.TextedBy.SENDER,
                            conversationType,
                            "",
                            -1,
                            p0.metadata
                        )
                    )
                    adapter.notifyItemInserted(goingOnConversationList.size)
                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                }

                override fun onError(p0: CometChatException?) {
                    LogUtils.error(TAG, "Message sending failed with exception: " + p0?.message)
                }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.cameraPermissionCode -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    LogUtils.info(TAG, "Requires Access to Camara.")
                    captureImage()
                } else {
                    LogUtils.info(TAG, "Camera permission granted")
                }
            }
            PermissionHelper.storagePermissionCodeForImagePicker -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    LogUtils.info(TAG, "Storage permission not granted")
                    if (requestCode == PermissionHelper.storagePermissionCodeForFilePicker) {
                        chooseDocument()
                    } else {
                        chooseImageFromGallery()
                    }
                } else {
                    LogUtils.info(TAG, "Storage permission granted")
                }
            }
        }
    }

    private fun captureImage() {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePicture, AppConstant.cameraImageCode)
    }

    private fun chooseDocument() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, AppConstant.filePickerCode)
    }

    private fun chooseImageFromGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, AppConstant.selectImageCode)

        /*val optionsMenu = arrayOf<CharSequence>(
            "Take Photo",
            "Choose from Gallery",
            "Exit"
        ) // create a menuOption Array
        // create a dialog for showing the optionsMenu
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // set the items in builder
        builder.setItems(optionsMenu) { dialogInterface, i ->
            when {
                optionsMenu[i] == "Take Photo" -> {
                    // Open the camera and get the photo
                    val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePicture, cameraImageCode)
                }
                optionsMenu[i] == "Choose from Gallery" -> {
                    // choose from  external storage
                    val pickPhoto = Intent(Intent.ACTION_PICK, Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhoto, selectImageCode)
                }
                optionsMenu[i] == "Exit" -> {
                    dialogInterface.dismiss()
                }
            }
        }
        builder.setCancelable(true)
        builder.show()*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            when (requestCode) {
                AppConstant.cameraImageCode -> if (resultCode == RESULT_OK && data != null) {
                    val selectedImage = data.extras!!["data"] as Bitmap?
                    val tempUri: Uri? = getImageUri(this, selectedImage)
                    val picturePath = getRealPathFromURI(tempUri)
                    if (picturePath != null) {
                        sendImage(picturePath, CometChatConstants.MESSAGE_TYPE_IMAGE)
                    }
                    //imageView.setImageBitmap(selectedImage)
                }
                AppConstant.selectImageCode -> if (resultCode == RESULT_OK && data != null) {
                    val selectedImage: Uri? = data.data
                    val filePathColumn = arrayOf(Images.Media.DATA)
                    if (selectedImage != null) {
                        val cursor: Cursor? =
                            contentResolver.query(selectedImage, filePathColumn, null, null, null)
                        if (cursor != null) {
                            cursor.moveToFirst()
                            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
                            val picturePath: String = cursor.getString(columnIndex)
                            sendImage(picturePath, CometChatConstants.MESSAGE_TYPE_IMAGE)
                            //imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath))
                            cursor.close()
                        }
                    }
                }
                AppConstant.filePickerCode -> if (resultCode == RESULT_OK && data != null) {
                    val selectedImage: Uri? = data.data
                    LogUtils.info(TAG, "====>>> File Uri: $selectedImage")
                    val filePath = FilePathProviderUtils.getFilePathFromUri(this, selectedImage!!);
                    if (filePath != null) {
                        val file = File(filePath)
                        LogUtils.info(TAG, "=====>>>> selected file path exist: ${file.exists()}")
                        sendImage(filePath, CometChatConstants.MESSAGE_TYPE_IMAGE)
                    }
                }
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap?): Uri? {
        if (inImage == null) return null
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun getRealPathFromURI(uri: Uri?): String? {
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToFirst()
        val idx = cursor.getColumnIndex(Images.ImageColumns.DATA)
        return cursor.getString(idx)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: EventBusResponseDto) {
        when (event.eventBusOperation) {
            EventBusOperation.FILE_DOWNLOAD -> {
                (event.view as TextView).setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_file,
                    0,
                    0,
                    0
                )
            }
            EventBusOperation.IMAGE_DOWNLOAD -> {
                event.view.visibility = View.GONE
            }
            EventBusOperation.DELETE_MESSAGE -> {
                deleteMessage(event.chatId!!, event.position)
            }
            EventBusOperation.NEW_MESSAGE_SENT -> {
                lastSentMessageRef.add(event)
            }
        }
    }

    private fun deleteMessage(messageId: Int, position: Int) {
        deleteMessage(messageId, object : CallbackListener<BaseMessage>() {
            override fun onSuccess(message: BaseMessage) {
                try {
                    Utils.showToast(this@ConversationLogActivity, "Message Deleted")
                    LogUtils.info(TAG, "Message deleted successfully at : " + message.deletedAt)
                    goingOnConversationList.remove(goingOnConversationList.elementAt(position))
                    adapter.notifyItemRemoved(position)
                }catch (e: Exception){
                    LogUtils.error(TAG, "Error: === onDelete msg: $e")
                }
            }

            override fun onError(e: CometChatException) {
                LogUtils.info(TAG, e.message!!)
            }
        })
    }

    private fun initBlockTask(id: String){
        val uid = ArrayList<String>()
        uid.add(id)
        CometChat.blockUsers(uid, object:CometChat.CallbackListener<HashMap<String, String>>() {
            override fun onSuccess(resultMap: HashMap<String, String>) {
                // Handle unblock users success.
                LogUtils.info(TAG, "User is blocked now")
                Utils.showToast(this@ConversationLogActivity, "User has been blocked")
                binding.chatLogIncludedLayout.viewTextTyping.visibility = View.GONE
                binding.chatLogIncludedLayout.disableGroupConversationView.visibility = View.VISIBLE
                binding.chatLogIncludedLayout.disableGroupConversationText.text = "This user is blocked by you. For conversation please unlock the user."
            }
            override fun onError(e: CometChatException) {
                Utils.showToast(this@ConversationLogActivity, "Unable to block user")
                LogUtils.error(TAG, "Error: unable to block user -> ${e.message}")
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
                val uid = finalReceivedObj.uid
                val intent = Intent(this@ConversationLogActivity, CallingActivity::class.java)
                intent.putExtra("receiverID", uid)
                intent.putExtra("conversationType", conversationType)
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

    private fun shareCurrentLocation(){
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            PermissionHelper.enableGPS(this)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Utils.showToast(this, "Please grant location permission")
            } else {
                val loader = Utils.showLoader(this)
                loader.show()
                locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(locationGPS != null){
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address> = geocoder.getFromLocation(locationGPS!!.latitude, locationGPS!!.longitude, 1)
                    loader.dismiss()
                    val address = addresses[0].getAddressLine(0)
                    LogUtils.info(TAG, "Current address ====>> first:$address ---- ${addresses[0]} ---- ${Gson().toJson(addresses[0])}")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("Share current location")
                    builder.setMessage("Current location:\n${address}").setCancelable(false)
                        .setPositiveButton("Share", DialogInterface.OnClickListener { dialog, which ->
                            val uid = finalReceivedObj.uid
                            val customType = "Location"
                            val customData = JSONObject()
                            customData.put("latitude", locationGPS!!.latitude)
                            customData.put("longitude",locationGPS!!.longitude)
                            val customMessage = CustomMessage(uid, CometChatConstants.RECEIVER_TYPE_USER,customType, customData)
                            CometChat.sendCustomMessage(customMessage, object :CometChat.CallbackListener<CustomMessage>() {
                                override fun onSuccess(customMessage: CustomMessage) {
                                    LogUtils.info(TAG, "Shared location response====>>> ${Gson().toJson(customMessage)}")
                                    dialog.cancel()
                                    goingOnConversationList.add(
                                        ConversationLogRowDto(
                                            this@ConversationLogActivity,
                                            null,
                                            customMessage.id,
                                            customMessage.sender.uid,
                                            customMessage.sender.name,
                                            getLoggedInUser().avatar,
                                            customMessage.sentAt,
                                            null,
                                            customMessage.deliveredAt,
                                            customMessage.readAt,
                                            AppConstant.TextedBy.SENDER,
                                            conversationType,
                                            Gson().toJson(customMessage),
                                            -1,
                                            null
                                        )
                                    )
                                    adapter.notifyItemInserted(goingOnConversationList.size)
                                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                                }
                                override fun onError(e: CometChatException) {
                                    LogUtils.error(TAG, "Error in sharing a location ===>> ${e.message}")
                                    dialog.cancel()
                                }
                            })
                        })
                        .setNegativeButton("Cancle", DialogInterface.OnClickListener { dialog, which ->
                            dialog.cancel()
                        })
                    val alertDialog: AlertDialog = builder.create()
                    alertDialog.show()
                }else{
                    loader.dismiss()
                    Utils.showToast(this, "Unable to get your location")
                }
            }
        }
    }

    private fun showSelectedMessageForReplyUI(message: ConversationLogRowDto) {
        edittextChatLog.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(edittextChatLog, InputMethodManager.SHOW_IMPLICIT)

        val repliedOnText = if(message.text == null && message.attachment != null){
            if (message.attachment.fileExtension.contains("png") || message.attachment.fileExtension.contains("jpg")) {
                "Image"
            }else{
                "File"
            }
        }else{
            message.text
        }
        selectedForReply = RepliedOnDto(
            message.chatID,
            message.name,
            message.uId,
            repliedOnText
        )
        binding.chatLogIncludedLayout.textSelectedMessageForReply.text = message.text

        val height = binding.chatLogIncludedLayout.textSelectedMessageForReply.getActualHeight()
        val startHeight = currentMessageHeight

        if (height != startHeight) {

            if (binding.chatLogIncludedLayout.replyLayout.visibility == View.GONE)
                Handler().postDelayed({
                    binding.chatLogIncludedLayout.replyLayout.visibility = View.VISIBLE
                }, 50)
            val targetHeight = height - startHeight

            val resizeAnim =
                ResizeAnim(
                    binding.chatLogIncludedLayout.replyLayout,
                    startHeight,
                    targetHeight
                )

            resizeAnim.duration = animationDuration.toLong()
            binding.chatLogIncludedLayout.replyLayout.startAnimation(resizeAnim)

            currentMessageHeight = height
        }
    }

    private fun hideReplyLayout() {
        val resizeAnim = ResizeAnim(binding.chatLogIncludedLayout.replyLayout, currentMessageHeight, 0)
        resizeAnim.duration = animationDuration
        Handler().postDelayed({
            binding.chatLogIncludedLayout.replyLayout.layout(0, -binding.chatLogIncludedLayout.replyLayout.height, binding.chatLogIncludedLayout.replyLayout.width, 0)
            binding.chatLogIncludedLayout.replyLayout.requestLayout()
            binding.chatLogIncludedLayout.replyLayout.forceLayout()
            binding.chatLogIncludedLayout.replyLayout.visibility = View.GONE

        }, animationDuration - 50)

        binding.chatLogIncludedLayout.replyLayout.startAnimation(resizeAnim)
        currentMessageHeight = 0

        resizeAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                val params = binding.chatLogIncludedLayout.replyLayout.layoutParams
                params.height = 0
                binding.chatLogIncludedLayout.replyLayout.layoutParams = params
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun TextView.getActualHeight(): Int {
        binding.chatLogIncludedLayout.textSelectedMessageForReply.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return measuredHeight
    }

    override fun onRepliedCardClick(position: Int) {
        binding.recyclerviewChatLog.smoothScrollToPosition(position - 1)
        (binding.recyclerviewChatLog.adapter as ConversationRowAdapter).blinkItem(position)
    }

    private fun establishAConnectionListener(){
        LogUtils.info(TAG, "=====>>>> ::: establishAConnection called")
        CometChat.addConnectionListener(establishAConnectionID, object : ConnectionListener {
            override fun onConnected() {
                LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onConnected")
                fetchMissedMessages(false)
            }

            override fun onConnecting() {
                LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onConnecting")
            }

            override fun onDisconnected() {
                LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onDisconnected")
            }

            override fun onFeatureThrottled() {
                LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onFeatureThrottled")
            }
        })
    }

    fun fetchMissedMessages(isPaginationCall: Boolean){
        val latestId = CometChat.getLastDeliveredMessageId()
        val messagesRequestForMissedMessages = if(conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
            MessagesRequest.MessagesRequestBuilder()
                .setGUID(finalReceivedObj.uid!!)
                .setMessageId(latestId)
                .hideDeletedMessages(true)
                .setLimit(20)
                .build()
        }else{
            MessagesRequest.MessagesRequestBuilder()
                .setUID(finalReceivedObj.uid!!)
                .setMessageId(latestId)
                .hideDeletedMessages(true)
                .setLimit(20)
                .build()
        }
        messagesRequestForMissedMessages.fetchNext(object : CallbackListener<List<BaseMessage?>>() {
            override fun onSuccess(list: List<BaseMessage?>) {
                LogUtils.info(TAG, "messagesRequestForMissedMessages ====>> 1: ${list.size}")
                for (baseMessage in list) {
                    LogUtils.info(TAG, "messagesRequestForMissedMessages ====>> 2: ${Gson().toJson(baseMessage)}")
                    if (baseMessage != null) {
                        arrangeReceivedMessages(baseMessage, AppConstant.FetchMessageType.MISSED_MESSAGES)
                    }
                }
                if(!isPaginationCall){
                    binding.recyclerviewChatLog.scrollToPosition(adapter.itemCount - 1)
                }
            }

            override fun onError(e: CometChatException) {
                LogUtils.error(TAG, "Error: Message fetching failed with exception: ${e.message}")
            }
        })
    }

    fun arrangeReceivedMessages(baseMessage: BaseMessage, fetchMessageType: String) {
        val avatar = if(CometChat.getLoggedInUser().uid == baseMessage.sender.uid){
            getLoggedInUser().avatar
        }else{
            finalReceivedObj.avatar
        }
        val textedBy = if(CometChat.getLoggedInUser().uid == baseMessage.sender.uid){
            AppConstant.TextedBy.SENDER
        }else{
            AppConstant.TextedBy.RECEIVER
        }
        LogUtils.info(TAG, "arrangeReceivedMessages >>> 1.1: ${Gson().toJson(baseMessage)}")
        CometChat.markAsRead(baseMessage)
        if (baseMessage is TextMessage) {
            if (conversationType != CometChatConstants.CONVERSATION_TYPE_GROUP){
                if(baseMessage.sender.status == "online"){
                    lastUserStatusRef = "Online"
                    binding.materialToolbar.subtitle = "Online"
                }else{
                    lastUserStatusRef = "Last seen ${getFormattedTimeChatLog(baseMessage.sender.lastActiveAt)}"
                    binding.materialToolbar.subtitle = "Last seen ${getFormattedTimeChatLog(baseMessage.sender.lastActiveAt)}"
                }
            }
            val supplyObject = ConversationLogRowDto(
                this@ConversationLogActivity,
                baseMessage.text,
                baseMessage.id,
                baseMessage.sender.uid,
                baseMessage.sender.name,
                avatar,
                baseMessage.sentAt,
                null,
                baseMessage.deliveredAt,
                baseMessage.readAt,
                textedBy,
                conversationType,
                "",
                -1,
                baseMessage.metadata
            )
            insertArrangedMessagesInRecyclerView(fetchMessageType, supplyObject)
        }
        if (baseMessage is MediaMessage) {
            LogUtils.info(TAG, "MediaMessage >>> 2.1: ${Gson().toJson(baseMessage)}")
            val supplyObject = ConversationLogRowDto(
                this@ConversationLogActivity,
                null,
                baseMessage.id,
                baseMessage.sender.uid,
                baseMessage.sender.name,
                avatar,
                baseMessage.sentAt,
                baseMessage.attachment,
                baseMessage.deliveredAt,
                baseMessage.readAt,
                textedBy,
                conversationType,
                "",
                -1,
                baseMessage.metadata
            )
            insertArrangedMessagesInRecyclerView(fetchMessageType, supplyObject)
        }
        if (baseMessage is CustomMessage){
            val supplyObject = ConversationLogRowDto(
                this@ConversationLogActivity,
                null,
                baseMessage.id,
                baseMessage.sender.uid,
                baseMessage.sender.name,
                avatar,
                baseMessage.sentAt,
                null,
                baseMessage.deliveredAt,
                baseMessage.readAt,
                textedBy,
                conversationType,
                Gson().toJson(baseMessage),
                -1,
                baseMessage.metadata
            )
            insertArrangedMessagesInRecyclerView(fetchMessageType, supplyObject)
        }
    }

    fun insertArrangedMessagesInRecyclerView(fetchMessageType: String, supplyObject: ConversationLogRowDto){
        if (fetchMessageType == AppConstant.FetchMessageType.OLD_MESSAGES){
            goingOnConversationList.addFirst(
                supplyObject
            )
            adapter.notifyItemInserted(0)
        }else{
            goingOnConversationList.add(
                supplyObject
            )
            adapter.notifyItemInserted(goingOnConversationList.size)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeMessageListener(realTimeMessageListenerID)
        CometChat.removeUserListener(onlineAndOfflineID)
        CometChat.removeUserListener(listenRealTimeReceiptsID)
        CometChat.removeCallListener(receiveIncomingCallListenerID)
        CometChat.removeConnectionListener(establishAConnectionID)
        EventBus.getDefault().unregister(this);
    }
}

