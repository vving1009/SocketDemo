/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scut.carson_ho.socket_carson.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scut.carson_ho.socket_carson.Constants;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class UdpService implements SocketService {
    // Debugging
    private static final String TAG = "UdpService";

    // indicate the current connection state
    private final int STATE_NONE = 0;       // we're doing nothing
    private final int STATE_LISTEN = 1;     // now listening for incoming connections

    private final int TTLTIME = 100;
    private final String MULTICAST_ADDR = "255.255.255.255";

    // Member fields
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private SendThread mSendThread;
    private MultiAcceptThread mMultiAcceptThread;
    private MultiSendThread mMultiSendThread;
    private int mState;
    private int mNewState;
    private ExecutorService mThreadPool;
    private ReceiveMessageListener mReceiveMessageListener;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public UdpService(Context context, Handler handler) {
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
        mThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     */
    public UdpService(Context context) {
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = new Handler();
        mThreadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void setReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        mReceiveMessageListener = receiveMessageListener;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    @Override
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Override
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mSendThread != null) {
            mSendThread.cancel();
            mSendThread = null;
        }

        // Cancel any thread attempting to make a connection
        if (mMultiSendThread != null) {
            mMultiSendThread.cancel();
            mMultiSendThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
/*        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mThreadPool.execute(mAcceptThread);
        }*/

        // Start the thread to listen on a BluetoothServerSocket
        if (mMultiAcceptThread == null) {
            mMultiAcceptThread = new MultiAcceptThread();
            mThreadPool.execute(mMultiAcceptThread);
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    @Override
    public synchronized void connect(String ip, int port) {
    }

    /**
     * Stop all threads
     */
    @Override
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mSendThread != null) {
            mSendThread.cancel();
            mSendThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mMultiSendThread != null) {
            mMultiSendThread.cancel();
            mMultiSendThread = null;
        }
        if (mMultiAcceptThread != null) {
            mMultiAcceptThread.cancel();
            mMultiAcceptThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see SendThread#setMessage(byte[])
     */
    @Override
    public void write(byte[] out, String ip) {
        synchronized (this) {
            if (mSendThread == null) {
                mSendThread = new SendThread();
            }
            mThreadPool.execute(mSendThread.setMessage(out).setIp(ip));
        }
    }

    @Override
    public void multiWrite(byte[] out) {
        synchronized (this) {
            if (mMultiSendThread == null) {
                mMultiSendThread = new MultiSendThread();
            }
            mThreadPool.execute(mMultiSendThread.setMessage(out));
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        UdpService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread implements Runnable {
        // The local server socket
        DatagramSocket datagramSocket;

        @Override
        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            //setName("AcceptThread");

            Log.d(TAG, "server client start.");

            try {
                datagramSocket = new DatagramSocket(PORT);
                Log.d(TAG, "server client run.");
                int remotePort = datagramSocket.getLocalPort();
                Log.d(TAG, "receiving............." + ", Port: " + remotePort);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread failed", e);
                e.printStackTrace();
            }
            byte[] data = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            mState = STATE_LISTEN;
            // Listen to the server socket if we're not connected
            while (mState == STATE_LISTEN) {
                // If a connection was accepted
                if (datagramSocket != null) {
                    try {
                        Log.d(TAG, "run: datagramSocket.receive(datagramPacket)");
                        datagramSocket.receive(datagramPacket);
                        mReceiveMessageListener.onReceived(new String(datagramPacket.getData()));
                        Log.d("UDP Demo", datagramPacket.getAddress().getHostAddress()
                                + ":" + new String(datagramPacket.getData()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "datagramSocket.receive(datagramPacket)", e);
                    }
                }
            }
            Log.i(TAG, "END AcceptThread");
        }

        void cancel() {
            Log.d(TAG, "AcceptThread cancel: " + Thread.currentThread());
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class SendThread implements Runnable {

        private DatagramSocket datagramSocket;
        private byte[] message;
        private String ip;

        SendThread setMessage(byte[] msg) {
            this.message = msg;
            return this;
        }

        SendThread setIp(String ip) {
            this.ip = ip;
            return this;
        }

        @Override
        public void run() {
            Log.i(TAG, "SEND mSendThread");
            //setName("SendThread");

            try {
                datagramSocket = new DatagramSocket();
            } catch (IOException e) {
                Log.e(TAG, "SendThread create() failed", e);
                try {
                    datagramSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            InetAddress addr = null;
            try {
                Log.d(TAG, "ip = " + ip);
                addr = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "InetAddress.getByName(ip);", e);
            }
            if (message != null && message.length > 0) {
                Log.d(TAG, "send msg: " + new String(message) + ", ip: " + addr + ", port: " + PORT);
                DatagramPacket datagramPacket = new DatagramPacket(message, message.length, addr, PORT);
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "datagramSocket.send(datagramPacket);", e);
                }
            }
        }

        void cancel() {
            try {
                datagramSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class MultiAcceptThread implements Runnable {
        // The local server socket
        MulticastSocket datagramSocket;

        @Override
        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            //setName("AcceptThread");

            Log.d(TAG, "server client start.");

            try {
                datagramSocket = new MulticastSocket(PORT);
                datagramSocket.setTimeToLive(TTLTIME);
                datagramSocket.joinGroup(InetAddress.getByName(MULTICAST_ADDR));
                Log.d(TAG, "server client run.");
                int remotePort = datagramSocket.getLocalPort();
                Log.d(TAG, "receiving............." + ", Port: " + remotePort);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread failed", e);
                e.printStackTrace();
            }
            byte[] data = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            mState = STATE_LISTEN;
            // Listen to the server socket if we're not connected
            while (mState == STATE_LISTEN) {
                // If a connection was accepted
                if (datagramSocket != null) {
                    try {
                        Log.d(TAG, "run: datagramSocket.receive(datagramPacket)");
                        datagramSocket.receive(datagramPacket);
                        mReceiveMessageListener.onReceived(new String(datagramPacket.getData()));
                        Log.d("UDP Demo", datagramPacket.getAddress().getHostAddress()
                                + ":" + new String(datagramPacket.getData()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "datagramSocket.receive(datagramPacket)", e);
                    }
                }
            }
            Log.i(TAG, "END AcceptThread");
        }

        void cancel() {
            Log.d(TAG, "AcceptThread cancel: " + Thread.currentThread());
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    private class MultiSendThread implements Runnable {

        private MulticastSocket datagramSocket;
        private byte[] message;
        private String ip;

        MultiSendThread setMessage(byte[] msg) {
            this.message = msg;
            return this;
        }

        MultiSendThread setIp(String ip) {
            this.ip = ip;
            return this;
        }

        @Override
        public void run() {
            Log.i(TAG, "SEND mSendThread");
            //setName("SendThread");

            try {
                datagramSocket = new MulticastSocket();
                datagramSocket.setTimeToLive(TTLTIME);
            } catch (IOException e) {
                Log.e(TAG, "SendThread create() failed", e);
                try {
                    datagramSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            if (message != null && message.length > 0) {
                Log.d(TAG, "send msg: " + new String(message) + ", ip: " + MULTICAST_ADDR + ", port: " + PORT);
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(MULTICAST_ADDR), PORT);
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "datagramSocket.send(datagramPacket);", e);
                }
            }
        }

        void cancel() {
            try {
                datagramSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
