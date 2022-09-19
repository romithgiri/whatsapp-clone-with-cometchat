package com.cometchat.ui.fragment.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.adapter.StartNewConversationAdapter
import com.cometchat.databinding.FragmentStartNewConverstationBinding
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.UsersRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson


class StartNewConversationFragment:  Fragment() {
    val TAG = "StartNewConversationFragment"
    private lateinit var binding: FragmentStartNewConverstationBinding
    private var data: MutableList<User> = arrayListOf()
    private lateinit var userRequestBuilder: UsersRequest
    private lateinit var adapter: StartNewConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartNewConverstationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtils.info(TAG, "==============>>> Fragment: onViewCreated:: $TAG")
    }

    override fun onResume() {
        super.onResume()
        LogUtils.info(TAG, "==============>>> Fragment: $TAG")

        data.clear()
        adapter = StartNewConversationAdapter(activity?.applicationContext!!, CometChatConstants.RECEIVER_TYPE_USER, data)
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        binding.recyclerview.adapter = adapter
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    //Toast.makeText(requireContext(), "Reached to end ==>> going for next 15", Toast.LENGTH_LONG).show()
                    getUsersList()
                }
            }
        })

        userRequestBuilder = UsersRequest.UsersRequestBuilder()
            .setLimit(15)
            .build()

        getUsersList()
    }

    fun getUsersList(){
        userRequestBuilder.fetchNext(object : CometChat.CallbackListener<List<User>>(){
            override fun onSuccess(p0: List<User>?) {
                LogUtils.info(TAG, "User list received: ${p0?.size} --- data: ${Gson().toJson(p0)}")
                p0?.forEach {
                    LogUtils.info(TAG, "User list received: ${p0.size} --- data: 2: ${Gson().toJson(it)}")
                    data.add(it)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onError(p0: CometChatException?) {
                Utils.showToast(activity!!, "Error: ${p0?.message}")
                LogUtils.error(TAG, "User list fetching failed with exception: " + p0?.message)
            }
        })
    }
}