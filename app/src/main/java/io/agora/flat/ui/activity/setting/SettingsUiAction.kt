package io.agora.flat.ui.activity.setting

sealed class SettingsUiAction {
    object Back : SettingsUiAction()
    object Logout : SettingsUiAction()
    data class SetNetworkAcceleration(val acceleration: Boolean) : SettingsUiAction()
}