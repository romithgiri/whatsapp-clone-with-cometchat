package com.cometchat.utils

import android.util.Log

object LogUtils {
    fun info(tag: String, msg: String){
        Log.i(tag, msg)
    }

    fun error(tag: String, msg: String){
        Log.e(tag, msg)
    }

}