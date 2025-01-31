package io.agora.flat.ui.compose

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

class CustomInteractionSource : MutableInteractionSource {
    override val interactions = MutableSharedFlow<Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private lateinit var pendingPress: PressInteraction.Press

    override suspend fun emit(interaction: Interaction) {
        if (interaction is PressInteraction.Press) {
            pendingPress = interaction
        } else {
            if (interaction is PressInteraction.Release) {
                interactions.emit(pendingPress)
                delay(100)
                interactions.emit(interaction)
            }
        }
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        return interactions.tryEmit(interaction)
    }
}