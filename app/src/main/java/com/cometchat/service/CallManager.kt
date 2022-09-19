package com.cometchat.service

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cometchat.R
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.models.Group
import com.cometchat.pro.models.User
import com.cometchat.ui.activities.CallingActivity
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.google.gson.Gson

class CallManager(var context: Context) {
    val TAG = "CallManager"
    var telecomManager: TelecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    var phoneAccountHandle: PhoneAccountHandle? = null

    init {
        val componentName = ComponentName(context, MyConnectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            phoneAccountHandle = PhoneAccountHandle(componentName, context.packageName)
            val phoneAccount = PhoneAccount.builder(phoneAccountHandle, context.packageName).setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER).build()
            Log.e("CallManager: ", phoneAccount.toString())
            telecomManager.registerPhoneAccount(phoneAccount)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startOutgoingCall() {
        val extras = Bundle()
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
        val manager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = PhoneAccountHandle(
            ComponentName(
                context.packageName,
                MyConnectionService::class.java.name
            ), "estosConnectionServiceId"
        )
        val test = Bundle()
        test.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        test.putInt(
            TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
            VideoProfile.STATE_BIDIRECTIONAL
        )
        test.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras)
        try {
            if (context.checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
                manager.placeCall(Uri.parse("tel:\$number"), test)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startIncomingCall(baseMessage: BaseMessage) {
        LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 2")
        val call = (baseMessage as Call)
        LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 3")
        if (context.checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
            LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 4")

            val uri: Uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, call.sessionId.substring(0, 11), null)

            val extras = Bundle()
            extras.putString(AppConstant.IntentStrings.SESSION_ID, call.sessionId)
            extras.putString(AppConstant.IntentStrings.TYPE, call.receiverType)
            extras.putString(AppConstant.IntentStrings.CALL_TYPE, call.type)
            extras.putString(AppConstant.IntentStrings.ID, call.receiverUid)

            if (call.receiverType.equals(CometChatConstants.RECEIVER_TYPE_GROUP, true)) extras.putString(
                AppConstant.IntentStrings.NAME,
                (call.receiver as Group).name
            ) else extras.putString(
                AppConstant.IntentStrings.NAME,
                (call.callInitiator as User).name
            )
            if (call.type.equals(CometChatConstants.CALL_TYPE_VIDEO, true)) extras.putInt(
                TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL
            ) else extras.putInt(
                TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                VideoProfile.STATE_AUDIO_ONLY
            )
            extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)

            val isCallPermitted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.isIncomingCallPermitted(phoneAccountHandle)
            } else {
                true
            }

            LogUtils.info(
                "CallManager",
                "is incoming call permitted = $isCallPermitted\n$phoneAccountHandle"
            )

            LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 6")

            try {
                LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 7")
                telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
            } catch (e: SecurityException) {
                LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 8")
                e.printStackTrace()
                val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, "2")
                    .setSmallIcon(R.drawable.cc)
                    .setContentTitle((call.callInitiator as User).name)
                    .setContentText(
                        call.callStatus.toUpperCase() + " " + call.type.toUpperCase() + " " + "Call"
                    )
                    .setColor(context.getColor(R.color.colorPrimary))
                    .setGroup("group_id")
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                builder.priority = NotificationCompat.PRIORITY_HIGH
                builder.setCategory(NotificationCompat.CATEGORY_CALL)
                builder.addAction(
                    0,
                    "Answers",
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        0,
                        getCallIntent("Answer_", baseMessage),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                builder.addAction(
                    0,
                    "Decline",
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        1,
                        getCallIntent("Decline_", call),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(3, builder.build())
            } catch (e: Exception) {
                LogUtils.error("CallManagerError: ", e.message.toString())
            }
        }else{
            LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 5")
        }
    }

    private fun getCallIntent(title: String, baseMessage: BaseMessage): Intent {
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
        val callType = (baseMessage as Call).receiverType
        val intent = Intent(context, CallingActivity::class.java)
        intent.putExtra("receiverID", uid)
        intent.putExtra("conversationType", conversationType)
        intent.putExtra("callType", callType)
        intent.putExtra("operationType", AppConstant.OperationType.IncomingCall)
        intent.putExtra("callObjectData", Gson().toJson(baseMessage))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent
    }


    fun launchVoIPSetting(context: Context) {
        val intent = Intent()
        intent.action = TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS
        val telecomComponent = ComponentName(
            "com.android.server.telecom",
            "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
        )
        intent.component = telecomComponent
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        /*val intent = Intent()
        intent.action = TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP*/

        context.startActivity(intent)
    }

    fun checkAccountConnection(context: Context?): Boolean {
        LogUtils.info(TAG, "=================::: ZZZZ: 1")
        var isConnected = false
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            LogUtils.info(TAG, "=================::: ZZZZ: 2")
            val enabledAccounts = telecomManager.callCapablePhoneAccounts
            for (account: PhoneAccountHandle in enabledAccounts) {
                LogUtils.info(TAG, "=================::: ZZZZ: 3: ${account.componentName.className} --- ${MyConnectionService::class.java.canonicalName}")
                if ((account.componentName.className == MyConnectionService::class.java.canonicalName)) {
                    LogUtils.info(TAG, "=================::: ZZZZ: 4")
                    isConnected = true
                    break
                }
            }
            LogUtils.info(TAG, "=================::: ZZZZ: 5")
        }
        LogUtils.info(TAG, "=================::: ZZZZ: 6")
        return isConnected
    }

    fun endCall() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        telecomManager!!.endCall()
    }

}