package com.brentvatne.exoplayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.content.ServiceConnection;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.brentvatne.react.R;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.LegacyPlayerControlView;

import java.util.ArrayList;

public class ExoPlayerPipVideoActivity extends AppCompatActivity implements ReactExoplayerView.PipDelegate {
    public static final String EXTRA_EXO_PLAYER_VIEW_ID = "extra_pip_id";

    private ReactExoplayerView exoplayerView;
    private LegacyPlayerControlView playerControlView;
    private ExoPlayer player;
    private int exoplayerViewId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        exoplayerViewId = getIntent().getIntExtra(EXTRA_EXO_PLAYER_VIEW_ID, -1);
        exoplayerView = ReactExoplayerView.getViewInstance(exoplayerViewId);
        if (exoplayerView == null) {
            finish();
            return;
        }
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.exo_player_fullscreen_video);
        player = exoplayerView.getPlayer();

        ExoPlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playerControlView = findViewById(R.id.player_controls);
        playerControlView.setPlayer(player);
        playerControlView.hide();

        enterPictureInPicture();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (exoplayerView != null) {
            exoplayerView.syncPlayerState();
            exoplayerView.registerPipDelegate(this);
            Boolean isSecured = exoplayerView.getIsSecured();
            if (isSecured) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exoplayerView != null) {
            exoplayerView.registerPipDelegate(null);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (!isInPictureInPictureMode) {
            if (exoplayerView != null) {
                Intent intent = new Intent(exoplayerView.getThemedContext(), exoplayerView.getThemedContext().getCurrentActivity().getClass());
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                exoplayerView.getThemedContext().startActivity(intent);
                finish();
            }
        } else {
            if (exoplayerView != null) {
                exoplayerView.setIsInPip(true);
            }
        }
    }

    private void togglePlayerControlVisibility() {
        if (playerControlView.isVisible()) {
            playerControlView.hide();
        } else {
            playerControlView.show();
        }
    }

    @Override
    public void enterPictureInPicture() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && exoplayerView != null) {
            Rational aspectRatio = new Rational(2, 3);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ArrayList<RemoteAction> actions = new ArrayList<>();
                Icon pauseIcon = Icon.createWithResource(exoplayerView.getThemedContext(), androidx.media3.ui.R.drawable.exo_icon_pause);
                Intent pauseIntent = new Intent(exoplayerView.getThemedContext(), PipBroadcastReceiver.class);
                pauseIntent.setAction(PipBroadcastReceiver.ACTION_PAUSE);
                pauseIntent.putExtra(ExoPlayerPipVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, exoplayerViewId);
                PendingIntent broadcastPause = PendingIntent.getBroadcast(
                        exoplayerView.getThemedContext(),
                        0,
                        pauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                RemoteAction pauseAction = new RemoteAction(pauseIcon, "Pause", "", broadcastPause);
                actions.add(pauseAction);
                Icon playIcon = Icon.createWithResource(exoplayerView.getThemedContext(), androidx.media3.ui.R.drawable.exo_icon_play);
                Intent playIntent = new Intent(exoplayerView.getThemedContext(), PipBroadcastReceiver.class);
                playIntent.setAction(PipBroadcastReceiver.ACTION_PLAY);
                playIntent.putExtra(ExoPlayerPipVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, exoplayerViewId);
                PendingIntent broadcastPlay = PendingIntent.getBroadcast(
                        exoplayerView.getThemedContext(),
                        0,
                        playIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                RemoteAction playAction = new RemoteAction(playIcon, "Play", "", broadcastPlay);
                actions.add(playAction);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .setActions(actions)
                        .build();
                enterPictureInPictureMode(params);
            } else {
                enterPictureInPictureMode();
            }
        }
    }
}
