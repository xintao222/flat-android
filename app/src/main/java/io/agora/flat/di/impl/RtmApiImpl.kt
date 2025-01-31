package io.agora.flat.di.impl

import android.content.Context
import android.util.Log
import io.agora.flat.Constants
import io.agora.flat.common.rtm.EmptyRtmChannelListener
import io.agora.flat.common.rtm.EmptyRtmClientListener
import io.agora.flat.common.rtm.RTMListener
import io.agora.flat.common.toFlatException
import io.agora.flat.data.Success
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.data.model.RtmQueryMessage
import io.agora.flat.data.repository.MessageRepository
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtm.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class RtmApiImpl @Inject constructor(private val messageRepository: MessageRepository) : RtmApi, StartupInitializer {
    companion object {
        val TAG = RtmApiImpl::class.simpleName

        const val CONNECTION_STATE_DISCONNECTED = 1
        const val CONNECTION_STATE_CONNECTING = 2
        const val CONNECTION_STATE_CONNECTED = 3
        const val CONNECTION_STATE_RECONNECTING = 4
        const val CONNECTION_STATE_ABORTED = 5

        const val CONNECTION_CHANGE_REASON_LOGIN = 1
        const val CONNECTION_CHANGE_REASON_LOGIN_SUCCESS = 2
        const val CONNECTION_CHANGE_REASON_LOGIN_FAILURE = 3
        const val CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT = 4
        const val CONNECTION_CHANGE_REASON_INTERRUPTED = 5
        const val CONNECTION_CHANGE_REASON_LOGOUT = 6
        const val CONNECTION_CHANGE_REASON_BANNED_BY_SERVER = 7
        const val CONNECTION_CHANGE_REASON_REMOTE_LOGIN = 8
    }

    private lateinit var rtmClient: RtmClient
    private val rtmClientListener = object : EmptyRtmClientListener {
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Connection state changes to $state reason:$reason")
            if (reason == CONNECTION_CHANGE_REASON_REMOTE_LOGIN) {
                rtmListeners.forEach { it.onRemoteLogin() }
            }
        }

        override fun onMessageReceived(message: RtmMessage, peerId: String) {
            Log.d(TAG, "Message received from $peerId ${message.text}")
            val event = RTMEvent.parseRTMEvent(message.text)
            if (channelCommandID == event.r) {
                rtmListeners.forEach { it.onRTMEvent(event, peerId) }
            }
        }
    }

    override fun init(context: Context) {
        try {
            rtmClient = RtmClient.createInstance(context, Constants.AGORA_APP_ID, rtmClientListener)
        } catch (e: Exception) {
            Log.w(TAG, "RTM SDK init fatal error!")
        }
    }

    private var messageListener = object : EmptyRtmChannelListener {
        override fun onMemberJoined(member: RtmChannelMember) {
            super.onMemberJoined(member)
            rtmListeners.forEach { it.onMemberJoined(member.userId, member.channelId) }
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            super.onMemberLeft(member)
            rtmListeners.forEach { it.onMemberLeft(member.userId, member.channelId) }
        }

        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            super.onMessageReceived(message, member)
            rtmListeners.forEach { it.onRTMEvent(RTMEvent.parseRTMEvent(message.text), member.userId) }
        }
    }

    private var commandListener = object : EmptyRtmChannelListener {
        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            super.onMessageReceived(message, member)
            rtmListeners.forEach { it.onRTMEvent(RTMEvent.parseRTMEvent(message.text), member.userId) }
        }
    }

    override fun rtmEngine(): RtmClient {
        return rtmClient
    }

    override suspend fun initChannel(rtmToken: String, channelId: String, userUUID: String): Boolean {
        channelMessageID = channelId
        channelCommandID = channelId + "commands"

        login(rtmToken, userUUID)
        channelMessage = joinChannel(channelMessageID, messageListener)
        channelCommand = joinChannel(channelCommandID, commandListener)

        return true
    }

    private suspend fun login(token: String, userId: String): Boolean = suspendCoroutine { cont ->
        rtmClient.login(token, userId, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(e.toFlatException())
            }
        })
    }

    override suspend fun logout(): Boolean = suspendCoroutine { cont ->
        rtmClient.logout(object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(e.toFlatException())
            }
        })
    }

    private lateinit var channelMessageID: String
    private lateinit var channelCommandID: String
    private var channelMessage: RtmChannel? = null
    private var channelCommand: RtmChannel? = null

    private suspend fun joinChannel(channelId: String, listener: RtmChannelListener): RtmChannel = suspendCoroutine {
        val channel = rtmClient.createChannel(channelId, listener)
        channel.join(object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                Log.d(TAG, "join onSuccess")
                it.resume(channel)
            }

            override fun onFailure(e: ErrorInfo) {
                Log.d(TAG, "join onFailure")
                it.resumeWithException(e.toFlatException())
            }
        })
    }

    override suspend fun getMembers(): List<RtmChannelMember> = suspendCoroutine { cont ->
        channelMessage?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(members: List<RtmChannelMember>) {
                Log.d(TAG, "member $members")
                cont.resume(members)
            }

            override fun onFailure(e: ErrorInfo) {
                Log.d(TAG, "onFailure $e")
                cont.resume(listOf())
            }
        }) ?: cont.resume(listOf())
    }

    private var sendMessageOptions = SendMessageOptions().apply {
        enableHistoricalMessaging = true
    }

    override suspend fun sendChannelMessage(msg: String): Boolean = suspendCoroutine { cont ->
        val message = rtmClient.createMessage()
        message.text = msg

        channelMessage?.sendMessage(message, sendMessageOptions, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(errorIn: ErrorInfo) {
                cont.resume(false)
            }
        }) ?: cont.resume(false)
    }

    override suspend fun sendChannelCommand(event: RTMEvent) = suspendCoroutine<Boolean> { cont ->
        run {
            val message = rtmClient.createMessage()
            message.text = RTMEvent.toText(event)

            channelCommand?.sendMessage(message, sendMessageOptions, object : ResultCallback<Void?> {
                override fun onSuccess(v: Void?) {
                    cont.resume(true)
                }

                override fun onFailure(error: ErrorInfo) {
                    cont.resume(false)
                }
            }) ?: cont.resume(false)
        }
    }

    override suspend fun sendPeerCommand(event: RTMEvent, peerId: String) = suspendCoroutine<Boolean> { cont ->
        // Peer command requires setting the channel id
        event.r = channelCommandID

        val message = rtmClient.createMessage()
        message.text = RTMEvent.toText(event)

        val option = SendMessageOptions().apply {
            enableOfflineMessaging = true
        }

        rtmClient.sendMessageToPeer(peerId, message, option, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(error: ErrorInfo) {
                cont.resume(false)
            }
        })
    }

    override suspend fun getTextHistory(
        channelId: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
        order: String,
    ): List<RtmQueryMessage> {
        val result = messageRepository.queryHistoryHandle(channelId, startTime, endTime, limit, offset, order)
        if (result is Success) {
            repeat(3) {
                delay(2000)
                val messageResult = messageRepository.getMessageList(handle = result.data)
                if (messageResult is Success) {
                    return messageResult.data
                }
            }
        }
        return listOf()
    }

    override suspend fun getTextHistoryCount(channelId: String, startTime: Long, endTime: Long): Int {
        repeat(3) {
            delay(2000)
            val result = messageRepository.getMessageCount(channelId, startTime, endTime)
            if (result is Success) {
                return result.data
            }
        }
        return -1
    }

    private var rtmListeners = mutableListOf<RTMListener>()

    override fun addRtmListener(listener: RTMListener) {
        rtmListeners.add(listener)
    }

    override fun removeRtmListener(listener: RTMListener) {
        rtmListeners.remove(listener)
    }
}