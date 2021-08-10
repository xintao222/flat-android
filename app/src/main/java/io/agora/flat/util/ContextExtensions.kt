package io.agora.flat.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.getAppVersion(defaultVersion: String = "1.0.0"): String {
    var versionName: String? = null
    try {
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return versionName ?: defaultVersion
}

fun Context.isApkInDebug(): Boolean {
    return try {
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Context.showDebugToast(@StringRes resId: Int) {
    if (isApkInDebug()) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showDebugToast(message: String) {
    if (isApkInDebug()) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.dp2px(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun Context.px2dp(px: Int): Int {
    return Resources.getSystem().displayMetrics.density.let {
        (px.toFloat() / it + 0.5).toInt()
    }
}

fun ComponentActivity.delayAndFinish(duration: Long = 2000, message: String = "") {
    lifecycleScope.launch {
        if (message.isNotBlank()) {
            showToast(message)
        }
        delay(duration)
        finish()
    }
}

fun Context.contentFileInfo(uri: Uri): ContentFileInfo? {
    val mediaType = contentResolver.getType(uri) ?: "text/plain"
    return contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        cursor.moveToFirst()
        val filename = cursor.getString(0)
        val size = cursor.getLong(1)
        cursor.close()

        return ContentFileInfo(filename, size, mediaType)
    }
}

data class ContentFileInfo(val filename: String, val size: Long, val mediaType: String)

