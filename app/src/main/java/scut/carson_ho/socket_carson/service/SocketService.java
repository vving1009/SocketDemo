package scut.carson_ho.socket_carson.service;

public interface SocketService {

    int PORT = 8191;

    interface ReceiveMessageListener {
        void onReceived(String message);
    }

    void setReceiveMessageListener(ReceiveMessageListener receiveMessageListener);

    /**
     * Return the current connection state.
     */
    int getState();

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    void start();

    void connect(String ip, int port);

    /**
     * Stop all threads
     */
    void stop();

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see TcpService.ConnectedThread#write(byte[])
     */
    void write(byte[] out);
}
