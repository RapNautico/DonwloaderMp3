package com.example.mimusica.data.player

import android.content.Context

internal class PlaybackSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): PlaybackSnapshot? {
        val encoded = preferences.getString(SNAPSHOT_KEY, null) ?: return null
        val snapshot = PlaybackSnapshotCodec.decode(encoded)
        if (snapshot == null) clear()
        return snapshot
    }

    fun write(snapshot: PlaybackSnapshot) {
        preferences.edit()
            .putString(SNAPSHOT_KEY, PlaybackSnapshotCodec.encode(snapshot))
            .apply()
    }

    fun clear() {
        preferences.edit().remove(SNAPSHOT_KEY).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "playback_state"
        const val SNAPSHOT_KEY = "queue_snapshot"
    }
}
