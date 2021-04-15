package link.netless.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import link.netless.flat.Constants
import link.netless.flat.data.ErrorResult
import link.netless.flat.data.Success
import link.netless.flat.data.model.RoomPlayInfo
import link.netless.flat.data.model.RtcUser
import link.netless.flat.data.repository.RoomRepository
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo: SharedFlow<RoomPlayInfo?> = _roomPlayInfo

    private var _joining = MutableStateFlow<Boolean>(true)
    val joining: SharedFlow<Boolean> = _joining

    private var _roomUsersMap = MutableStateFlow<Map<String, RtcUser>>(emptyMap())
    val roomUsersMap: SharedFlow<Map<String, RtcUser>> = _roomUsersMap

    init {
        viewModelScope.launch {
            when (val result =
                roomRepository.joinRoom(intentValue(Constants.IntentKey.ROOM_UUID))) {
                is Success -> {
                    _roomPlayInfo.value = result.data
                }
                is ErrorResult -> {

                }
            }
            _joining.value = false
        }
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }

    fun requestRoomUsers(roomUUID: String, usersUUID: List<String>) {
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, usersUUID)) {
                is Success -> {
                    _roomUsersMap.value = result.data;
                }
                is ErrorResult -> {

                }
            }
        }
    }
}