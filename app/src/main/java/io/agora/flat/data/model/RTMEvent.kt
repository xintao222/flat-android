package io.agora.flat.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

sealed class RTMEvent {
    data class ChannelMessage(@SerializedName("v") val text: String) : RTMEvent()
    data class Notice(@SerializedName("v") val text: String) : RTMEvent()
    data class RaiseHand(@SerializedName("v") val v: Boolean) : RTMEvent()
    data class AcceptRaiseHand(@SerializedName("v") val value: AcceptRaiseHandValue) : RTMEvent()
    data class CancelAllHandRaising(@SerializedName("v") val v: Boolean) : RTMEvent()
    data class BanText(@SerializedName("v") val v: Boolean) : RTMEvent()
    data class Speak(@SerializedName("v") val v: Boolean) : RTMEvent()
    data class DeviceState(@SerializedName("v") val value: DeviceStateValue) : RTMEvent()
    data class ClassMode(@SerializedName("v") val classModeType: ClassModeType) : RTMEvent()
    data class RoomStatus(@SerializedName("v") val roomStatus: RoomStatus) : RTMEvent()
    data class RequestChannelStatus(@SerializedName("v") val value: RequestChannelStatusValue) : RTMEvent()
    data class ChannelStatus(@SerializedName("v") val value: ChannelStatusValue) : RTMEvent()

    companion object {
        val gson = Gson()

        fun parseRTMEvent(text: String): RTMEvent {
            val jsonObject = JsonParser.parseString(text).asJsonObject
            val type = jsonObject.get("t").asString
            // val value = jsonObject.get("v")
            return when (RTMessageType.valueOf(type)) {
                RTMessageType.Notice -> gson.fromJson(jsonObject, Notice::class.java)
                RTMessageType.RaiseHand -> gson.fromJson(jsonObject, RaiseHand::class.java)
                RTMessageType.AcceptRaiseHand -> gson.fromJson(jsonObject, AcceptRaiseHand::class.java)
                RTMessageType.CancelAllHandRaising -> gson.fromJson(jsonObject, CancelAllHandRaising::class.java)
                RTMessageType.BanText -> gson.fromJson(jsonObject, BanText::class.java)
                RTMessageType.Speak -> gson.fromJson(jsonObject, Speak::class.java)
                RTMessageType.DeviceState -> gson.fromJson(jsonObject, DeviceState::class.java)
                RTMessageType.ClassMode -> gson.fromJson(jsonObject, ClassMode::class.java)
                RTMessageType.RoomStatus -> gson.fromJson(jsonObject, RoomStatus::class.java)
                RTMessageType.RequestChannelStatus -> gson.fromJson(jsonObject, RequestChannelStatus::class.java)
                RTMessageType.ChannelStatus -> gson.fromJson(jsonObject, ChannelStatus::class.java)
                // TODO
                else -> ChannelMessage(text)
            }
        }

        fun toText(event: RTMEvent): String {
            return gson.toJson(event)
        }
    }
}

data class RTMMessageUser(
    val name: String,
    val camera: Boolean,
    val mic: Boolean,
    /** this filed is only accepted from room creator */
    val isSpeak: Boolean,
)

data class AcceptRaiseHandValue(
    val userUUID: String,
    val accept: Boolean
)

data class DeviceStateValue(
    val userUUID: String,
    val camera: Boolean,
    val mic: Boolean
)

data class RequestChannelStatusValue(
    val roomUUID: String,
    /** these users should response */
    val userUUIDs: List<String>,
    /** also inform others about current user states */
    val user: RTMMessageUser
)

data class ChannelStatusValue(
    val roomUUID: String,
    /** these users should response */
    val userUUIDs: List<String>,
    /** also inform others about current user states */
    val user: RTMMessageUser
)

enum class RTMessageType {
    /** group message */
    ChannelMessage,

    /** a notice message */
    Notice,

    /** A joiner raises hand */
    RaiseHand,

    /** creator accept hand raising from a joiner */
    AcceptRaiseHand,

    /** creator cancel all hand raising */
    CancelAllHandRaising,

    /** creator ban all rtm */
    BanText,

    /** creator allows a joiner or joiners allows themselves to speak */
    Speak,

    /**
     * joiner updates own camera and mic state
     * creator may turn off joiner's camera and mic but not turn on
     */
    DeviceState,

    /** creator updates class mode */
    ClassMode,

    /** creator updates class status */
    RoomStatus,

    /** joiner request room's status */
    RequestChannelStatus,

    /** send room's status */
    ChannelStatus;
}

