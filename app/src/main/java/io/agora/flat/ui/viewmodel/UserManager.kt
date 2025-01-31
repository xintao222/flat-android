package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.data.model.RTMUserProp
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.di.interfaces.RtcApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ActivityRetainedScoped
class UserManager @Inject constructor(
    private val roomConfigRepository: RoomConfigRepository,
    private val userQuery: UserQuery,
    private val rtcApi: RtcApi,
) {
    private var _users = MutableStateFlow<List<RtcUser>>(emptyList())
    val users = _users.asStateFlow()

    private var _currentUser = MutableStateFlow<RtcUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private var creator: RtcUser? = null
    private var speakingJoiners: MutableList<RtcUser> = mutableListOf()
    private var handRaisingJoiners: MutableList<RtcUser> = mutableListOf()
    private var otherJoiners: MutableList<RtcUser> = mutableListOf()

    // current all users
    private var usersCache = mutableMapOf<String, RtcUser>()

    private lateinit var roomUUID: String
    private lateinit var userUUID: String
    private lateinit var ownerUUID: String
    private lateinit var scope: CoroutineScope

    fun reset(roomUUID: String, userUUID: String, ownerUUID: String, scope: CoroutineScope) {
        this.roomUUID = roomUUID
        this.userUUID = userUUID
        this.ownerUUID = ownerUUID
        this.scope = scope

        creator = null
        speakingJoiners.clear()
        handRaisingJoiners.clear()
        otherJoiners.clear()
        notifyUsers()
    }

    suspend fun init(uuids: List<String>): Boolean = suspendCoroutine { cont ->
        scope.launch {
            val userMap = userQuery.loadUsers(uuids).mapValues {
                RtcUser(
                    userUUID = it.value.userUUID,
                    name = it.value.name,
                    avatarURL = it.value.avatarURL,
                    rtcUID = it.value.rtcUID,
                )
            }
            userMap[userUUID]?.run {
                val config = roomConfigRepository.getRoomConfig(roomUUID)
                this.audioOpen = config?.enableAudio ?: false
                this.videoOpen = config?.enableVideo ?: false
            }
            usersCache.putAll(userMap)
            sortAndNotify(userMap.values.toList())
            cont.resume(true)
        }
    }

    private fun sortAndNotify(users: List<RtcUser>) {
        users.forEach(this::sortUser)
        notifyUsers()
    }

    private fun sortUser(rtcUser: RtcUser) {
        if (rtcUser.userUUID == userUUID) {
            _currentUser.value = rtcUser.copy()
        }

        speakingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
        handRaisingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
        otherJoiners.removeAll { it.userUUID == rtcUser.userUUID }

        when {
            rtcUser.userUUID == ownerUUID -> {
                creator = rtcUser
            }
            rtcUser.isSpeak -> {
                speakingJoiners.add(rtcUser)
            }
            rtcUser.isRaiseHand -> {
                handRaisingJoiners.add(rtcUser)
            }
            else -> {
                otherJoiners.add(rtcUser)
            }
        }
    }

    fun addUser(userUUID: String) {
        scope.launch {
            if (usersCache[userUUID] == null) {
                usersCache[userUUID] = RtcUser(userUUID = userUUID)
            }
            userQuery.loadUser(userUUID)?.let {
                val rtcUser = usersCache[userUUID]!!
                updateUser(rtcUser.copy(rtcUID = it.rtcUID, name = it.name, avatarURL = it.avatarURL))
            }
        }
    }

    fun removeUser(userUUID: String) {
        scope.launch {
            usersCache.remove(userUUID)
            if (userUUID == ownerUUID) {
                creator = null
            }
            speakingJoiners.removeAll { it.userUUID == userUUID }
            handRaisingJoiners.removeAll { it.userUUID == userUUID }
            otherJoiners.removeAll { it.userUUID == userUUID }
            notifyUsers()
        }
    }

    private fun updateUser(user: RtcUser) {
        usersCache[user.userUUID] = user
        sortUser(user)
        notifyUsers()
    }

    private fun notifyUsers() {
        val ranked = mutableListOf<RtcUser>()
        ranked += speakingJoiners
        ranked += handRaisingJoiners
        creator?.run {
            ranked += this
        }
        ranked += otherJoiners

        _users.value = ranked
    }

    fun findFirstOtherUser(): RtcUser? {
        return users.value.find { it.userUUID != userUUID }
    }

    fun findFirstUser(uuid: String): RtcUser? {
        return users.value.find { it.userUUID == uuid }
    }

    fun username(uuid: String): String {
        return findUser(uuid)?.name ?: ""
    }

    fun cancelHandRaising() {
        scope.launch {
            val updateUsers = handRaisingJoiners.toMutableList()
            handRaisingJoiners.clear()
            updateUsers.forEach { it.isRaiseHand = false }
            sortAndNotify(updateUsers)
        }
    }

    fun updateDeviceState(uuid: String, videoOpen: Boolean, audioOpen: Boolean) {
        scope.launch {
            findUser(uuid)?.apply {
                updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen))
                updateRtcStream(rtcUID, audioOpen, videoOpen)
            }
        }
    }

    fun updateUserState(uuid: String, audioOpen: Boolean, videoOpen: Boolean, name: String, isSpeak: Boolean) {
        scope.launch {
            findUser(uuid)?.run {
                updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen, name = name, isSpeak = isSpeak))
            }
        }
    }

    fun updateSpeakStatus(uuid: String, isSpeak: Boolean) {
        scope.launch {
            findUser(uuid)?.run {
                updateUser(copy(isSpeak = isSpeak))
            }
        }
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        scope.launch {
            findUser(uuid)?.run {
                updateUser(copy(isRaiseHand = isRaiseHand))
            }
        }
    }

    fun updateSpeakAndRaise(uuid: String, isSpeak: Boolean, isRaiseHand: Boolean) {
        scope.launch {
            findUser(uuid)?.run {
                updateUser(copy(isSpeak = isSpeak, isRaiseHand = isRaiseHand))
            }
        }
    }

    fun updateUserStates(uStates: HashMap<String, String>) {
        val updateUsers = users.value.toMutableList().map { rtcUser ->
            // defaultFlag for no flags
            val s = uStates[rtcUser.userUUID] ?: ""
            rtcUser.copy(
                videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true),
                audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true),
                isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true),
                isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true)
            )
        }
        updateUsers.forEach {
            updateRtcStream(it.rtcUID, it.audioOpen, it.videoOpen)
        }
        sortAndNotify(updateUsers)
    }

    private fun findUser(uuid: String): RtcUser? {
        return usersCache[uuid]
    }

    private fun updateRtcStream(rtcUID: Int, audioOpen: Boolean, videoOpen: Boolean) {
        rtcApi.rtcEngine().muteRemoteAudioStream(rtcUID, !audioOpen)
        rtcApi.rtcEngine().muteRemoteVideoStream(rtcUID, !videoOpen)
    }
}