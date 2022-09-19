package com.cometchat.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.dto.SelectedUsersForGroupConversationModel
import com.cometchat.ui.activities.BlockedUsers.Companion.unblockTempList
import com.squareup.picasso.Picasso


class SelectedUsersForUnblockAdapter(
    private val context: Context,
    private val modelList: MutableList<SelectedUsersForGroupConversationModel>
) : RecyclerView.Adapter<SelectedUsersForUnblockAdapter.ViewHolder>() {
    private val TAG = "SelectedUsersForUnblockAdapter"

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.had_conversation_card, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: SelectedUsersForGroupConversationModel = modelList[position]
        holder.textView.text = model.name
        if (model.avatar.isNullOrEmpty()) { //url.isEmpty()
            Picasso.get()
                .load(R.drawable.dp)
                .placeholder(R.drawable.dp)
                .error(R.drawable.dp)
                .into(holder.imageview);

        } else {
            Picasso.get()
                .load(model.avatar)
                .placeholder(R.drawable.dp)
                .error(R.drawable.dp)
                .into(holder.imageview); //this is your ImageView
        }
        holder.view.setBackgroundColor(if (model.isSelected) Color.GRAY else Color.TRANSPARENT)
        holder.view.setOnClickListener {
            model.isSelected = !model.isSelected
            holder.view.setBackgroundColor(if (model.isSelected) {
                var tempIndex = -1
                if (!unblockTempList.isNullOrEmpty()) {
                    for (i in 0 until unblockTempList.size){
                        if(model.uid == unblockTempList[i].uid){
                            tempIndex = i
                            break
                        }
                    }
                    if (tempIndex == -1){
                        unblockTempList.add(model)
                    }
                } else {
                    unblockTempList.add(model)
                }
                Color.GRAY
            } else {
                var tempIndex = -1
                if (!unblockTempList.isNullOrEmpty()) {
                    for (i in 0 until unblockTempList.size){
                        if(model.uid == unblockTempList[i].uid){
                            tempIndex = i
                            break
                        }
                    }
                    if (tempIndex != -1){
                        unblockTempList.removeAt(tempIndex)
                    }
                }
                Color.TRANSPARENT
            })
        }
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageview: ImageView = itemView.findViewById(R.id.imageview)
        val view: ConstraintLayout = itemView.findViewById(R.id.card_chat)
    }

}