package com.cometchat.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.dto.GroupMentionedUserDto
import com.cometchat.pro.models.GroupMember
import com.cometchat.ui.activities.conversation.ConversationLogActivity.Companion.edittextChatLog
import com.cometchat.ui.activities.conversation.ConversationLogActivity.Companion.groupMentionedUserListForMetaData
import com.cometchat.ui.activities.conversation.ConversationLogActivity.Companion.tempGroupMentionedUserList
import com.cometchat.utils.LogUtils
import com.google.gson.Gson
import java.io.InputStream
import java.net.URL
import java.util.ArrayList


class GroupMemberMentionedAdapter(private val context: Context, private val mList: MutableList<GroupMember>) : RecyclerView.Adapter<GroupMemberMentionedAdapter.ViewHolder>() {
    private val TAG = "GroupMemberMentionedAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_user_suggestions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvGroupUserName.text = mList[position].name

        holder.groupMember.setOnClickListener {
            val text = "${edittextChatLog.text}${mList[position].name}"
            if(edittextChatLog.text!!.contains("@")){
                val list = textGroupMentioned(text)
                var tempText = ""
                for(i in 0 until list.size){
                    tempText = "$tempText ${list[i]}"
                }
                edittextChatLog.setText(Html.fromHtml(tempText))
            }else{
                edittextChatLog.setText(text)
            }
            edittextChatLog.setSelection(edittextChatLog.length())//placing cursor at the end of the text
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val tvGroupUserName: TextView = itemView.findViewById(R.id.tv_group_user_name)
        val groupUserDP: ImageView = itemView.findViewById(R.id.display_pic_img)
        val groupMember: ConstraintLayout = itemView.findViewById(R.id.view_group_member)
    }

    private fun loadImageFromWebOperations(url: String?): Drawable? {
        return try {
            val inputStream: InputStream = URL(url).content as InputStream
            Drawable.createFromStream(inputStream, "src name")
        } catch (e: Exception) {
            null
        }
    }

    private fun textGroupMentioned(text: String): ArrayList<String> {
        val finalTextArray = arrayListOf<String>()
        val txt1 = text.split(" ".toRegex()).toTypedArray()
        groupMentionedUserListForMetaData.clear()
        txt1.forEach {
            if(Gson().toJson(tempGroupMentionedUserList).contains(it.removePrefix("@"), false)   ){
                val tempUserName = tempGroupMentionedUserList.filter { _userInfo ->
                    _userInfo.name == it.removePrefix("@")
                }
                if (tempUserName.isNotEmpty()){
                    val text = "<b><font color=#55C364>@${it.removePrefix("@")}</font></b>"
                    finalTextArray.add(text)
                    groupMentionedUserListForMetaData.add(
                        GroupMentionedUserDto(
                            tempUserName[0].uid,
                            tempUserName[0].name
                        )
                    )
                }else{
                    finalTextArray.add(it)
                }
            }else{
                LogUtils.info(TAG, "==========>>>>>:::: 8: ${txt1.size}")
                finalTextArray.add(it)
            }
        }
        return finalTextArray
    }
}