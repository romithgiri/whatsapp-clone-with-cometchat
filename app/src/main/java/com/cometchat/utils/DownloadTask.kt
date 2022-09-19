package com.cometchat.utils

import android.content.Context
import android.util.Log
import android.view.View
import com.cometchat.dto.EventBusResponseDto
import com.cometchat.pro.models.Attachment
import com.cometchat.utils.FilePathProviderUtils.appStorageDevicePath
import com.cometchat.utils.FilePathProviderUtils.intentFileOpener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL


object DownloadTask {
    private val TAG = "DownloadTask"

    fun download(
        context: Context,
        position: Int,
        attachment: Attachment?,
        folderName: String,
        view: View,
        eventBusOperation: EventBusOperation
    ){
        try {
            GlobalScope.launch {
                LogUtils.info(TAG, "File downloaded to path >>>>>>>>>>>: 1.1: ${"${appStorageDevicePath(folderName)}/${attachment?.fileName}"}")
                LogUtils.info(TAG, "File downloaded to path >>>>>>>>>>>: 1.2: ${File("${appStorageDevicePath(folderName)}/${attachment?.fileName}").exists()}")
                val clickedFile = File("${appStorageDevicePath(folderName)}/${attachment?.fileName}")
                if (clickedFile.exists()){
                    intentFileOpener(context, clickedFile)
                }else{
                    File("${appStorageDevicePath(folderName)}/").mkdir()
                    LogUtils.info(TAG, "File downloaded to path >>>>>>>>>>>: 1.3: ${File("${appStorageDevicePath(folderName)}/${attachment?.fileName}").exists()}")
                    BufferedInputStream(URL(attachment?.fileUrl).openStream()).use { `in` ->
                        FileOutputStream("${"${appStorageDevicePath(folderName)}/${attachment?.fileName}"}").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                            LogUtils.info(TAG, "File downloaded to path >>>>>>>>>>>: 1.4: ${"${appStorageDevicePath(folderName)}/${attachment?.fileName}"}")
                        }
                    }
                }
                EventBus.getDefault().post(
                 EventBusResponseDto(
                     eventBusOperation,
                     position,
                     0,
                     null,
                     null,
                     view
                 ))
                LogUtils.info(TAG, "File downloaded to path >>>>>>>>>>>: 1.6: ${File("${appStorageDevicePath(folderName)}/${attachment?.fileName}").exists()}")
            }
        } catch (e: Exception) {
            // handle exception
            LogUtils.error(TAG, "File downloaded to path >>>>>>>>>>>: ${"${appStorageDevicePath(folderName)}/${attachment?.fileName}"}")
        }
    }


}