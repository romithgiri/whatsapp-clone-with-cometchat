package com.cometchat.ui.fragment.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.adapter.HadGroupConversationListAdapter
import com.cometchat.databinding.FragmentHadGroupConverstationBinding
import com.cometchat.dto.HadConversationItemCardDTO
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.core.GroupsRequest
import com.cometchat.pro.core.GroupsRequest.GroupsRequestBuilder
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.Group
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import com.google.gson.Gson


class HadGroupConversationFragment:  Fragment() {
    val TAG = "GroupChatFragment"
    private lateinit var binding: FragmentHadGroupConverstationBinding
    private lateinit var groupsRequest: GroupsRequest
    private lateinit var adapter: HadGroupConversationListAdapter
    private var data: MutableList<HadConversationItemCardDTO> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHadGroupConverstationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        data.clear()

        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        adapter = HadGroupConversationListAdapter(activity?.applicationContext!!, CometChatConstants.RECEIVER_TYPE_GROUP, data)
        binding.recyclerview.adapter = adapter
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    //Toast.makeText(requireContext(), "Reached to end ==>> going for next 15", Toast.LENGTH_LONG).show()
                    getGroupListIntoRecyclerView()
                    LogUtils.info(TAG, "chat history: 1.1 ==========>>>> 2: dataSize: ${data.size}")
                }
            }
        })

        groupsRequest = GroupsRequestBuilder().setLimit(15).build()
        getGroupListIntoRecyclerView()
    }

    private fun getGroupListIntoRecyclerView(){
        groupsRequest.fetchNext(object : CallbackListener<List<Group?>>() {
            override fun onSuccess(list: List<Group?>) {
                LogUtils.info(TAG, "Groups list received: Size:${list.size} --- data: ${Gson().toJson(list)}")
                if(list.isNullOrEmpty() && data.isNullOrEmpty()){
                    binding.tvNoConversation.visibility = View.VISIBLE
                    binding.recyclerview.visibility = View.GONE
                }else {
                    binding.tvNoConversation.visibility = View.GONE
                    binding.recyclerview.visibility = View.VISIBLE
                    list.forEach {
                        val uid = it?.guid
                        val avatar = it?.icon
                        val name = it?.name
                        val conversationType = CometChatConstants.CONVERSATION_TYPE_GROUP
                        val hasJoined = it?.isJoined ?: false
                        val obj = HadConversationItemCardDTO(
                            uid,
                            null,
                            avatar,
                            name,
                            false,
                            false,
                            conversationType,
                            hasJoined,
                            null,
                            null,
                            null,
                            0,
                            0,
                            0,
                            0,
                            0
                        )
                        data.add(obj)
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onError(e: CometChatException) {
                Utils.showToast(activity!!, "Error: ${e.message}")
                LogUtils.info(TAG, "Groups list fetching failed with exception: " + e.message)
            }
        })
    }
}