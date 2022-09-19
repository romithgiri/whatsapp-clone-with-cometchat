package com.cometchat.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.cometchat.R
import com.cometchat.adapter.TabAdapter
import com.cometchat.databinding.ActivityLandingBinding
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*
import com.cometchat.service.CallManager
import com.cometchat.ui.activities.auth.Login
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils
import com.cometchat.utils.PermissionHelper
import com.cometchat.utils.Utils
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson


class LandingActivity : AppCompatActivity() {
    private val TAG = "LandingActivity"
    private lateinit var binding: ActivityLandingBinding
    lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager
    lateinit var callManager: CallManager
    private val realTimeMessageArrivalID = "realTimeMessageArrivalLandingActivity"
    private val receiveIncomingCallListenerID = "receiveIncomingCallListenerIDLandingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "CometChat"
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tabLayout.addTab(tabLayout.newTab().setText("Chat"))
        tabLayout.addTab(tabLayout.newTab().setText("Group Chat"))
        tabLayout.addTab(tabLayout.newTab().setText("Users"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val adapter = TabAdapter(this, supportFragmentManager, tabLayout.tabCount)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.materialToolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.menu_main_new_group -> {
                    startActivity(Intent(this, CreateConversationGroup::class.java))
                    //Utils.showToast(this, "New Group")
                }
                R.id.menu_main_settings -> {
                    Utils.showToast(this, "Settings")
                }
                R.id.menu_blocked_list -> {
                    startActivity(Intent(this, BlockedUsers::class.java))
                }
                R.id.menu_logout -> {
                    CometChat.logout(object : CometChat.CallbackListener<String>() {
                        override fun onSuccess(p0: String?) {
                            LogUtils.info(TAG, "Logout from current account.")
                            Utils.showToast(this@LandingActivity, "Logout from current account")
                            startActivity(Intent(this@LandingActivity, Login::class.java))
                            finishAffinity()
                        }

                        override fun onError(p0: CometChatException?) {
                            LogUtils.error(TAG, "Fail to logout from current account. ${p0?.message}")
                            Utils.showToast(this@LandingActivity, "Fail to logout from current account. ${p0?.message}")
                        }
                    })
                }
            }
            true
        }

        if(!PermissionHelper.checkPushNotificationPermission(this)){
            PermissionHelper.requestForPushNotificationPermission(this)
        }

        receiveIncomingCallListener()
        realTimeMessageArrivalListener()

        callManager = CallManager(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (!callManager.checkAccountConnection(this)) {
                showAlertForVOIP()
            }
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 20);
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_landing_activity, menu)
    }

    private fun receiveIncomingCallListener(){
        CometChat.addCallListener(receiveIncomingCallListenerID, object :CometChat.CallListener(){
            override fun onOutgoingCallAccepted(p0: Call?) {
                LogUtils.info(TAG, "Outgoing call accepted: " + p0?.toString())

            }
            override fun onIncomingCallReceived(p0: Call?) {
                LogUtils.info(TAG, "Incoming call: ${Gson().toJson(p0)}")
                val intent = Intent(this@LandingActivity, CallingActivity::class.java)
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
            object : CallbackListener<Void?>() {
                override fun onSuccess(unused: Void?) {
                    LogUtils.info(TAG, "markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
                }

                override fun onError(e: CometChatException) {
                    LogUtils.error(TAG, "markAsDelivered======>>>> : 3: msgId:$msgId --- receiverId: $receiverId --- receiverTypeUser: $receiverTypeUser --- senderUID: $senderUID")
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
        when(requestCode){
            20 -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    showAlertForVOIP()
                }else{
                    Toast.makeText(this, "Please grant required permission", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showAlertForVOIP() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("VoIP Permission")
        alertDialog.setMessage(
            "To make VoIP Calling work properly, you need to allow certain " +
                    "permission from your call account settings for this app."
        )
        alertDialog.setPositiveButton(
            "Open Settings"
        ) { dialog, which -> callManager.launchVoIPSetting(this@LandingActivity) }
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.dismiss() }
        alertDialog.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeMessageListener(realTimeMessageArrivalID);
        CometChat.removeCallListener(receiveIncomingCallListenerID);
    }

}