package com.ecemoca.zhoub.a3dtracking;

import android.media.MediaPlayer;

import java.io.IOException;

/**
 * MapScanner: Indoor Map Construction using Acoustics
 * Created by zhoub on 3/1/2017.
 */

public class Emitting extends Thread {
    private MediaPlayer mPlayer = null;
    private static String mFileName = null;

    public Emitting(String path) {
        mFileName = path;
    }

    public void run() {
        playRecording();
    }

    // Sound Play methods
    private void playRecording() {
        ditchMediaPlayer();
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
    }

    private void ditchMediaPlayer() {
        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    public void stopPlayback() {
        if (mPlayer != null)
            mPlayer.stop();
    }
}
