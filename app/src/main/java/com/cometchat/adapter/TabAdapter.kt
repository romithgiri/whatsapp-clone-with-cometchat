package com.cometchat.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.cometchat.ui.fragment.conversation.HadConversationFragment
import com.cometchat.ui.fragment.conversation.HadGroupConversationFragment
import com.cometchat.ui.fragment.conversation.StartNewConversationFragment

internal class TabAdapter(
    var context: Context,
    fm: FragmentManager,
    private var totalTabs: Int
) :
    FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                HadConversationFragment()
            }
            1 -> {
                HadGroupConversationFragment()
            }
            2 -> {
                StartNewConversationFragment()
            }
            else -> getItem(position)
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}