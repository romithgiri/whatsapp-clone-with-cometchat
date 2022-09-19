package com.cometchat.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.R
import com.cometchat.dto.ConversationLogRowDto
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.ui.activities.auth.Login
import com.cometchat.ui.activities.conversation.ConversationLogActivity
import com.cometchat.utils.LogUtils
import com.cometchat.utils.SDKConfig
import java.util.*

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val receivedObj = intent.getStringExtra("obj")
        val conversationType = intent.getStringExtra("conversationType")
        val notificationType = intent.getStringExtra("notificationType")
        val payloadExtraData = intent.getStringExtra("payloadExtraData")
        LogUtils.info(TAG, "payloadExtraData =====>>>> : $payloadExtraData")

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
                    if (CometChat.getLoggedInUser() != null){
                        if(receivedObj != null && conversationType != null && notificationType != null){
                            val intent = Intent(this@MainActivity, ConversationLogActivity::class.java)
                            intent.putExtra("obj", receivedObj)
                            intent.putExtra("conversationType", conversationType)
                            intent.putExtra("notificationType", notificationType)
                            startActivity(intent)
                            finish()
                        }else{
                            LogUtils.info(TAG, "=====>>>>: Initialization completed successfully")
                            startActivity(Intent(this@MainActivity, Login::class.java))
                            finish()
                        }
                    }else{
                        LogUtils.info(TAG, "=====>>>>: Initialization completed successfully")
                        startActivity(Intent(this@MainActivity, Login::class.java))
                        finish()
                    }
                }

                override fun onError(p0: CometChatException?) {
                    LogUtils.error(TAG, "=====>>>>: Initialization failed with exception: " + p0?.message)
                }
            }
        )

        //check()
    }

    fun check(){
        var myList: Deque<Int> = LinkedList()
        var tempMyArray: Deque<Int> = LinkedList()

        for(i in 30 downTo 21){
            myList.addFirst(i)
        }

        LogUtils.info(TAG, "KEY========>>> 1st: $myList")

        for(i in 20 downTo 11){
            myList.addFirst(i)
        }

        LogUtils.info(TAG, "KEY========>>> 2nd: $myList")

        for(i in 10 downTo 0){
            myList.addFirst(i)
        }

        LogUtils.info(TAG, "KEY========>>> 3rd: $myList")


    }
}