package io.agora.flat.data.model

data class CloudStorageFileListResp constructor(
    val totalUsage: Long,
    val files: List<CloudStorageFile>,
)

data class CloudStorageFile constructor(
    val fileUUID: String,
    val fileName: String,
    val fileSize: Long,
    val fileURL: String,
    val convertStep: FileConvertStep,
    val taskUUID: String,
    val taskToken: String,
    val createAt: Long,
)

enum class FileConvertStep {
    None,
    Converting,
    Done,
    Failed;
}