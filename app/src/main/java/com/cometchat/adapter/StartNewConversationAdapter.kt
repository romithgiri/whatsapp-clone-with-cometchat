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
import com.cometchat.pro.models.User
import com.cometchat.ui.activities.conversation.ConversationLogActivity
import com.google.gson.Gson
import com.squareup.picasso.Picasso


class StartNewConversationAdapter(private val context: Context, private val conversationType: String, private val mList: MutableList<User>) :
    RecyclerView.Adapter<StartNewConversationAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.had_conversation_card, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = mList[position]
        holder.textView.text = data.name
        if (data.avatar.isNullOrEmpty()) { //url.isEmpty()
            Picasso.get()
                .load(R.drawable.dp)
                .placeholder(R.drawable.dp)
                .error(R.drawable.dp)
                .into(holder.imageView);

        } else {
            Picasso.get()
                .load(data.avatar)
                .placeholder(R.drawable.dp)
                .error(R.drawable.dp)
                .into(holder.imageView); //this is your ImageView
        }
        holder.cardChat.setOnClickListener {
            val intent = Intent(context, ConversationLogActivity::class.java)
            intent.putExtra("obj", Gson().toJson(data))
            intent.putExtra("conversationType", conversationType)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val cardChat: ConstraintLayout = itemView.findViewById(R.id.card_chat)
    }

}
