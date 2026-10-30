package com.example.mimusica.data.player

import kotlinx.coroutines.CancellationException

internal suspend fun executePlaybackCommand(
    command: suspend () -> Unit,
    onFailure: (Exception) -> Unit,
) {
    try {
        command()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}
