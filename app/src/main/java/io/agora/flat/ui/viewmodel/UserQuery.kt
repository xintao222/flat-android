package io.agora.flat.ui.viewmodel

import android.util.Log
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomUser
import io.agora.flat.data.repository.RoomRepository
import javax.inject.Inject

@ActivityRetainedScoped
class UserQuery @Inject constructor(
    private val roomRepository: RoomRepository,
) {
    private lateinit var roomUUID: String
    private var userMap = mutableMapOf<String, RoomUser>()

    fun update(roomUUID: String) {
        this.roomUUID = roomUUID
    }

    suspend fun loadUser(uuid: String): RoomUser? {
        return loadUsers(listOf(uuid))[uuid]
    }

    suspend fun loadUsers(uuids: List<String>): Map<String, RoomUser> {
        val filter = uuids.filter { !userMap.containsKey(it) }
        if (filter.isNotEmpty()) {
            Log.d("UserQuery", "loadUsers more $filter")
            when (val result = roomRepository.getRoomUsers(roomUUID, filter)) {
                is Success -> {
                    result.data.forEach {
                        it.value.userUUID = it.key
                    }
                    userMap.putAll(result.data)
                }
                is Failure -> return emptyMap()
            }
        }
        return userMap.filter { uuids.contains(it.key) }
    }

    fun queryUser(uuid: String): RoomUser? {
        val user = userMap[uuid]
        if (user == null) {
            Log.e("UserQuery", "should not hint here")
        }
        return user
    }

    fun hasCache(userUUID: String): Boolean {
        return userMap.containsKey(userUUID)
    }
}