package com.cometchat.service

import android.Manifest
import android.app.Notification.DEFAULT_SOUND
import android.app.Notification.DEFAULT_VIBRATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cometchat.R
import com.cometchat.dto.HadConversationItemCardDTO
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.ConnectionListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.helpers.CometChatHelper
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.models.Group
import com.cometchat.ui.activities.CallingActivity
import com.cometchat.ui.activities.MainActivity
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.cometchat.utils.SDKConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class MyFirebaseMessagingService : FirebaseMessagingService() {
    private lateinit var json: JSONObject
    private val intent: Intent? = null
    private var count = 0
    private var call: Call? = null
    private var isCall = false
    private var messageNotificationChannelIDUniqueNotificationId = 101
    private val realTimeMessageArrivalID = "realTimeMessageArrivalMyFirebaseMessagingService"
    private val establishAConnectionID = "establishAConnectionMyFirebaseMessagingService"

    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var notificationBuilder: NotificationCompat.Builder
    var notificationChannelID = "messageNotificationChannelID"
    var notificationChannelName = "messageNotificationChannel"
    var notificationGroupID = "CometChatNotification"


    var summaryNotificationBuilder: NotificationCompat.Builder? = null
    var bundleNotificationId = 100
    var singleNotificationId = 100
    var GROUP_KEY_WORK_EMAIL = "com.android.example.WORK_EMAIL"

    companion object {
        private const val TAG = "MyFirebaseService"
        var token: String? = null
        var isNotificationChannelCreated = false
        var notificationManagerCompat: NotificationManagerCompat ?= null
    }

    override fun onNewToken(s: String) {
        token = s
        LogUtils.info(TAG, "================>>>> onSuccessPN: 1: $s ---- toke: $token")
    }

    /*override fun onCreate() {
        super.onCreate()
        LogUtils.info(TAG, "=====>>>> ::: onCreate called >>> isNotificationChannelCreated: $isNotificationChannelCreated")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!isNotificationChannelCreated){
                createNotificationChannel()
            }
        }
        if (aa == null){
            aa = NotificationManagerCompat.from(this)
        }
    }*/

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            count++
            json = JSONObject(remoteMessage.data as Map<*, *>)
            Log.d(TAG, "Firebase FCM JSONObject ===>>>>: $json")
            val messageData = JSONObject(json.getString("message"))
            val baseMessage = CometChatHelper.processMessage(JSONObject(remoteMessage.data["message"]))
            initCometChatSDK(baseMessage)
            if (baseMessage is Call) {
                call = baseMessage
                isCall = true
            }
            createNotificationChannel()
            showNotification(baseMessage)

            //showNotificationTest(baseMessage)

            /*sendNotification2(
                "Temp Body",
            "Temp",
                json?.getString("alert")!!
            )*/
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /*private fun showNotification(baseMessage: BaseMessage) {
        try {
            val m = Date().time.toInt()
            val GROUP_ID = "group_id"
            val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "2")
                .setSmallIcon(R.drawable.ic_photo_media)
                .setContentTitle(json!!.getString("title"))
                .setContentText(json!!.getString("alert"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setLargeIcon(getBitmapFromURL(baseMessage.sender.avatar))
                .setGroup(GROUP_ID)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            val summaryBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "2")
                .setContentTitle("CometChat")
                .setContentText("$count messages")
                .setSmallIcon(R.drawable.ic_photo_media)
                .setGroup(GROUP_ID)
                .setGroupSummary(true)
            val notificationManager = NotificationManagerCompat.from(this)

            notificationManager.notify(baseMessage.id, builder.build())
            notificationManager.notify(0, summaryBuilder.build())

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    /*private fun getCallIntent(title: String): Intent {
        val callIntent = Intent(applicationContext, CallNotificationAction::class.java)
        callIntent.putExtra(StringContract.IntentStrings.SESSION_ID, call!!.sessionId)
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        callIntent.action = title
        return callIntent
    }*/

    private fun showNotification(baseMessage: BaseMessage) {
        LogUtils.info(TAG, "=====>>>> ::: showNotification called >>> isIncomingCall: $isCall >>> baseMessage: ${Gson().toJson(baseMessage)}")
        val pendingIntent = if(isCall){
            PendingIntent.getActivity(this, 0, getReadyDataForIntentCall(baseMessage), PendingIntent.FLAG_UPDATE_CURRENT)
        }else{
            PendingIntent.getActivity(this, 1, getReadyDataForIntentMessage(baseMessage), PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val avatar = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            if(baseMessage.sender.avatar != null){
                getBitmapFromURL(baseMessage.sender.avatar)
            }else{
                getBitmapFromURL("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460__480.png")
            }
        }else{
            if((baseMessage.receiver as Group).icon != null){
                getBitmapFromURL((baseMessage.receiver as Group).icon)
            }else{
                getBitmapFromURL("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460__480.png")
            }
        }
        val title = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender.name
        }else{
            (baseMessage.receiver as Group).name
        }

        notificationBuilder = NotificationCompat.Builder(this, notificationChannelID)
            .setSmallIcon(R.drawable.cc)
            .setContentTitle(title)
            .setContentText(json.getString("alert"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(json.getString("alert")))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(notificationGroupID)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if(avatar != null){
            notificationBuilder.setLargeIcon(avatar)
        }

        val summaryBuilder = NotificationCompat.Builder(this, notificationChannelID)
            .setContentTitle("CometChat")
            .setContentText("$count messages")
            .setSmallIcon(R.drawable.cc)
            .setGroup(notificationGroupID)
            .setGroupSummary(true)

        if (isCall) {
            LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 0.1 : ${checkAccountConnection(this)}")
            if(checkAccountConnection(this)){
                LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 1")
                val callManager = CallManager(applicationContext)
                callManager.startIncomingCall(baseMessage)
            }else{
                LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 0.2")
                notificationBuilder.setContentTitle("Incoming Call")
                notificationBuilder.setContentText("Please grant required permission")
                val pendingIntent = PendingIntent.getActivity(this, 1, launchVoIPSetting(this), PendingIntent.FLAG_UPDATE_CURRENT)
                notificationBuilder.setContentIntent(pendingIntent)
                notificationManager.notify(0, notificationBuilder.build())
            }

            /*notificationBuilder.setGroup(notificationGroupID + "Call")
            if (json.getString("alert") == "Incoming audio call" || json.getString("alert") == "Incoming video call") {
                notificationBuilder.setOngoing(true)
                notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
                notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            }
            notificationBuilder.setContentIntent(pendingIntent)
            notificationManager.notify(0, notificationBuilder.build())*/
        } else {
            notificationBuilder.setContentIntent(pendingIntent)
            notificationManager.notify(baseMessage.id, notificationBuilder.build())
            notificationManager.notify(1, summaryBuilder.build())
        }
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? {
        val url = URL(strURL)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        return getCircleBitmap(BitmapFactory.decodeStream(input))
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        bitmap.recycle()
        return output
    }

    fun getReadyDataForIntentMessage(baseMessage: BaseMessage): Intent {
        val uid = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender?.uid
        }else{
            (baseMessage.receiver as Group).guid
        }
        val conversationId = baseMessage.conversationId
        val avatar = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender.avatar
        }else{
            (baseMessage.receiver as Group).icon
        }
        val name = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender.name
        }else{
            (baseMessage.receiver as Group).name
        }
        val isOnline = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender.status == "online"
        }else{
            false
        }
        val isTyping= false
        val conversationType = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            CometChatConstants.CONVERSATION_TYPE_USER
        }else{
            CometChatConstants.CONVERSATION_TYPE_GROUP
        }
        val hasJoined = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            false
        }else{
            LogUtils.info(TAG, "(baseMessage.receiver as Group).isJoined ===> ${(baseMessage.receiver as Group).isJoined}")
            (baseMessage.receiver as Group).isJoined
        }
        val lastMessageText = ""

        val lastMessageSenderId = baseMessage.sender.uid
        val lastMessageType = if(baseMessage.type == "text"){
            "text"
        }else{
            "Attachment"
        }
        val lastMessageDeliveredAtTimeStamp= baseMessage.deliveredAt
        val lastMessageDeliveredToMeAtStamp= baseMessage.deliveredToMeAt
        val lastMessageSentAtAtStamp= baseMessage.sentAt
        val lastMessageReadAtStamp= baseMessage.readAt
        val unreadMessageCount = 0

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
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("obj", Gson().toJson(obj))
        intent.putExtra("conversationType", obj.conversationType)
        intent.putExtra("notificationType", "textMessage")
        return intent
    }


    fun getReadyDataForIntentCall(baseMessage: BaseMessage): Intent {
        val uid = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender?.uid
        }else{
            (baseMessage.receiver as Group).guid
        }
        val conversationType = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            CometChatConstants.CONVERSATION_TYPE_USER
        }else{
            CometChatConstants.CONVERSATION_TYPE_GROUP
        }
        val callType = if(json.getString("alert").equals("Incoming audio call")){
            CometChatConstants.CALL_TYPE_AUDIO
        }else{
            CometChatConstants.CALL_TYPE_VIDEO
        }
        val intent = Intent(this, CallingActivity::class.java)
        intent.putExtra("receiverID", uid)
        intent.putExtra("conversationType", conversationType)
        intent.putExtra("callType", callType)
        intent.putExtra("operationType", AppConstant.OperationType.IncomingCall)
        intent.putExtra("callObjectData", Gson().toJson(call))
        return intent
    }

    fun initCometChatSDK(baseMessage: BaseMessage){
        LogUtils.info(TAG, "=====>>>> ::: initCometChatSDK called")

        val appSettings: AppSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(SDKConfig.region)
            .autoEstablishSocketConnection(true)
            .build()

        CometChat.init(
            this,
            SDKConfig.appID,
            appSettings,
            object : CometChat.CallbackListener<String>() {
                override fun onSuccess(p0: String?) {
                    LogUtils.info(TAG, "=====>>>> ::: CometChat.init onSuccess: $p0 >>>> getConnectionStatus: ${CometChat.getConnectionStatus()}")
                    if (CometChat.getConnectionStatus() == CometChatConstants.WS_STATE_CONNECTED){
                        callForDeliveryReport(baseMessage)
                    }else{
                        establishAConnection(baseMessage)
                    }
                }
                override fun onError(p0: CometChatException?) {
                    LogUtils.error(TAG, "=====>>>> ::: CometChat.init onError: ${p0?.message}")
                }
            }
        )
    }

    private fun establishAConnection(baseMessage: BaseMessage){
        LogUtils.info(TAG, "=====>>>> ::: establishAConnection called")
        CometChat.addConnectionListener(establishAConnectionID, object : ConnectionListener {
            override fun onConnected() {
                LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onConnected")
                callForDeliveryReport(baseMessage)
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

    private fun callForDeliveryReport(baseMessage: BaseMessage){
        if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onConnected >>>> RECEIVER_TYPE_USER: ${CometChatConstants.RECEIVER_TYPE_USER}")
            markAsDelivered(
                baseMessage.id,
                baseMessage.sender.uid,
                CometChatConstants.RECEIVER_TYPE_USER,
                CometChat.getLoggedInUser().uid
            )
        }else{
            LogUtils.info(TAG, "=====>>>> ::: establishAConnection >>>> onConnected >>>> RECEIVER_TYPE_USER: ${CometChatConstants.RECEIVER_TYPE_GROUP}")
            markAsDelivered(
                baseMessage.id,
                baseMessage.receiverUid,
                CometChatConstants.RECEIVER_TYPE_GROUP,
                baseMessage.sender.uid
            )
        }
    }

    private fun markAsDelivered(
        msgId: Int,
        receiverId: String,
        receiverTypeUser: String,
        senderUID: String
    ) {
        LogUtils.info(TAG, "=====>>>> ::: markAsDelivered called: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
        CometChat.markAsDelivered(msgId, receiverId, receiverTypeUser, senderUID,
            object : CometChat.CallbackListener<Void?>() {
                override fun onSuccess(unused: Void?) {
                    LogUtils.info(TAG, "=====>>>> ::: markAsDelivered called >>> onSuccess: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
                }

                override fun onError(e: CometChatException) {
                    LogUtils.info(TAG, "=====>>>> ::: markAsDelivered called >>> onError: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
                }
            }
        )
    }






    private fun createNotificationChannel(): Boolean{
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(notificationChannelID, notificationChannelName, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
            isNotificationChannelCreated = true
            return true
        }
        return true
    }

    private fun createNotificationSummary(){
        val notification = NotificationCompat.Builder(this, AppConstant.notificationMessageChannelID)
            .setSmallIcon(R.drawable.cc)
            .setGroup(AppConstant.notificationMessageChannelGroupID)
            .setGroupSummary(true)
            .setStyle(NotificationCompat.InboxStyle())
            .build()
        notificationManagerCompat?.notify(303, notification)
    }

    private fun showNotificationTest(baseMessage: BaseMessage) {
        LogUtils.info(TAG, "=====>>>> ::: showNotification called >>> received data: ${Gson().toJson(baseMessage)}")
        initCometChatSDK(baseMessage)
        val pendingIntent = PendingIntent.getActivity(this, 0, getReadyDataForIntentMessage(baseMessage), PendingIntent.FLAG_UPDATE_CURRENT)

        val avatar = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            if(baseMessage.sender.avatar != null){
                getBitmapFromURL(baseMessage.sender.avatar)
            }else{
                getBitmapFromURL("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460__480.png")
            }
        }else{
            if((baseMessage.receiver as Group).icon != null){
                getBitmapFromURL((baseMessage.receiver as Group).icon)
            }else{
                getBitmapFromURL("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460__480.png")
            }
        }

        val title = if(baseMessage.receiverType == CometChatConstants.RECEIVER_TYPE_USER){
            baseMessage.sender.name
        }else{
            (baseMessage.receiver as Group).name
        }

        notificationBuilder = NotificationCompat.Builder(this, notificationChannelID)
            .setSmallIcon(R.drawable.cc)
            .setContentTitle(title)
            .setContentText(json?.getString("alert"))
            .setContentIntent(pendingIntent)
            .setGroup(AppConstant.notificationMessageChannelGroupID)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(json?.getString("alert")))
        if(avatar != null){
            notificationBuilder.setLargeIcon(avatar)
        }
        notificationManagerCompat?.notify(baseMessage.id, notificationBuilder.build())
        createNotificationSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.info(TAG, "=====>>>> ::: onDestroy called")
        //CometChat.removeConnectionListener(establishAConnectionID)
    }











    /** =================================== new ==================*/
    fun sendNotification2(
        messageBody: String,
        title: String,
        payloadExtraData: String
    ) {
        val id = System.currentTimeMillis().toInt()
        //  Create Notification
        val bm = BitmapFactory.decodeResource(application.resources, R.drawable.cc)
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val v = longArrayOf(200, 500)
        // Notification Group Key
        val groupKey = "bundle_notification_$bundleNotificationId"

        //  Notification Group click intent
        var resultIntent = Intent(applicationContext, MainActivity::class.java)
        resultIntent.putExtra("payloadExtraData", payloadExtraData)
        resultIntent.putExtra("notification_id", bundleNotificationId)
        resultIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var resultPendingIntent = PendingIntent.getActivity(
            this,
            bundleNotificationId,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // We need to update the bundle notification every time a new notification comes up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogUtils.info(TAG, "payloadExtraData =====>>>> : notificationManager?.notificationChannels?.size!! : ${notificationManager?.notificationChannels?.size!!}")

            //if (notificationManager?.notificationChannels?.size!! < 2) {
                val groupChannel = NotificationChannel(
                    "bundle_channel_id",
                    "bundle_channel_name",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager?.createNotificationChannel(groupChannel)
                val channel = NotificationChannel(
                    "channel_id",
                    "channel_name",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager?.createNotificationChannel(channel)
            //}
        }
        summaryNotificationBuilder = NotificationCompat.Builder(this, "bundle_channel_id")
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setSmallIcon(R.drawable.cc)
            .setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
            .setLargeIcon(bm)
            .setAutoCancel(true)
            .setContentIntent(resultPendingIntent)
        if (singleNotificationId == bundleNotificationId) singleNotificationId =
            bundleNotificationId else singleNotificationId++

        //  Individual notification click intent
        resultIntent = Intent(applicationContext, MainActivity::class.java)
        resultIntent.putExtra("payloadExtraData", payloadExtraData)
        resultIntent.putExtra("notification_id", singleNotificationId)
        resultIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        resultPendingIntent = PendingIntent.getActivity(
            this,
            singleNotificationId,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: NotificationCompat.Builder =
            NotificationCompat.Builder(this, "channel_id")
                .setGroup(groupKey)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setSmallIcon(R.drawable.cc)
                .setLargeIcon(bm)
                .setSound(defaultSoundUri)
                .setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setContentIntent(resultPendingIntent)
        notificationManager?.notify(id, notification.build())
        notificationManager?.notify(bundleNotificationId, summaryNotificationBuilder!!.build())
    }

    /** ==================================================== end ============================*/



    /** ==================================================== calling alert ============================*/

    open fun launchVoIPSetting(context: Context): Intent {
        val intent = Intent()
        intent.action = TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return intent
    }

    fun checkAccountConnection(context: Context?): Boolean {
        val telecomManager = context!!.getSystemService(TELECOM_SERVICE) as TelecomManager
        var isConnected = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val enabledAccounts: List<PhoneAccountHandle> = telecomManager.callCapablePhoneAccounts
            for (account in enabledAccounts) {
                if (account.componentName.className == MyConnectionService::class.java.canonicalName) {
                    isConnected = true
                    break
                }
            }
        }
        return isConnected
    }
}