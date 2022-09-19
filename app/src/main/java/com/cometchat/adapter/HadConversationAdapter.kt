package com.cometchat.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.dto.EventBusResponseDto
import com.cometchat.dto.HadConversationItemCardDTO
import com.cometchat.dto.RealTimeUpdateForHadConversationItemCardDTO
import com.cometchat.pro.core.CometChat
import com.cometchat.ui.activities.conversation.ConversationLogActivity
import com.cometchat.ui.fragment.conversation.HadConversationFragment.Companion.realTimeUpdateManagerList
import com.cometchat.utils.DateUtils
import com.cometchat.utils.EventBusOperation
import com.cometchat.utils.LogUtils
import com.google.gson.Gson
import com.squareup.picasso.Picasso


class HadConversationAdapter(private val context: Context, private val mList: MutableList<HadConversationItemCardDTO>, private var onItemClicked: ((event: EventBusResponseDto) -> Unit)) :
    RecyclerView.Adapter<HadConversationAdapter.ViewHolder>() {

    val TAG = "HadConversationAdapterNew"

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.items_had_conversation, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = mList[position]

        //for dp
        if (data.avatar.isNullOrEmpty()) { //url.isEmpty()
            Picasso.get()
                .load(R.drawable.ic_person)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.dp);

        } else {
            Picasso.get()
                .load(data.avatar)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.dp); //this is your ImageView
        }

        //for user name
        holder.name.text = data.name

        //for last message date time stamp
        holder.lastMessageTimeStamp.text = DateUtils.getFormattedTime(data.lastMessageDeliveredToMeAtStamp)

        //check last message is sent or received
        if (CometChat.getLoggedInUser().uid == data.lastMessageSenderId){
            //for delivery tick mark
            holder.sendMessageStatusReportIcon.visibility = View.VISIBLE
            if (data.lastMessageDeliveredAtTimeStamp == 0.toLong()){
                holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_sent)
            }else if(data.lastMessageDeliveredAtTimeStamp != 0.toLong() && data.lastMessageReadAtStamp == 0.toLong()){
                holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_delivered)
            }else if(data.lastMessageDeliveredAtTimeStamp != 0.toLong() && data.lastMessageReadAtStamp != 0.toLong()){
                holder.sendMessageStatusReportIcon.setImageResource(R.drawable.ic_check_read)
            }
        }else{
            //for delivery tick mark
            holder.sendMessageStatusReportIcon.visibility = View.GONE
        }

        //for unread message count
        if(data.unreadMessageCount == 0){
            holder.unreadMessageBackground.visibility = View.GONE
            holder.unreadMessageCount.visibility = View.GONE
        }else{
            holder.unreadMessageBackground.visibility = View.VISIBLE
            holder.unreadMessageCount.visibility = View.VISIBLE
            holder.unreadMessageCount.text = data.unreadMessageCount.toString()
        }

        //for online
        if(data.isOnline || data.isTyping){
            holder.groupSentMessage.visibility = View.GONE
            holder.sendMessageStatusReportIcon.visibility = View.GONE
            holder.sendMessageFileIcon.visibility = View.GONE
            holder.sendMessageText.visibility = View.GONE
            holder.groupOnlineAndTyping.visibility = View.VISIBLE
            if(data.isTyping){
                holder.onlineAndTypingStatus.text = "Typing..."
            }else if(data.isOnline){
                holder.onlineAndTypingStatus.text = "Online"
            }
        }else{
            holder.groupSentMessage.visibility = View.VISIBLE
            holder.sendMessageText.visibility = View.VISIBLE
            holder.groupOnlineAndTyping.visibility = View.GONE

            //for text message and message type icon
            holder.sendMessageText.text = data.lastMessageText
            LogUtils.info(TAG, "data.lastMessageType ===>>>${data.lastMessageType} ---- data.lastMessageText: ${data.lastMessageText}")
            when (data.lastMessageType) {
                "text" -> {
                    holder.sendMessageFileIcon.visibility = View.GONE
                }
                "Location" -> {
                    holder.sendMessageFileIcon.visibility = View.VISIBLE
                    holder.sendMessageFileIcon.setImageResource(R.drawable.ic_share_location)
                    holder.sendMessageText.text = "Location"
                }
                "extension_poll" -> {
                    holder.sendMessageFileIcon.visibility = View.VISIBLE
                    holder.sendMessageFileIcon.setImageResource(R.drawable.ic_poll_blue)
                    holder.sendMessageText.text = "Poll"
                }
                "audio" -> {
                    holder.sendMessageFileIcon.visibility = View.VISIBLE
                    holder.sendMessageFileIcon.setImageResource(R.drawable.ic_voice_call)
                    holder.sendMessageFileIcon.setColorFilter(R.color.colorPurple)
                    holder.sendMessageText.text = "Audio call"
                }
                "video" -> {
                    holder.sendMessageFileIcon.visibility = View.VISIBLE
                    holder.sendMessageFileIcon.setImageResource(R.drawable.ic_video_call)
                    holder.sendMessageFileIcon.setColorFilter(R.color.colorPurple)
                    holder.sendMessageText.text = "Video call"
                }
                else -> {
                    holder.sendMessageFileIcon.visibility = View.VISIBLE
                }
            }
        }

        //for typing
        if(data.isTyping){
            holder.groupSentMessage.visibility = View.GONE
            holder.groupOnlineAndTyping.visibility = View.VISIBLE
            holder.onlineAndTypingStatus.text = "Typing..."
        }

        //for open chat
        holder.cardChat.setOnClickListener {
            val intent = Intent(context, ConversationLogActivity::class.java)
            intent.putExtra("obj", Gson().toJson(data))
            intent.putExtra("conversationType", data.conversationType)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        holder.cardChat.setOnLongClickListener {
            LogUtils.info(TAG, "=====>>>>>>>>> long press")
            onItemClicked(
                EventBusResponseDto(
                    EventBusOperation.DELETE_CONVERSATION,
                    position,
                    null,
                    data.uid,
                    data.conversationId,
                    holder.cardChat
                )
            )
            true
        }

        val objectRef = RealTimeUpdateForHadConversationItemCardDTO(
            data,
            holder
        )

        if (!realTimeUpdateManagerList.contains(objectRef)){
            realTimeUpdateManagerList.add(objectRef)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val dp: ImageView = itemView.findViewById(R.id.display_pic_img)
        val name: TextView = itemView.findViewById(R.id.display_name_tv)
        val lastMessageTimeStamp: TextView = itemView.findViewById(R.id.display_time)
        val unreadMessageBackground: ImageView = itemView.findViewById(R.id.iv_unread_message_counter)
        val unreadMessageCount: TextView = itemView.findViewById(R.id.tv_unread_message_counter)
        val groupSentMessage: ConstraintLayout = itemView.findViewById(R.id.group_sent_message)
        val sendMessageStatusReportIcon: ImageView = itemView.findViewById(R.id.display_message_status)
        val sendMessageFileIcon: ImageView = itemView.findViewById(R.id.display_file_type_ic)
        val sendMessageText: TextView = itemView.findViewById(R.id.tv_group_sent_message)
        val groupOnlineAndTyping: ConstraintLayout = itemView.findViewById(R.id.group_online_typing_status)
        val onlineAndTypingStatus: TextView = itemView.findViewById(R.id.tv_online_typing_status)
        val cardChat: ConstraintLayout = itemView.findViewById(R.id.cardChat)
    }

}
