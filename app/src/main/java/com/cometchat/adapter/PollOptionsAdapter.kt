package com.cometchat.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.ui.activities.conversation.CustomPollDialog.Companion.pollOptionList
import com.cometchat.utils.LogUtils


class PollOptionsAdapter(private val context: Context, private val optionsList: MutableList<String>) : RecyclerView.Adapter<PollOptionsAdapter.ViewHolder>() {
    private val TAG = "PollOptionsAdapter"

    interface PollOptionsAdapterListener {
        fun onDeletePollOptionClick(position: Int)
    }

    private var pollOptionsAdapterListener: PollOptionsAdapterListener? = null

    fun initPollOptionsAdapterListener(listener: PollOptionsAdapterListener) {
        pollOptionsAdapterListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.poll_options_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, cureentPosition: Int) {
        if (cureentPosition < 2){
            holder.ivDeleteOption.visibility = View.GONE
        }
        holder.etPollOption.hint = "Option ${cureentPosition+1}"

        holder.ivDeleteOption.setOnClickListener {
            pollOptionsAdapterListener?.onDeletePollOptionClick(cureentPosition)
        }

        holder.etPollOption.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                pollOptionList[holder.adapterPosition] = holder.etPollOption.text.toString()
            }
        })

    }

    override fun getItemCount(): Int {
        return optionsList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val etPollOption: EditText = itemView.findViewById(R.id.et_poll_option)
        val ivDeleteOption: ImageView = itemView.findViewById(R.id.iv_close_poll_dialog)
    }

}