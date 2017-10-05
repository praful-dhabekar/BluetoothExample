package com.example.praful.bluetoothexample;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by praful on 10/5/2017.
 */

public class BluetoothConnectionService {


    private static final String TAG = "bluetoothConnectionServ";
    private static final String appName = "MYAPP";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter bluetoothAdapter;
    Context mcontext;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    private ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context ctx) {
        mcontext = ctx;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }


            if (socket != null) {
                //A connection was accepted. Perform  work associated with
                //the connection in a separate thread
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Socket is close..", e);
                    break;
                }
             }

            }
        }
        public void cancel(){
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private  class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started");
            mmDevice = device;
            deviceUUID = uuid;
        }

        @Override
        public void run() {
            BluetoothSocket tmp =null;
            Log.d(TAG, "RUN: ConnectThread started");
            try {
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.d(TAG, "RUN: ConnectThread failed");
            }
            mmSocket = tmp;

            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Log.d(TAG, "RUN: ConnectThread connected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public synchronized  void  start(){
            Log.d(TAG, "start");
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mInsecureAcceptThread != null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started");
        mProgressDialog = ProgressDialog.show(mcontext,
                "Connecting Bluetooth","Please Wait..",true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    public class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private final OutputStream mmOutputStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG,"ConnectedThread: Started");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            mProgressDialog.dismiss();

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
                mmInputStream = tmpIn;
                mmOutputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try {
                    bytes = mmInputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream"+incomingMessage);

                } catch (IOException e) {
                    Log.d(TAG,"Write: error reading inputstream "+e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"writing to outputstream: "+text);
            try {
                mmOutputStream.write(bytes);
            } catch (IOException e) {
                Log.d(TAG,"Write: error writing outputstream "+e.getMessage());
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void connected(BluetoothSocket mSocket, BluetoothDevice mDevice){
            Log.d(TAG,"connected: starting");
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        ConnectedThread tmp;
        mConnectedThread.write(out);
    }
}
