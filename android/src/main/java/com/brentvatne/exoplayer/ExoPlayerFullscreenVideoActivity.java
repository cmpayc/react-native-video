package com.brentvatne.exoplayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.brentvatne.react.R;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerControlView;

import java.util.ArrayList;

public class ExoPlayerFullscreenVideoActivity extends AppCompatActivity implements ReactExoplayerView.FullScreenDelegate {
    public static final String EXTRA_EXO_PLAYER_VIEW_ID = "extra_id";
    public static final String EXTRA_ORIENTATION = "extra_orientation";
    public static final String EXTRA_PIP = "extra_pip";

    private ReactExoplayerView exoplayerView;
    private PlayerControlView playerControlView;
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
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String orientation = getIntent().getStringExtra(EXTRA_ORIENTATION);
        String withPip = getIntent().getStringExtra(EXTRA_PIP);
        if ("landscape".equals(orientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else if ("portrait".equals(orientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        setContentView(R.layout.exo_player_fullscreen_video);
        player = exoplayerView.getPlayer();

        ExoPlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playerView.setOnClickListener(v -> togglePlayerControlVisibility());

        playerControlView = findViewById(R.id.player_controls);
        playerControlView.setPlayer(player);
        // Set the fullscreen button to "close fullscreen" icon
        ImageView fullscreenIcon = playerControlView.findViewById(R.id.exo_fullscreen_icon);
        fullscreenIcon.setImageResource(R.drawable.exo_controls_fullscreen_exit);
        playerControlView.findViewById(R.id.exo_fullscreen_button)
                .setOnClickListener(v -> {
                    if (exoplayerView != null) {
                        exoplayerView.setFullscreen(false);
                    }
                });
        //Handling the playButton click event
        playerControlView.findViewById(R.id.exo_play).setOnClickListener(v -> {
            if (player != null && player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            if (exoplayerView != null) {
                exoplayerView.setPausedModifier(false);
            }
        });

        //Handling the pauseButton click event
        playerControlView.findViewById(R.id.exo_pause).setOnClickListener(v -> {
            if (exoplayerView != null) {
                exoplayerView.setPausedModifier(true);
            }
        });

        if ("yes".equals(withPip) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playerControlView.hide();
            enterPictureInPicture();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (exoplayerView != null) {
            exoplayerView.syncPlayerState();
            exoplayerView.registerFullScreenDelegate(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoplayerView.getIsPip()) {
            return;
        }
        player.setPlayWhenReady(false);
        if (exoplayerView != null) {
            exoplayerView.registerFullScreenDelegate(null);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            playerControlView.postDelayed(this::hideSystemUI, 200);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (!isInPictureInPictureMode) {
            if (exoplayerView != null) {
                Intent intent = new Intent(exoplayerView.getThemedContext(), exoplayerView.getThemedContext().getCurrentActivity().getClass());
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                exoplayerView.getThemedContext().startActivity(intent);
            }
        } else {
            if (exoplayerView != null) {
                Activity mainActivity = exoplayerView.getThemedContext().getCurrentActivity();
                mainActivity.moveTaskToBack(true);
                exoplayerView.setIsInPip(true);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (exoplayerView != null) {
                if (exoplayerView.getIsPip()) {
                    exoplayerView.setPip(false);
                } else {
                    exoplayerView.setFullscreen(false);
                }
                return false;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void togglePlayerControlVisibility() {
        if (playerControlView.isVisible()) {
            playerControlView.hide();
        } else {
            playerControlView.show();
        }
    }

    /**
     * Enables regular immersive mode.
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * Shows the system bars by removing all the flags
     * except for the ones that make the content appear under the system bars.
     */
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void closeFullScreen() {
        finish();
    }

    @Override
    public void enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && exoplayerView != null) {
            Rational aspectRatio = new Rational(2, 3);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ArrayList<RemoteAction> actions = new ArrayList<>();
                Icon pauseIcon = Icon.createWithResource(exoplayerView.getThemedContext(), R.drawable.exo_controls_pause);
                Intent pauseIntent = new Intent(exoplayerView.getThemedContext(), PipBroadcastReceiver.class);
                pauseIntent.setAction(PipBroadcastReceiver.ACTION_PAUSE);
                pauseIntent.putExtra(ExoPlayerFullscreenVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, exoplayerViewId);
                PendingIntent broadcastPause = PendingIntent.getBroadcast(
                        exoplayerView.getThemedContext(),
                        0,
                        pauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                RemoteAction pauseAction = new RemoteAction(pauseIcon, "Pause", "", broadcastPause);
                actions.add(pauseAction);
                Icon playIcon = Icon.createWithResource(exoplayerView.getThemedContext(), R.drawable.exo_controls_play);
                Intent playIntent = new Intent(exoplayerView.getThemedContext(), PipBroadcastReceiver.class);
                playIntent.setAction(PipBroadcastReceiver.ACTION_PLAY);
                playIntent.putExtra(ExoPlayerFullscreenVideoActivity.EXTRA_EXO_PLAYER_VIEW_ID, exoplayerViewId);
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