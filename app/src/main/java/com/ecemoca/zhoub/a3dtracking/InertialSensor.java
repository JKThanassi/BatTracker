package com.ecemoca.zhoub.a3dtracking;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Network;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.port;
import static android.os.Build.VERSION_CODES.N;

/**
 * MapScanner: Indoor Map Construction using Acoustics
 * Created by zhoub on 3/7/2017.
 */

public class InertialSensor extends Thread {
    private int sensorScanRate = 100;
    private String path = null;
    private SensorManager mSensorManager = null;
    private Context mContext = null;
    private List<Sensor> currentDevice = new ArrayList<>();
    private SensorEventListener mListener;
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    private float[] initialOrientation = new float[3];
    private boolean firstOrien = true;
    private StringBuilder sb = null;
    private StringBuilder sbRotate = null;
    private String fileNamePreStr = null;
    private float[] mRotationMatrix = null;
    private float[] globalRotationMatrix = null;
    private float[] orientationVals = null;
    private sendDataOverNetwork networkHandler;

    public InertialSensor(Context applicationContext, String path) {
        this.path = path;
        mContext = applicationContext;
        sb = new StringBuilder();
        sbRotate = new StringBuilder();
        networkHandler = new sendDataOverNetwork("10.6.0.221", 10000);
    }

    public void run() {
        networkHandler.start();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
            currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null)
            currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
            currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null)
            currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR));
        else if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
            currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));


        mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if ((mRotationMatrix != null) && event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    float[] deviceRelativeAcceleration = new float[4];
                    deviceRelativeAcceleration[0] = event.values[0];
                    deviceRelativeAcceleration[1] = event.values[1];
                    deviceRelativeAcceleration[2] = event.values[2];
                    deviceRelativeAcceleration[3] = 0;

                    // Change coordinates: X axis -> East, Y axis -> North, Z axis -> Sky
                    float[] earthAcc = new float[16], inv = new float[16];
                    android.opengl.Matrix.invertM(inv, 0, mRotationMatrix, 0);
                    android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
//                    String add = System.currentTimeMillis() + "," + earthAcc[0] + "," + earthAcc[1] + "," + earthAcc[2] + "\n";
                    String add = System.currentTimeMillis() + "," + deviceRelativeAcceleration[0] + "," + deviceRelativeAcceleration[1] + "," + deviceRelativeAcceleration[2] + "\n";
                    sb.append(add);
                }
//                if ((gravityValues != null) && (orientationVals != null)  && event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//                    float[] deviceRelativeAcceleration = new float[4];
//                    deviceRelativeAcceleration[0] = event.values[0];
//                    deviceRelativeAcceleration[1] = event.values[1];
//                    deviceRelativeAcceleration[2] = event.values[2];
//                    deviceRelativeAcceleration[3] = 0;
//
//                    // Change coordinates: X axis -> East, Y axis -> North, Z axis -> Sky
//                    float[] R = new float[16], I = new float[16], earthAcc = new float[16], inv = new float[16];
//                    SensorManager.getRotationMatrix(R, I, gravityValues, orientationVals);
//                    android.opengl.Matrix.invertM(inv, 0, R, 0);
//                    android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
//                    String add = System.currentTimeMillis() + "," + earthAcc[0] + "," + earthAcc[1] + "," + earthAcc[2] + "\n";
//                    sb.append(add);
//                }
                else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    gravityValues = event.values;
                }
                else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticValues = event.values;
                }
                else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR || event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    mRotationMatrix = new float[16];
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix,event.values);

                    orientationVals = new float[3];
                    SensorManager.getOrientation(mRotationMatrix, orientationVals);

                    String timeStamp = "" + System.currentTimeMillis();

                    // Optionally convert the result from radians to degrees
                    orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
                    orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
                    orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);
                    if (firstOrien) {
                        initialOrientation[0] = orientationVals[0];
                        initialOrientation[1] = orientationVals[1];
                        initialOrientation[2] = orientationVals[2];
                        fileNamePreStr = timeStamp;
                        firstOrien = false;
                    }
//                    orientationVals[0] -= initialOrientation[0];
//                    orientationVals[1] -= initialOrientation[1];
//                    orientationVals[2] -= initialOrientation[2];

                    final String add = timeStamp + "," + orientationVals[0] + "," + orientationVals[1] + "," + orientationVals[2] + "\n";
                    //System.out.println(add);
                    networkHandler.pushData(add);


                    sbRotate.append(add);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        for (Sensor insert : currentDevice) {
            mSensorManager.registerListener(mListener, insert, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stopSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mListener);
        }
        //writeFile();
    }

    private class sendDataOverNetwork extends Thread{
        private Socket mSocket;
        private PrintWriter out;
        private InetAddress IPADDR;
        private String ipaddr;
        private final int PORT;
        private ArrayList<String> gyroData;
        private boolean isActive = true;

        public sendDataOverNetwork(String ipaddr, int PORT) {
            this.ipaddr = ipaddr;
            this.PORT = PORT;
            this.gyroData = new ArrayList<String>();
        }

        public void setActive(boolean active) {
            isActive = active;
        }

        @Override
        public void run() {
            try{

                IPADDR = InetAddress.getByName(ipaddr);
                mSocket = new Socket(IPADDR, PORT);
                out = new PrintWriter(mSocket.getOutputStream(), true);
                Log.d("SDON", "socket connected on port: " + PORT);
                //uses arraylist as a stack to store data and will pop the first index if that
                //index exists and then sends it overt the network
                while(isActive){
                    if(gyroData.size() >= 1){
                        out.println(gyroData.remove(0));
                    }
                }
                out.println("disconnect");
                mSocket.close();
                Log.d("SDON", "socket closed");
                out.close();
                Log.d("SDON", "PrintWriter closed");

            }catch(Exception e){
                e.printStackTrace();
            }
        }


        public void pushData(String data){
            gyroData.add(data);
        }

    }

    private void writeFile() {
        String mFileName = fileNamePreStr + "_acc" +  ".csv";

        File fileAcc = new File(path + "/Recording/" + mFileName);
        try {
            FileWriter fstream = new FileWriter(fileAcc);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mFileName = fileNamePreStr + "_ori" + ".csv";
        File fileRotate = new File(path + "/Recording/" + mFileName);
        try {
            FileWriter fstream = new FileWriter(fileRotate);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(sbRotate.toString());
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stopNetwork(){
        try {
            networkHandler.setActive(false);
            sleep(5);
            networkHandler.join();
        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
