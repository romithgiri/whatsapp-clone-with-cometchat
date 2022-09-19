package com.cometchat.ui.activities.auth

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.databinding.ActivityRegisterBinding
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.cometchat.utils.LogUtils
import com.cometchat.utils.SDKConfig
import com.cometchat.utils.Utils

class Register : AppCompatActivity() {
    lateinit var binding: ActivityRegisterBinding
    private lateinit var loader: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Utils.showLoader(this)

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            when {
                binding.etUserId.text.isNullOrBlank() -> {
                    Utils.showToast(this, "Please enter user id")
                }
                binding.etUserName.text.isNullOrBlank() -> {
                    Utils.showToast(this, "Please enter user name")
                }
                else -> {
                    loader.show()
                    val authKey = SDKConfig.authKey
                    val user = User()
                    user.uid = binding.etUserId.text.toString()
                    user.name = binding.etUserName.text.toString()
                    CometChat.createUser(
                        user,
                        authKey,
                        object : CometChat.CallbackListener<User>() {
                            override fun onSuccess(user: User) {
                                loader.dismiss()
                                LogUtils.info("createUser", user.toString())
                                Utils.showToast(this@Register, "User created successfully")
                                finish()
                            }

                            override fun onError(e: CometChatException) {
                                loader.dismiss()
                                LogUtils.error("createUser", e.message ?: "Error message is null")
                            }
                        }
                    )
                }
            }
        }
    }


}