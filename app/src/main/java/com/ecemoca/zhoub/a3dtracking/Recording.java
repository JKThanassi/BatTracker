package com.ecemoca.zhoub.a3dtracking;

/**
 * MapScanner: Indoor Map Construction using Acoustics
 * Created by zhoub on 3/1/2017.
 */
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static android.R.attr.port;


/**
 * Created by sahar on 12/2/2016.
 */
public class Recording extends Thread {
    private AudioRecord audioRecord;


    int sRate = 44100;

    private boolean record = true;
    int brSizeInc =  AudioRecord.getMinBufferSize(sRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT);

    public static final int PORT = 5555;
    private String ipAddr;

    public Recording(String ipAddr){
        this.ipAddr = ipAddr;
    }



    public void run() {

        try {

            DatagramSocket socket = new DatagramSocket();
            Log.d("VS", "Socket Created");

            byte[] buffer = new byte[brSizeInc];

            Log.d("VS","Buffer created of size " + brSizeInc);
            DatagramPacket packet;

            final InetAddress destination = InetAddress.getByName(ipAddr);
            Log.d("VS", "Address retrieved");
            //for debug purposes
            DatagramPacket packet1 = new DatagramPacket(new byte[7104], 7104, destination,PORT);
            socket.send(packet1);
            Log.d("VS", "packet sent");

            int source = MediaRecorder.AudioSource.MIC;

            // Stereo recording
            audioRecord = new AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sRate)
                            .setChannelIndexMask(AudioFormat.CHANNEL_IN_STEREO)
                            .build())
                    .setBufferSizeInBytes(brSizeInc)
                    .build();
            //audioRecord = new AudioRecord(source,sRate, AudioFormat.CHANNEL_IN_STEREO,android.media.AudioFormat.ENCODING_PCM_16BIT,brSizeInc);//2*brSizeInc?
            Log.d("VS", "Recorder initialized");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                record = true;
                audioRecord.startRecording();


                while (record == true) {

                    Log.d("VS", "record-chunk-send loop entered");
                    //reading data from MIC into buffer
                    brSizeInc = audioRecord.read(buffer, 0, buffer.length);

                    //putting buffer in the packet
                    packet = new DatagramPacket(buffer, buffer.length, destination, PORT);

                    socket.send(packet);
                    System.out.println("MinBufferSize: " + brSizeInc);


                }
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.i("status", "Stopped Recording");

            }

        } catch(UnknownHostException e) {
            Log.e("VS", "UnknownHostException");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("VS", "IOException");
        }

    }
    public void stopRecording() {

        record = false;
    }


}
