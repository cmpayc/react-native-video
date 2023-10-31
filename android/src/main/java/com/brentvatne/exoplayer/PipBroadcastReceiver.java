package com.brentvatne.exoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;

public class PipBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "PipBroadcastReceiver";
    public static final String ACTION_PAUSE = "ActionPause";
    public static final String ACTION_PLAY = "ActionPlay";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (intent.getAction().equals(ACTION_PLAY)) {
            int exoplayerViewId = intent.getIntExtra(ExoPlayerPipVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, -1);
            ReactExoplayerView exoplayerView = ReactExoplayerView.getViewInstance(exoplayerViewId);
            if (exoplayerView == null) {
                return;
            }
            ExoPlayer player = exoplayerView.getPlayer();
            if (player != null && player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            if (exoplayerView != null) {
                exoplayerView.setPausedModifier(false);
                exoplayerView.setIsPipPaused(false);
            }
        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            int exoplayerViewId = intent.getIntExtra(ExoPlayerPipVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, -1);
            ReactExoplayerView exoplayerView = ReactExoplayerView.getViewInstance(exoplayerViewId);
            if (exoplayerView == null) {
                return;
            }
            if (exoplayerView != null) {
                exoplayerView.setPausedModifier(true);
                exoplayerView.setIsPipPaused(true);
            }
        }
    }
}