package scut.carson_ho.socket_carson;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    @BindView(R.id.server)
    Button server;
    @BindView(R.id.client)
    Button client;

    Fragment serverFragment;
    Fragment clientFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        serverFragment = new ServerFragment();
        clientFragment = new ClientFragment();
    }

    @OnClick(R.id.server)
    public void onServerClicked() {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, serverFragment).commit();
    }

    @OnClick(R.id.client)
    public void onClientClicked() {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, clientFragment).commit();
    }
}
