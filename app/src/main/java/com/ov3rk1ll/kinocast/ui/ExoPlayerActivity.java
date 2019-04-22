package com.ov3rk1ll.kinocast.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.ov3rk1ll.kinocast.R;

public class ExoPlayerActivity extends AppCompatActivity implements ExoPlayer.EventListener {

    private static final String TAG = "ExoPlayerActivity";
    private BandwidthMeter bandwidthMeter;
    private TrackSelector trackSelector;
    private TrackSelection.Factory trackSelectionFactory;
    private SimpleExoPlayer player;
    private DataSource.Factory dataSourceFactory;
    private ExtractorsFactory extractorsFactory;
    private DefaultBandwidthMeter defaultBandwidthMeter;
    private MediaSource mediaSource;
    private Uri mVideoUri;

    private Button stopButton;
    private Button startButton;
    private PlayerView playerView;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoUri = getIntent().getData();

        setContentView(R.layout.activity_exo_player);
        playerView = findViewById(R.id.video_view);
        statusView = findViewById(R.id.exo_status);
        

        initializePlayer();
    }

    private void initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this,
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);

        player.setPlayWhenReady(true);
        //player.seekTo(currentWindow, playbackPosition);

        defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "KinoCast"), defaultBandwidthMeter);
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mVideoUri);
        player.addListener(this);
        // Prepare the player with the source.
        player.prepare(videoSource);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.setPlayWhenReady(false);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.i(TAG, "onLoadingChanged: " + isLoading + "");
        Log.i(TAG, "Buffered Position: " + player.getBufferedPosition() + "");
        Log.i(TAG, "Buffered Percentage: " + player.getBufferedPercentage() + "");
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_READY) {
            statusView.setText("");
            Log.i(TAG, "ExoPlayer State is: READY");
        } else if (playbackState == Player.STATE_BUFFERING) {
            statusView.setText("Loading ...");
            Log.i(TAG, "ExoPlayer State is: BUFFERING");
        } else if (playbackState == Player.STATE_ENDED) {
            statusView.setText("");
            Log.i(TAG, "ExoPlayer State is: ENDED");
        } else if (playbackState == Player.STATE_IDLE) {
            statusView.setText("");
            Log.i(TAG, "ExoPlayer State is: IDLE");
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }
}