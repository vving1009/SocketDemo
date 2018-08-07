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

public class ClientFragment extends Fragment {

    private final int PORT = 8191;
    private static final String TAG = "ClientFragment";
    @BindView(R.id.ip)
    EditText ip;
    @BindView(R.id.connect)
    Button connect;
    @BindView(R.id.disconnect)
    Button disconnect;
    @BindView(R.id.edit)
    EditText edit;
    @BindView(R.id.send)
    Button send;
    @BindView(R.id.receive_message)
    TextView receiveMessage;
    Unbinder unbinder;

    private Handler mMainHandler;
    private SocketService mSocketService;


    @SuppressLint("HandlerLeak")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.client_fragment, container, false);
        unbinder = ButterKnife.bind(this, rootView);
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
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.connect)
    public void onConnectClicked() {
        /*mThreadPool.execute(new Runnable() {
            BufferedInputStream bufferedInputStream;

            public byte[] receiveData() {

                byte[] data = null;
                if (socket.isConnected()) {
                    try {
                        bufferedInputStream = new BufferedInputStream(socket.getInputStream());
                        data = new byte[bufferedInputStream.available()];
                        bufferedInputStream.read(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    data = new byte[1];
                }
                return data;
            }

            @Override
            public void run() {
                isRun = true;
                try {
                    socket = new Socket(ip.getText().toString(), PORT);
                    Log.d(TAG, "socket connect: " + socket.isConnected());

                    outputStream = socket.getOutputStream();
                    PrintStream out = new PrintStream(socket.getOutputStream(), true, "gbk");
                    while (isRun) {
                        *//*BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                        String line;
                        StringBuilder sb = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        Log.d(TAG, "onConnectClicked: " + br.readLine());
                        mMainHandler.obtainMessage(0, sb.toString()).sendToTarget();*//*
                        byte[] data = receiveData();
                        if (data.length > 1) {
                            System.out.println(new String(data));
                            mMainHandler.obtainMessage(0, new String(data)).sendToTarget();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onConnectClicked: error" + e.getMessage());
                }
            }
        });*/
        //mSocketService.connect(ip.getText().toString(), PORT);
        mSocketService.start();
    }

    @OnClick(R.id.disconnect)
    public void onDisconnectClicked() {
/*        try {
            // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
            if (outputStream != null) {
                outputStream.close();
            }

            // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
            //br.close();

            // 最终关闭整个Socket连接
            socket.close();

            // 判断客户端和服务器是否已经断开连接
            System.out.println(socket.isConnected());

        } catch (Exception e) {
            e.printStackTrace();
        }*/
        mSocketService.stop();
    }

    @OnClick(R.id.send)
    public void onSendClicked() {
/*        mThreadPool.execute(() -> {
            try {
                outputStream = socket.getOutputStream();
                if (outputStream != null) {
                    outputStream.write((edit.getText().toString() + "\n").getBytes("utf-8"));
                    outputStream.flush();
                    Log.d(TAG, "onSendClicked: " + edit.getText().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "onSendClicked: " + e.getMessage());
            }

        });*/
        try {
            //mSocketService.write((edit.getText().toString() + "\n").getBytes("utf-8"), null);
            //mSocketService.write((edit.getText().toString() + "\n").getBytes("utf-8"), ip.getText().toString());
            mSocketService.multiWrite((edit.getText().toString() + "\n").getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

/*    @OnClick(R.id.receive)
    public void onReceiveClicked() {
        mThreadPool.execute(() -> {

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                mMainHandler.obtainMessage(0, sb.toString()).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }*/
}
