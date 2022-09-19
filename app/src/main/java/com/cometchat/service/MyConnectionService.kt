package com.cometchat.service

import android.net.Uri
import android.os.Build
import android.provider.CallLog.Calls.PRESENTATION_ALLOWED
import android.telecom.*
import android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
import android.view.Surface
import androidx.annotation.RequiresApi
import com.cometchat.pro.core.Call
import com.cometchat.utils.AppConstant
import com.cometchat.utils.LogUtils


@RequiresApi(api = Build.VERSION_CODES.M)
class MyConnectionService : ConnectionService() {
    private val TAG = "MyConnectionService"

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 8")

        val bundle = request.extras

        val sessionID = bundle.getString(AppConstant.IntentStrings.SESSION_ID)
        val name = bundle.getString(AppConstant.IntentStrings.NAME)
        val type = bundle.getString(AppConstant.IntentStrings.TYPE)
        val callType = bundle.getString(AppConstant.IntentStrings.CALL_TYPE)
        val receiverUID = bundle.getString(AppConstant.IntentStrings.ID)

        val call = Call(receiverUID!!, type, callType)

        call.sessionId = sessionID

        LogUtils.info("CallConnectionService", "onCreateIncomingConnectionCall:$call")
        LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 9: sessionID: $sessionID")

        conn = CallConnection(this, call)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            conn.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        }
        conn.setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
        conn.setAddress(request.address, PRESENTATION_ALLOWED)
        conn.setInitializing()
        conn.videoProvider = object : Connection.VideoProvider() {
            override fun onSetCamera(cameraId: String?) {}
            override fun onSetPreviewSurface(surface: Surface?) {}
            override fun onSetDisplaySurface(surface: Surface?) {}
            override fun onSetDeviceOrientation(rotation: Int) {}
            override fun onSetZoom(value: Float) {}
            override fun onSendSessionModifyRequest(fromProfile: VideoProfile?, toProfile: VideoProfile?) {}
            override fun onSendSessionModifyResponse(responseProfile: VideoProfile?) {}
            override fun onRequestCameraCapabilities() {}
            override fun onRequestConnectionDataUsage() {}
            override fun onSetPauseImage(uri: Uri?) {}
        }
        conn.setActive()
        return conn
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
        LogUtils.info(TAG, "Firebase FCM JSONObject ===>>>>: call: 10: ${connectionManagerPhoneAccount.toString()}")

        LogUtils.info("onIncomingFailed:", connectionManagerPhoneAccount.toString())
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        return super.onCreateOutgoingConnection(connectionManagerPhoneAccount, request)
    }

    companion object {
        lateinit var conn: CallConnection
    }
}