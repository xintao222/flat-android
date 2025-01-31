package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.model.*
import io.agora.flat.data.toResult
import io.agora.flat.http.api.CloudRecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRecordRepository @Inject constructor(
    private val cloudRecordService: CloudRecordService,
) {
    suspend fun acquireRecord(roomUUID: String, expiredHour: Int = 24): Result<RecordAcquireRespData> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.acquireRecord(
                RecordAcquireReq(
                    roomUUID,
                    RecordAcquireReqData(RecordAcquireReqDataClientRequest(expiredHour, 0))
                )
            ).toResult()
        }
    }

    suspend fun startRecordWithAgora(
        roomUUID: String,
        resourceId: String,
        transcodingConfig: TranscodingConfig = TranscodingConfig(width = 640, height = 360, fps = 15, bitrate = 500),
        mode: AgoraRecordMode = AgoraRecordMode.Mix,
    ): Result<RecordStartRespData> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.startRecordWithAgora(
                RecordStartReq(
                    roomUUID,
                    AgoraRecordParams(resourceId, mode),
                    AgoraRecordStartedData(
                        ClientRequest(
                            RecordingConfig(
                                subscribeUidGroup = 0,
                                transcodingConfig = transcodingConfig
                            )
                        )
                    )
                )
            ).toResult()
        }
    }

    suspend fun queryRecordWithAgora(
        roomUUID: String,
        resourceId: String, mode: AgoraRecordMode = AgoraRecordMode.Mix,
    ): Result<RecordQueryRespData> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.queryRecordWithAgora(
                RecordReq(
                    roomUUID,
                    AgoraRecordParams(resourceId, mode),
                )
            ).toResult()
        }
    }

    suspend fun updateRecordLayoutWithAgora(
        roomUUID: String,
        resourceId: String,
        clientRequest: UpdateLayoutClientRequest,
        mode: AgoraRecordMode = AgoraRecordMode.Mix,
    ): Result<RecordQueryRespData> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.updateRecordLayoutWithAgora(
                RecordUpdateLayoutReq(
                    roomUUID,
                    AgoraRecordParams(resourceId, mode),
                    AgoraRecordUpdateLayoutData(clientRequest)
                )
            ).toResult()
        }
    }

    suspend fun stopRecordWithAgora(
        roomUUID: String,
        resourceId: String,
        sid: String,
        mode: AgoraRecordMode = AgoraRecordMode.Mix,
    ): Result<RecordStopRespData> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.stopRecordWithAgora(
                RecordReq(
                    roomUUID,
                    AgoraRecordParams(resourceId, mode, sid),
                )
            ).toResult()
        }
    }

    suspend fun getRecordInfo(roomUUID: String): Result<RecordInfo> {
        return withContext(Dispatchers.IO) {
            cloudRecordService.getRecordInfo(PureRoomReq(roomUUID)).toResult()
        }
    }
}