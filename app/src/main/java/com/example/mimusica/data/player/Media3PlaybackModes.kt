package com.example.mimusica.data.player

import androidx.media3.common.Player
import com.example.mimusica.domain.model.PlaybackRepeatMode

internal fun PlaybackRepeatMode.toMedia3RepeatMode(): Int =
    when (this) {
        PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
        PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

internal fun Int.toPlaybackRepeatMode(): PlaybackRepeatMode =
    when (this) {
        Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
        Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
        else -> PlaybackRepeatMode.OFF
    }
