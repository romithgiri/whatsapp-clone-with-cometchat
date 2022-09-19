package com.cometchat.ui.activities.auth

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cometchat.databinding.ActivityLoginBinding
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.cometchat.service.CallManager
import com.cometchat.ui.activities.LandingActivity
import com.cometchat.utils.LogUtils
import com.cometchat.utils.SDKConfig
import com.cometchat.utils.Utils
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson


class Login : AppCompatActivity() {
    private val TAG = "Login"
    lateinit var callManager: CallManager
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loader: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Utils.showLoader(this)

        LogUtils.info(TAG, "Login Screen")

        if (CometChat.getLoggedInUser() != null) {
            startActivity(Intent(this@Login, LandingActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (callManager.checkAccountConnection(this)) {
                    if (binding.etLoginUserId.text.isNullOrBlank()) {
                        Utils.showToast(this, "Please enter user id")
                    } else {
                        loader.show()
                        val uID = binding.etLoginUserId.text.toString() // Replace with the UID of the user to login
                        val authKey = SDKConfig.authKey // Replace with your App Auth Key
                        if (CometChat.getLoggedInUser() == null) {
                            CometChat.login(uID, authKey, object : CometChat.CallbackListener<User>() {
                                override fun onSuccess(p0: User?) {
                                    LogUtils.info(TAG, "Login Successful : ${Gson().toJson(p0)}")
                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                        if (task.isComplete) {
                                            registerFCMToken(task.result)
                                        }
                                    }
                                }

                                override fun onError(p0: CometChatException?) {
                                    loader.dismiss()
                                    LogUtils.info(TAG, "Login failed with exception: " + p0?.message)
                                    Utils.showToast(this@Login, p0?.message ?: "Error in login!!")
                                }
                            })
                        } else {
                            // User already logged in
                            LogUtils.info(TAG, "User already logged in: ${Gson().toJson(CometChat.getLoggedInUser())}")
                            Utils.showToast(this, "User already logged in")
                            startActivity(Intent(this@Login, LandingActivity::class.java))
                        }
                    }
                }else{
                    loader.show()
                    val uID = binding.etLoginUserId.text.toString() // Replace with the UID of the user to login
                    val authKey = SDKConfig.authKey // Replace with your App Auth Key
                    if (CometChat.getLoggedInUser() == null) {
                        CometChat.login(uID, authKey, object : CometChat.CallbackListener<User>() {
                            override fun onSuccess(p0: User?) {
                                LogUtils.info(TAG, "Login Successful : ${Gson().toJson(p0)}")
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isComplete) {
                                        registerFCMToken(task.result)
                                    }
                                }
                            }

                            override fun onError(p0: CometChatException?) {
                                loader.dismiss()
                                LogUtils.info(TAG, "Login failed with exception: " + p0?.message)
                                Utils.showToast(this@Login, p0?.message ?: "Error in login!!")
                            }
                        })
                    } else {
                        // User already logged in
                        LogUtils.info(TAG, "User already logged in: ${Gson().toJson(CometChat.getLoggedInUser())}")
                        Utils.showToast(this, "User already logged in")
                        startActivity(Intent(this@Login, LandingActivity::class.java))
                    }
                    showAlertForVOIP()
                }
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 20);
            }
        }

        binding.btnRegister.setOnClickListener {
            LogUtils.info(TAG, "====>>>> register screen nav")
            startActivity(Intent(this, Register::class.java))
        }

        callManager = CallManager(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (!callManager.checkAccountConnection(this)) {
                showAlertForVOIP()
            }
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 20);
        }
    }

    fun registerFCMToken(token: String?){
        if(token != null){
            CometChat.registerTokenForPushNotification(token, object : CometChat.CallbackListener<String?>() {
                override fun onSuccess(s: String?) {
                    LogUtils.info(TAG, "================>>>> onSuccessPN: 2: $s ---- toke: $token")
                    loader.dismiss()
                    Utils.showToast(this@Login,"Login successfully completed")
                    startActivity(Intent(this@Login, LandingActivity::class.java))
                    finish()
                }

                override fun onError(e: CometChatException) {
                    LogUtils.error(TAG, "================>>>> onErrorPN: ${e.message}")

                }
            })
        }else{
            Utils.showToast(this, "Unable to login. Error: FCM token")
        }
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
        ) { dialog, which -> callManager.launchVoIPSetting(this@Login) }
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.dismiss() }
        alertDialog.create().show()
    }

}