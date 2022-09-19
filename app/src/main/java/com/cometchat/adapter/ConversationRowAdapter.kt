package com.cometchat.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.cometchat.R
import com.cometchat.dto.*
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.models.Attachment
import com.cometchat.pro.models.CustomMessage
import com.cometchat.ui.activities.ReceiverProfileViewActivity
import com.cometchat.utils.*
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*


class ConversationRowAdapter(
    private val context: Context,
    private var messageList: Deque<ConversationLogRowDto>
) : RecyclerView.Adapter<ConversationRowAdapter.MessageViewHolder>() {

    val TAG = "ConversationRowAdapter"

    private var blinkItem = NO_POSITION

    interface OnReplyClickListener {
        fun onRepliedCardClick(position: Int)
    }

    private var mOnReplyClickListener: OnReplyClickListener? = null

    fun setReplyClickListener(listener: OnReplyClickListener) {
        mOnReplyClickListener = listener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MessageViewHolder {
        val messageViewHolder = MessageViewHolder(LayoutInflater.from(context).inflate(viewType, viewGroup, false))
        messageViewHolder.messageReplyGroup.setOnClickListener {
            mOnReplyClickListener?.onRepliedCardClick(messageList.elementAt(messageViewHolder.adapterPosition).repliedMessagePosition)
        }
        return messageViewHolder
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val data = messageList.elementAt(position)

        if (data.textedBy == AppConstant.TextedBy.SENDER) {
            holder.messageMgsDeliveryReportCheckMark.visibility = View.VISIBLE
        } else if (data.textedBy == AppConstant.TextedBy.RECEIVER) {
            holder.messageMgsDeliveryReportCheckMark.visibility = View.GONE
        }

        /**by default onRecycler view adapter data patch set it as gone.
        other wise it will create issue for hide and show*/
        holder.messageMediaTypeImageGroupDownloadIcon.visibility = View.GONE

        /**by default hide messageReplyGroup.
        other wise it will create issue for hide and show*/
        holder.messageReplyGroup.visibility = View.GONE

        /**by default hide pollGroup.
        other wise it will create issue for hide and show*/
        holder.pollGroup.visibility = View.GONE

        manageUI(
            holder,
            context,
            data.text,
            data.chatID,
            data.avatar,
            data.timestamp,
            data.attachment,
            data.deliveredAt,
            data.readAt,
            data.conversationType,
            data.tempShareAnyData,
            data.metadata,
            position
        )
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList.elementAt(position).textedBy == AppConstant.TextedBy.SENDER)
            R.layout.send_message_row
        else
            R.layout.received_message_row
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageGroup: ConstraintLayout = itemView.findViewById(R.id.message_group)
        val messageDp: ImageView = itemView.findViewById(R.id.message_dp)

        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val messageMediaTypeImageGroup: ConstraintLayout = itemView.findViewById(R.id.message_media_type_image_group)
        val messageMediaTypeImageGroupImageHolder: ImageView = itemView.findViewById(R.id.message_media_type_image_group_image_holder)

        val messageMediaTypeImageGroupDownloadIcon: ImageView = itemView.findViewById(R.id.message_media_type_image_group_download_icon)

        val messageMediaTypeFileGroup: TextView = itemView.findViewById(R.id.message_media_type_file_group)

        val messageMgsDeliveryReportCheckMark: ImageView = itemView.findViewById(R.id.message_msg_delivery_report_check_mark)
        val messageMsgTime: TextView = itemView.findViewById(R.id.message_msg_time)

        val messageGoogleMapView: ConstraintLayout = itemView.findViewById(R.id.message_google_map_view)
        val googleMapWebView: WebView = itemView.findViewById(R.id.google_map_web_view)
        val googleMapTextView: TextView = itemView.findViewById(R.id.google_map_text)

        val messageReplyGroup: ConstraintLayout = itemView.findViewById(R.id.message_reply_group)
        val messageReplyGroupUserName: TextView = itemView.findViewById(R.id.message_reply_group_user_name)
        val messageReplyGroupUserMessage: TextView = itemView.findViewById(R.id.message_reply_group_user_message)

        val pollGroup: ConstraintLayout = itemView.findViewById(R.id.poll_view)
        val pollQuestion: TextView = itemView.findViewById(R.id.poll_question)
        val pollResultOptionsRecyclerView: RecyclerView = itemView.findViewById(R.id.poll_result_recyclerview)
        val pollVoteCount: TextView = itemView.findViewById(R.id.poll_totalCount)
    }

    fun blinkItem(position: Int) {
        blinkItem = position
        notifyItemChanged(position)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun manageUI(
        holder: MessageViewHolder,
        context: Context,
        text: String?,
        chatID: Int?,
        avatar: String?,
        timestamp: Long,
        attachment: Attachment?,
        deliveredAt: Long,
        readAt: Long,
        conversationType: String,
        tempShareAnyData: String?,
        metadata: JSONObject?,
        position: Int
    ) {

        //user dp ref
        val requestOptions = RequestOptions().placeholder(R.drawable.dp)
        Glide.with(holder.messageDp.context)
            .load(avatar)
            .thumbnail(0.1f)
            .apply(requestOptions)
            .into(holder.messageDp)

        //set message time
        LogUtils.info(TAG, "============>>timestamp::: txt: ${text} --- ${timestamp}")
        holder.messageMsgTime.text = DateUtils.getFormattedTimeChatLog(timestamp)

        //update msg delivery check mark status
        if (deliveredAt == 0.toLong() && readAt == 0.toLong()) {
            holder.messageMgsDeliveryReportCheckMark.setImageResource(R.drawable.ic_check_sent)
        } else if (deliveredAt != 0.toLong() && readAt == 0.toLong()) {
            holder.messageMgsDeliveryReportCheckMark.setImageResource(R.drawable.ic_check_delivered)
        } else if (deliveredAt != 0.toLong() && readAt != 0.toLong()) {
            holder.messageMgsDeliveryReportCheckMark.setImageResource(R.drawable.ic_check_read)
        } else {
            holder.messageMgsDeliveryReportCheckMark.setImageResource(R.drawable.ic_check_sent)
        }

        //pass call back EventBusOperation.NEW_MESSAGE_SENT
        EventBus.getDefault().post(
            EventBusResponseDto(
                EventBusOperation.NEW_MESSAGE_SENT,
                position,
                chatID,
                null,
                null,
                holder.messageMgsDeliveryReportCheckMark
            )
        )

        //handling all messages type groups
        if(text == null && attachment == null && !tempShareAnyData.isNullOrBlank() && metadata != null){
            val obj = Gson().fromJson(tempShareAnyData, CustomMessage::class.java)

            if(obj.type == "Location"){
                holder.messageGoogleMapView.visibility = View.VISIBLE
                holder.messageText.visibility = View.GONE
                holder.messageMediaTypeFileGroup.visibility = View.GONE
                holder.messageMediaTypeImageGroup.visibility = View.GONE

                val jsonObject = JSONObject(Gson().toJson(obj.customData))
                val objInStr = jsonObject.get("nameValuePairs").toString()
                val jsonObject1 = JSONObject(objInStr)
                val isHaveLatitude = jsonObject1.has("latitude")
                val isHaveLongitude = jsonObject1.has("longitude")
                if (isHaveLatitude && isHaveLongitude) {
                    val latitude = jsonObject1.get("latitude").toString()
                    val longitude = jsonObject1.get("longitude").toString()
                    LogUtils.info(TAG, "Lat&Long ======>>>>: 1: latitude: $latitude --- longitude: $longitude")
                    holder.googleMapWebView.webViewClient = WebViewClient()
                    holder.googleMapWebView.settings.javaScriptEnabled = true
                    holder.googleMapWebView.loadUrl("http://maps.google.com/maps?q=$latitude,$longitude")
                    holder.googleMapTextView.setOnClickListener {
                        val strUri = "http://maps.google.com/maps?q=loc:$latitude,$longitude (User current location)"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strUri))
                        intent.setClassName(
                            "com.google.android.apps.maps",
                            "com.google.android.maps.MapsActivity"
                        )
                        context.startActivity(intent)
                    }
                }
            }

            //for poll messages
            if(obj.type == "extension_poll"){
                holder.pollGroup.visibility = View.VISIBLE
                holder.messageText.visibility = View.GONE
                holder.messageMediaTypeImageGroup.visibility = View.GONE
                holder.messageMediaTypeFileGroup.visibility = View.GONE
                holder.messageGoogleMapView.visibility = View.GONE

                var pollResultList: MutableList<PollResultDataDto> = arrayListOf()
                val jsonObject = JSONObject(Gson().toJson(metadata))
                val jsonObjectInString = jsonObject.get("nameValuePairs").toString()
                val stringToJsonObject = JSONObject(jsonObjectInString)
                val isMetadataHavePoll = stringToJsonObject.has("@injected")
                LogUtils.info(TAG, "Poll ========>>>> type: ${obj.type}")
                LogUtils.info(TAG, "Poll ========>>>> metadata: ${jsonObject.get("nameValuePairs")}")
                LogUtils.info(TAG, "Poll ========>>>> isMetadataHavePoll: $isMetadataHavePoll")
                if (isMetadataHavePoll) {
                    pollResultList.clear()
                    val pollId = stringToJsonObject.getJSONObject("@injected")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("extensions")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("polls")
                        .getJSONObject("nameValuePairs")
                        .get("id")

                    val adapter = PollResultAdapter(context, CometChat.getLoggedInUser().uid, pollId.toString(), pollResultList)
                    holder.pollResultOptionsRecyclerView.layoutManager = LinearLayoutManager(context)
                    holder.pollResultOptionsRecyclerView.adapter = adapter

                    val pollQuestionObj = stringToJsonObject.getJSONObject("@injected")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("extensions")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("polls")
                        .getJSONObject("nameValuePairs")
                        .get("question")

                    val pollOptionsObj = stringToJsonObject.getJSONObject("@injected")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("extensions")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("polls")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("results")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("options")
                        .getJSONObject("nameValuePairs")

                    val pollVoteCountObj = stringToJsonObject.getJSONObject("@injected")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("extensions")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("polls")
                        .getJSONObject("nameValuePairs")
                        .getJSONObject("results")
                        .getJSONObject("nameValuePairs")
                        .get("total")

                    holder.pollQuestion.text = pollQuestionObj.toString()
                    holder.pollVoteCount.text = "Votes: $pollVoteCountObj"

                    val iter: Iterator<String> = pollOptionsObj.keys()
                    while (iter.hasNext()) {
                        val key = iter.next()
                        try {
                            val obj = pollOptionsObj.getJSONObject(key)
                            val obj2 = obj.getJSONObject("nameValuePairs")
                            val isVotersExist = obj2.has("voters")
                            var voters: JSONObject ?= null
                            if (isVotersExist){
                                voters = obj2.get("voters") as JSONObject?
                            }
                            pollResultList.add(
                                PollResultDataDto(
                                    obj2.get("text").toString(),
                                    obj2.get("count") as Int?,
                                    voters
                                )
                            )
                            adapter.notifyDataSetChanged()
                        } catch (e: JSONException) {
                            // Something went wrong!
                        }
                    }

                    LogUtils.info(TAG, "Poll ========>>>> pollQuestionObj: $pollQuestionObj --- pollOptionsObj: $pollOptionsObj --- pollVoteCountObj: $pollVoteCountObj")
                } else {
                    holder.messageReplyGroup.visibility = View.GONE
                }
            }
        } else if (text == null && attachment != null) {
            holder.messageText.visibility = View.GONE
            holder.messageGoogleMapView.visibility = View.GONE

            if (attachment.fileExtension.contains("png") || attachment.fileExtension.contains("jpg")) {
                holder.messageMediaTypeImageGroup.visibility = View.VISIBLE
                holder.messageMediaTypeFileGroup.visibility = View.GONE

                val receivedImageOptions = RequestOptions().placeholder(R.drawable.image_media_placeholder)
                Glide.with(holder.messageMediaTypeImageGroupImageHolder.context)
                    .load(attachment.fileUrl)
                    .thumbnail(0.1f)
                    .transform(CenterInside(), RoundedCorners(15))
                    .apply(receivedImageOptions)
                    .into(holder.messageMediaTypeImageGroupImageHolder)

                val clickedFile = File("${FilePathProviderUtils.appStorageDevicePath("CometChatImages")}/${attachment.fileName}")
                if (clickedFile.exists()) {
                    holder.messageMediaTypeImageGroupDownloadIcon.visibility = View.GONE
                }else{
                    holder.messageMediaTypeImageGroupDownloadIcon.visibility = View.VISIBLE
                }

                holder.messageMediaTypeImageGroupImageHolder.setOnClickListener {
                    if (clickedFile.exists()) {
                        FilePathProviderUtils.intentFileOpener(context, clickedFile)
                    } else {
                        DownloadTask.download(
                            context,
                            position,
                            attachment,
                            "CometChatImages",
                            holder.messageMediaTypeImageGroupDownloadIcon,
                            EventBusOperation.IMAGE_DOWNLOAD
                        )
                    }
                }
            } else {
                holder.messageMediaTypeImageGroup.visibility = View.GONE
                holder.messageMediaTypeFileGroup.visibility = View.VISIBLE

                holder.messageMediaTypeFileGroup.text = attachment.fileName
                val clickedFile = File("${FilePathProviderUtils.appStorageDevicePath("CometChatFile")}/${attachment.fileName}")
                if (clickedFile.exists()) {
                    holder.messageMediaTypeFileGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file, 0, 0, 0);
                }else{
                    holder.messageMediaTypeFileGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file, 0, R.drawable.ic_download, 0)
                }
                holder.messageMediaTypeFileGroup.setOnClickListener {
                    if (PermissionHelper.checkStoragePermissions(context as Activity)) {
                        if (clickedFile.exists()) {
                            FilePathProviderUtils.intentFileOpener(context, clickedFile)
                        } else {
                            DownloadTask.download(
                                context,
                                position,
                                attachment,
                                "CometChatFile",
                                holder.messageMediaTypeFileGroup,
                                EventBusOperation.FILE_DOWNLOAD
                            )
                        }
                    } else {
                        PermissionHelper.requestPermissionForStorage(
                            context,
                            PermissionHelper.storagePermissionCodeForFilePicker
                        )
                    }
                }
            }
        } else {
            holder.messageText.visibility = View.VISIBLE
            holder.messageMediaTypeFileGroup.visibility = View.GONE
            holder.messageMediaTypeImageGroup.visibility = View.GONE
            holder.messageGoogleMapView.visibility = View.GONE

            if (metadata != null) {
                val jsonObject = JSONObject(Gson().toJson(metadata))
                val isMetadataExist = jsonObject.has("nameValuePairs")
                LogUtils.info(TAG, "Poll ========>>>> metadata 0: isMetadataExist:$isMetadataExist ---- ${jsonObject.get("nameValuePairs").toString()}")

                if(isMetadataExist){
                    val jsonObjectInString = jsonObject.get("nameValuePairs").toString()
                    val stringToJsonObject = JSONObject(jsonObjectInString)

                    if (conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP) {
                        //for mention users in chat
                        val isMetadataHaveMentionedUser = stringToJsonObject.has("mentionedUser")
                        if (isMetadataHaveMentionedUser) {
                            val objInStr = stringToJsonObject.get("mentionedUser").toString()
                            val receivedObj = Gson().fromJson(
                                "{\"mentionedUser\": $objInStr}",
                                GroupMentionedUserMetadataDto::class.java
                            )
                            if (text != null) {
                                makeMenitionUsersAsClickable(text, receivedObj, holder.messageText)
                            }
                        }
                    }

                    //for replied messages
                    val isMetadataHaveRepliedOn = stringToJsonObject.has("repliedOn")
                    if (isMetadataHaveRepliedOn) {
                        val objInStr = stringToJsonObject.get("repliedOn").toString()
                        val receivedObj = Gson().fromJson("{$objInStr}", RepliedOnDto::class.java)
                        holder.messageReplyGroup.visibility = View.VISIBLE
                        holder.messageReplyGroupUserName.text = receivedObj.userName
                        holder.messageReplyGroupUserMessage.text = receivedObj.repliedOnText
                    } else {
                        holder.messageReplyGroup.visibility = View.GONE
                    }
                }else{
                    holder.messageText.text = text
                }
            } else {
                holder.messageText.text = text
            }
        }


        holder.messageGroup.setOnLongClickListener {
            EventBus.getDefault().post(
                EventBusResponseDto(
                    EventBusOperation.DELETE_MESSAGE,
                    position,
                    chatID!!,
                    null,
                    null,
                    holder.messageGroup
                )
            )
            true
        }

        holder.messageMediaTypeFileGroup.setOnLongClickListener {
            EventBus.getDefault().post(
                EventBusResponseDto(
                    EventBusOperation.DELETE_MESSAGE,
                    position,
                    chatID!!,
                    null,
                    null,
                    holder.messageGroup
                )
            )
            true
        }

        holder.messageMediaTypeImageGroupImageHolder.setOnLongClickListener {
            EventBus.getDefault().post(
                EventBusResponseDto(
                    EventBusOperation.DELETE_MESSAGE,
                    position,
                    chatID!!,
                    null,
                    null,
                    holder.messageGroup
                )
            )
            true
        }
    }

    private fun makeMenitionUsersAsClickable(
        text: String,
        receivedObj: GroupMentionedUserMetadataDto,
        textView: TextView
    ) {
        var characterCount = 0
        var characterCurrentCount = 0
        val finalTextArray = arrayListOf<String>()
        val spanIndexDtoArray = arrayListOf<SpanIndexDto>()
        val txt1 = text.split(" ".toRegex()).toTypedArray()

        txt1.forEach {
            characterCount += it.length + 1
            characterCurrentCount = it.length
            if (it.startsWith("@")) {
                if (Gson().toJson(receivedObj.mentionedUser)
                        .contains("${it.removePrefix("@")}", false)
                ) {
                    val tempText = "<b><font color=#55C364>$it</font></b>"
                    val list = receivedObj.mentionedUser.filter { _userMetadata ->
                        _userMetadata.userName == it.removePrefix("@")
                    }
                    if (list.isNotEmpty()) {
                        spanIndexDtoArray.add(
                            SpanIndexDto(
                                characterCount - characterCurrentCount - 1,
                                characterCount - 1,
                                receivedObj.mentionedUser.filter { _userMetadata ->
                                    _userMetadata.userName == it.removePrefix("@")
                                }[0]
                            )
                        )
                        finalTextArray.add(tempText)
                    } else {
                        finalTextArray.add(it)
                    }
                } else {
                    finalTextArray.add(it)
                }
            } else {
                finalTextArray.add(it)
            }
        }

        var tempText = ""
        for (i in 0 until finalTextArray.size) {
            tempText = "$tempText ${finalTextArray[i]}"
        }
        val myString = SpannableString(Html.fromHtml(tempText))

        spanIndexDtoArray.forEach {
            myString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(textView: View) {
                        //Toast.makeText(context, "UserId: ===>> ${it.metadataDto.userName}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, ReceiverProfileViewActivity::class.java)
                        intent.putExtra("uid", it.metadataDto.userID)
                        intent.putExtra(
                            "conversationType",
                            CometChatConstants.CONVERSATION_TYPE_USER
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                },
                it.start,
                it.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = myString
    }
}
