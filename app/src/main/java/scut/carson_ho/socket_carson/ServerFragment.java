package scut.carson_ho.socket_carson;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import scut.carson_ho.socket_carson.service.SocketService;
import scut.carson_ho.socket_carson.service.TcpService;
import scut.carson_ho.socket_carson.service.UdpService;

public class ServerFragment extends Fragment {

    private static final String TAG = "ServerFragment";
    private final int PORT = 8191;

    @BindView(R.id.server)
    Button server;
    @BindView(R.id.edit)
    EditText edit;
    @BindView(R.id.send)
    Button send;
    @BindView(R.id.receive_message)
    TextView receiveMessage;
    @BindView(R.id.receive)
    Button receive;
    Unbinder unbinder;
    @BindView(R.id.stop)
    Button stop;

    private Handler mMainHandler;
    private SocketService mSocketService;

    @SuppressLint("HandlerLeak")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.server_fragment, container, false);
        unbinder = ButterKnife.bind(this, root);
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        receiveMessage.setText((String) msg.obj);
                        break;
                }
            }
        };
        mSocketService = new UdpService(getContext());
        mSocketService.setReceiveMessageListener(message -> {
            Log.d(TAG, "setReceiveMessageListener: " + message);
            getActivity().runOnUiThread(() -> receiveMessage.setText(message));
            //receiveMessage::setText
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.server)
    public void onServerClicked() {
        mSocketService.start();
        /*mThreadPool.execute(() -> {
            isRun = true;
            try {
                Log.d(TAG, "server client start.");
                mServerSocket = new ServerSocket(PORT);
                Log.d(TAG, "server client run.");
                socket = mServerSocket.accept();
                String remoteIP = socket.getInetAddress().getHostAddress();
                int remotePort = socket.getLocalPort();
                Log.d(TAG, "A client connected. IP:" + remoteIP + ", Port: " + remotePort);
                Log.d(TAG, "\"server: receiving.............\"");
                BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

                while (isRun) {
                    *//*BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                    Log.d(TAG, "103");
                    //PrintWriter pw = new PrintWriter(socket.getOutputStream(), false);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    String receive = sb.toString();*//*
                    *//*byte[] data = null;
                    if (socket.isConnected()) {
                        byte[] data = new byte[bis.available()];
                        int length;
                        while ((length = bis.read(data)) != -1) {
                            String msg = new String(data, 0 , length);
                            Log.d(TAG, "socket received: " + msg);
                            mMainHandler.obtainMessage(0, msg).sendToTarget();
                        }
                    }*//*
                    byte[] data = null;
                    if (socket.isConnected()) {
                        try {
                            //bis = new BufferedInputStream(socket.getInputStream());
                            data = new byte[bis.available()];
                            bis.read(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        data = new byte[1];
                    }
                    if (data.length > 1) {
                        String msg = new String(data);
                        Log.d(TAG, "socket received: " + msg);
                        mMainHandler.obtainMessage(0, msg).sendToTarget();
                    }
                    //pw.println("Your message has been received successfully！.");
                    //pw.close();
                    //br.close();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "server exception: " + e.getMessage());
            } finally {
                Log.d(TAG, "server socket close.");
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //handler.obtainMessage(10, (Object) "接受 完成").sendToTarget();
            }
        });*/
    }

    @OnClick(R.id.send)
    public void onSendClicked() {
        /*mThreadPool.execute(() -> {
            try {
                outputStream = socket.getOutputStream();
                if (outputStream != null) {
                    outputStream.write((edit.getText().toString() + "\n").getBytes("utf-8"));
                    outputStream.flush();
                    Log.d(TAG, "onSendClicked: " + edit.getText().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/
        try {
            mSocketService.write((edit.getText().toString() + "\n").getBytes("utf-8"), null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.stop)
    public void onStopedClicked() {
        /*try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRun = false;*/
        mSocketService.stop();
    }
}
