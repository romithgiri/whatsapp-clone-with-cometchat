package com.cometchat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.R
import com.cometchat.dto.PollResultDataDto
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.utils.LogUtils
import com.cometchat.utils.Utils
import org.json.JSONObject
import kotlin.math.roundToInt


class PollResultAdapter(private val context: Context, private val loginUserID: String, private val pollId: String, private val optionsList: MutableList<PollResultDataDto>) : RecyclerView.Adapter<PollResultAdapter.ViewHolder>() {
    private val TAG = "PollOptionsAdapter"

    interface OnOptionDeleteListener {
        fun onDeletePollOptionClick(position: Int)
    }

    private var onOptionDeleteListener: OnOptionDeleteListener? = null

    fun setOnOptionDeleteListener(listener: OnOptionDeleteListener) {
        onOptionDeleteListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.poll_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var percentage = 0.0
        if(getTotal() != 0) {
            percentage = ((optionsList[position].count!!.toDouble() * 100) / getTotal())
        }
        holder.pollResultPercentage.text = "${percentage.roundToInt()}%"
        //holder.pollResultSeekBar.progress = percentage.roundToInt()
        //holder.pollResultSeekBar.setOnTouchListener { view, motionEvent -> true }
        //holder.pollResultSeekBar.refreshDrawableState();
        val getVoterObject = optionsList[position].voters
        if (getVoterObject != null){
            val isYouVoted = getVoterObject.getJSONObject("nameValuePairs").has(loginUserID)
            if (isYouVoted){
                holder.pollResultOptionCard.setBackgroundResource(R.drawable.poll_option_selected)
            }else{
                holder.pollResultOptionCard.setBackgroundResource(R.drawable.poll_option_not_selected)
            }
        }

        holder.pollResultOption.text = optionsList[position].text
        holder.pollResultOptionCard.setOnClickListener {
            val body = JSONObject()
            body.put("vote", position+1)
            body.put("id", pollId)
            CometChat.callExtension("polls", "POST", "/v2/vote", body,
                object : CallbackListener<JSONObject?>() {
                    override fun onSuccess(jsonObject: JSONObject?) {
                        // Voted successfully
                        LogUtils.info(TAG, "Poll response================>>>>>>>>: $jsonObject")
                        Utils.showToast(context, "Response updated")
                    }
                    override fun onError(e: CometChatException) {
                        LogUtils.error(TAG, "Error: unable to update your poll request")
                    }
                })
        }
    }

    override fun getItemCount(): Int {
        return optionsList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val pollResultOptionCard: ConstraintLayout = itemView.findViewById(R.id.poll_result_option_card)
        //val pollResultSeekBar: SeekBar = itemView.findViewById(R.id.poll_result_seek_bar)
        val pollResultOption: TextView = itemView.findViewById(R.id.poll_result_tv_option)
        val pollResultPercentage: TextView = itemView.findViewById(R.id.poll_result_tv_percent)
    }

    private fun getTotal(): Int {
        // calculate total
        var total = 0
        for(i in optionsList){
            total += i.count!!
        }
        return total
    }
}