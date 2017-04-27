package com.ecemoca.zhoub.a3dtracking;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Emitting emitting = null;
    private Recording recording = null;
    private InertialSensor inertial = null;
    private String sourcePath = Environment.getExternalStorageDirectory() + "/3DTracking/chirp.wav";
    private String recordPath = Environment.getExternalStorageDirectory() +  "/3DTracking";
    private String inertialPath = Environment.getExternalStorageDirectory() +  "/3DTracking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Button buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setText("START");
        final Button buttonEmit = (Button) findViewById(R.id.buttonEmit);
        buttonEmit.setText("EMIT");
        buttonListener();

    }

    private void buttonListener() {
        final Button buttonDuration = (Button) findViewById(R.id.buttonDuration);
        buttonDuration.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonDuration.getText() == "FMCW") {
                    sourcePath = Environment.getExternalStorageDirectory() + "/3DTracking/chirp.wav";
                    buttonDuration.setText("CHIRP");
                }
                else {
                    sourcePath = Environment.getExternalStorageDirectory() + "/3DTracking/fmcw.wav";
                    buttonDuration.setText("FMCW");
                }
            }
        });

        final Button buttonPlay =  (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonPlay.getText() == "START") {
                    emitting = new Emitting(sourcePath);
                    recording = new Recording(recordPath);
                    inertial = new InertialSensor(getApplicationContext(), inertialPath);
                    emitting.start();
                    recording.start();
                    inertial.start();
                    buttonPlay.setText("STOP");
                }
                else {
                    if (emitting != null) {
                        emitting.stopPlayback();
                        try {
                            emitting.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (recording != null) {
                        recording.stopRecording();
                        try {
                            recording.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (inertial != null) {
                        inertial.stopSensor();
                        try {
                            inertial.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    buttonPlay.setText("START");
                }
            }
        });
    }

}
