package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.FlatException
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RTMUserProp
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ActivityRetainedScoped
class UserState @Inject constructor(
    private val roomRepository: RoomRepository,
    private val roomConfigRepository: RoomConfigRepository,
) {
    private var _users = MutableStateFlow<List<RtcUser>>(emptyList())
    val users = _users.asStateFlow()

    private var _currentUser = MutableStateFlow<RtcUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    // 缓存用户信息，降低web服务压力
    private var usersCacheMap: MutableMap<String, RtcUser> = mutableMapOf()
    private var creator: RtcUser? = null
    private var speakingJoiners: MutableList<RtcUser> = mutableListOf()
    private var handRaisingJoiners: MutableList<RtcUser> = mutableListOf()
    private var otherJoiners: MutableList<RtcUser> = mutableListOf()

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

    suspend fun initRoomUsers(uuids: List<String>): Boolean = suspendCoroutine { cont ->
        scope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, uuids)) {
                is Success -> {
                    result.data.forEach { (uuid, user) ->
                        run {
                            user.userUUID = uuid
                            if (user.userUUID == userUUID) {
                                val config = roomConfigRepository.getRoomConfig(roomUUID)
                                user.audioOpen = config?.enableAudio ?: false
                                user.videoOpen = config?.enableVideo ?: false
                            }
                        }
                    }
                    addToCache(result.data)
                    addToCurrent(result.data.values.toList())
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resumeWithException(FlatException(result.error.code, ""))
                }
            }
        }
    }

    private fun addToCache(userMap: Map<String, RtcUser>) {
        userMap.forEach { (uuid, user) -> usersCacheMap[uuid] = user.copy() }
    }

    private fun addToCurrent(userUUID: String) {
        usersCacheMap[userUUID]?.run {
            addToCurrent(listOf(this))
        }
    }

    private fun addToCurrent(userList: List<RtcUser>) {
        userList.forEach {
            if (it.userUUID == userUUID) {
                _currentUser.value = it
            }
            when {
                it.userUUID == ownerUUID -> {
                    creator = it
                }
                it.isSpeak -> {
                    val index = speakingJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        speakingJoiners.removeAt(index)
                    }
                    speakingJoiners.add(it)
                }
                it.isRaiseHand -> {
                    val index = handRaisingJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        handRaisingJoiners.removeAt(index)
                    }
                    handRaisingJoiners.add(it)
                }
                else -> {
                    val index = otherJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        otherJoiners.removeAt(index)
                    }
                    otherJoiners.add(it)
                }
            }
        }

        notifyUsers()
    }

    private fun hasCache(userUUID: String): Boolean {
        return usersCacheMap.containsKey(userUUID)
    }

    fun removeUser(userUUID: String, notify: Boolean = true) {
        if (userUUID == ownerUUID) {
            creator = null
        }
        speakingJoiners.removeAll { it.userUUID == userUUID }
        handRaisingJoiners.removeAll { it.userUUID == userUUID }
        otherJoiners.removeAll { it.userUUID == userUUID }

        if (notify) {
            notifyUsers()
        }
    }

    fun addUser(userUUID: String) {
        if (hasCache(userUUID)) {
            addToCurrent(userUUID)
            return
        }

        scope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, listOf(userUUID))) {
                is Success -> {
                    result.data.forEach { (uuid, user) -> user.userUUID = uuid }
                    addToCache(result.data)
                    addToCurrent(result.data.values.toList())
                }
                is ErrorResult -> {
                }
            }
        }
    }

    private fun updateUser(user: RtcUser, notify: Boolean = true) {
        removeUser(user.userUUID, false)
        addToCurrent(listOf(user))
        if (notify) {
            notifyUsers()
        }
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
        val list = handRaisingJoiners.toMutableList()
        handRaisingJoiners.clear()
        list.forEach { it.isRaiseHand = false }
        otherJoiners.addAll(list)

        notifyUsers()
    }

    fun updateDeviceState(uuid: String, videoOpen: Boolean, audioOpen: Boolean) {
        findUser(uuid)?.apply {
            updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen))
        }
    }

    fun updateUserState(uuid: String, audioOpen: Boolean, videoOpen: Boolean, name: String, isSpeak: Boolean) {
        findUser(uuid)?.apply {
            updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen, name = name, isSpeak = isSpeak))
        }
    }

    fun updateSpeakStatus(uuid: String, isSpeak: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isSpeak = isSpeak))
        }
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isRaiseHand = isRaiseHand))
        }
    }

    fun updateSpeakAndRaise(uuid: String, isSpeak: Boolean, isRaiseHand: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isSpeak = isSpeak, isRaiseHand = isRaiseHand))
        }
    }

    fun updateUserStates(uStates: HashMap<String, String>) {
        uStates.forEach { (userId, s) ->
            findUser(userId)?.copy(
                videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true),
                audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true),
                isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true),
                isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true),
            )?.also {
                updateUser(it, false)
            }
        }
        notifyUsers()
    }

    private fun findUser(uuid: String): RtcUser? {
        return users.value.find { it.userUUID == uuid }
    }

    fun createMessageUsersObserver(): Flow<List<RtcUser>> {
        return users
    }
}