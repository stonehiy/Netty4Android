package com.james.netty4android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.james.nettylib.NettyService;
import com.james.nettylib.netty.ConnectionManager;
import com.james.nettylib.netty.NettyClient;
import com.james.nettylib.netty.NettyListener;
import com.james.nettylib.netty.Request;
import com.james.nettylib.netty.ResponseListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements ConnectionManager.ConnectionListener {
    private EditText mEditText;
    private Button mLoginBtn;
    private TextView mResultTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Intent intent = new Intent(getApplication(), NettyService.class);
        startService(intent);
        mEditText = (EditText) findViewById(R.id.et_content);
        mResultTextView = (TextView) findViewById(R.id.resultTextView);
        mLoginBtn = (Button) findViewById(R.id.btn);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        ConnectionManager.getInstance().registerListener(this);
        mEditText.setText("DT109668");
    }

    /**
     * 发送消息测试
     */
    private void sendMessage() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("version", "1.01");
        params.put("userid", String.valueOf(mEditText.getText()));
        params.put("sessionid", "60010000007309e0bf043f0186000839");
        NettyClient.getInstance().sendMessage(new Request("sessionlogin", params, new ResponseListener() {

            @Override
            public void onSuccess(String data) {

            }

            @Override
            public void onFail(int errCode) {

            }
        }), null);
    }

    /**
     * 监听连接状态
     */
    @Override
    public void onConnectionStatusChange(int status) {
        if (status == NettyListener.STATUS_CONNECT_SUCCESS) {
            Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
        } else if (status == NettyListener.STATUS_CONNECT_CLOSED) {
            Toast.makeText(getApplicationContext(), "连接关闭", Toast.LENGTH_SHORT).show();
        } else if (status == NettyListener.STATUS_CONNECT_ERROR) {
            Toast.makeText(getApplicationContext(), "连接错误", Toast.LENGTH_SHORT).show();
        } else if (status == NettyListener.STATUS_CONNECT_RECONNECT) {
            Toast.makeText(getApplicationContext(), "正在重连", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectionManager.getInstance().unregisterListener(this);
    }
}