package com.cometchat.ui.activities.conversation

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.adapter.PollOptionsAdapter
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject


class CustomPollDialog(
    var activity: Activity,
    private val guid: String,
    val conversationType: String
) : Dialog(activity), PollOptionsAdapter.PollOptionsAdapterListener {
    private val TAG = "CustomPollDialog"
    lateinit var closePollDialog: ImageView
    lateinit var btnCreatePoll: Button
    lateinit var btnAddNewPollOption: Button
    lateinit var pollQuestion: EditText
    lateinit var pollOptionsRecyclerView: RecyclerView

    private lateinit var adapter: PollOptionsAdapter

    companion object {
        var pollOptionList: MutableList<String> = arrayListOf()
    }
    //var pollOptionList: MutableList<String> = arrayListOf()
    //var pollOptionListTemp: MutableList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.poll_layout)

        val window = this.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        closePollDialog = findViewById(R.id.iv_close_poll_dialog)
        btnCreatePoll = findViewById(R.id.btn_create_poll)
        btnAddNewPollOption = findViewById(R.id.btn_add_new_poll_option)
        pollQuestion = findViewById(R.id.et_poll_question)
        pollOptionsRecyclerView = findViewById(R.id.poll_options_Recyclerview)

        adapter = PollOptionsAdapter(activity, pollOptionList)
        adapter.initPollOptionsAdapterListener(this)
        pollOptionsRecyclerView.layoutManager = LinearLayoutManager(activity)
        pollOptionsRecyclerView.adapter = adapter

        for (i in 0..1) {
            pollOptionList.add("")
            adapter.notifyItemInserted(pollOptionList.size - 1)
        }

        closePollDialog.setOnClickListener {
            dismiss()
            pollOptionList.clear()
            pollQuestion.text.clear()
        }

        btnAddNewPollOption.setOnClickListener {
            LogUtils.info(TAG, "onClick of add new option ==========>>>>>>>")
            pollOptionList.add("")
            adapter.notifyItemInserted(pollOptionList.size - 1)
        }

        btnCreatePoll.setOnClickListener {
            val body = JSONObject()
            val options = JSONArray()

            if (pollQuestion.text.isNullOrBlank()) {
                Utils.showToast(activity, "Please enter poll question")
            } else if (pollOptionList[0].isBlank() || pollOptionList[1].isBlank() || pollOptionList.size < 2) {
                Utils.showToast(activity, "Please enter poll options")
            } else {
                for (i in pollOptionList) {
                    options.put(i)
                    LogUtils.info(
                        TAG,
                        "Poll ========>>>> array: 1: i: $i ---- ${Gson().toJson(options)}"
                    )
                }
                val loader = Utils.showLoader(activity)
                loader.show()
                body.put("question", pollQuestion.text)
                body.put("options", options)
                body.put("receiver", guid)
                body.put("receiverType", conversationType)

                CometChat.callExtension("polls", "POST", "/v2/create", body,
                    object : CometChat.CallbackListener<JSONObject?>() {
                        override fun onSuccess(jsonObject: JSONObject?) {
                            //On Success
                            LogUtils.info(
                                TAG,
                                "Poll ========>>>> Success: 1: ${Gson().toJson(jsonObject)}"
                            )
                            dismiss()
                            loader.dismiss()
                            pollOptionList.clear()
                            pollQuestion.text.clear()
                        }

                        override fun onError(e: CometChatException) {
                            //On Failure
                            LogUtils.info(TAG, "Poll ========>>>> Failure: ${e}")
                            loader.dismiss()
                            Utils.showToast(activity, "Unable to create poll")
                        }
                    }
                )
            }
        }
    }

    override fun onDeletePollOptionClick(position: Int) {
        LogUtils.info(TAG, "onClick delete option ==========>>>>>>> : $position")
        pollOptionList.removeAt(position)
        adapter.notifyDataSetChanged()
    }


}