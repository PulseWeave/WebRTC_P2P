package com.example.webrtc_p2p;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView tvStatus;
    private TextView tvLog;
    private Button btnCall;
    private Button btnConnect;
    private EditText etIp;
    private EditText etPort;
    private TextView tvConnectionStatus;
    private WebRTCManager webRTCManager;
    private boolean isCallActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initWebRTC();
        checkPermissions();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvLog = findViewById(R.id.tv_log);
        btnCall = findViewById(R.id.btn_call);
        btnConnect = findViewById(R.id.btn_connect);
        etIp = findViewById(R.id.et_ip);
        etPort = findViewById(R.id.et_port);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        
        btnCall.setOnClickListener(v -> toggleCall());
        btnConnect.setOnClickListener(v -> connectToServer());
        
        // 延迟显示项目状态，确保TextView布局完全初始化
        tvLog.post(() -> {
            appendLog("WebRTC P2P 音频传输 Demo");
            appendLog("项目已成功创建，WebRTC 功能已启用");
            appendLog("请输入服务器IP地址和端口，然后点击连接");
        });
    }

    private void initWebRTC() {
        // 获取用户输入的IP地址和端口
        String serverIp = etIp.getText().toString().trim();
        int serverPort = 8081; // 默认端口
        
        try {
            String portText = etPort.getText().toString().trim();
            if (!portText.isEmpty()) {
                serverPort = Integer.parseInt(portText);
            }
        } catch (NumberFormatException e) {
            appendLog("端口格式错误，使用默认端口8081");
        }
        
        // 验证IP地址格式
        if (serverIp.isEmpty()) {
            serverIp = "192.168.0.105"; // 使用默认IP
            appendLog("IP地址为空，使用默认IP: " + serverIp);
        }
        
        appendLog("连接服务器: " + serverIp + ":" + serverPort);
        
        webRTCManager = new WebRTCManager(this, serverIp, serverPort);
        webRTCManager.setStatusCallback(new WebRTCManager.StatusCallback() {
            @Override
            public void onStatusChanged(String status) {
                runOnUiThread(() -> {
                    tvStatus.setText(status);
                    // 更新连接状态显示
                    if (status.contains("已连接")) {
                        tvConnectionStatus.setText("已连接");
                        tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else if (status.contains("已断开")) {
                        tvConnectionStatus.setText("未连接");
                        tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else if (status.contains("连接中")) {
                        tvConnectionStatus.setText("连接中...");
                        tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    }
                });
            }

            @Override
            public void onLog(String message) {
                runOnUiThread(() -> appendLog(message));
            }
        });
        
        // 自动连接信令服务器
        appendLog("正在自动连接信令服务器...");
        webRTCManager.connectToSignalingServer();
    }

    private void connectToServer() {
        if (webRTCManager != null) {
            appendLog("正在连接信令服务器...");
            webRTCManager.connectToSignalingServer();
        } else {
            appendLog("WebRTC管理器未初始化，请先初始化");
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void toggleCall() {
        if (!isCallActive) {
            startCall();
        } else {
            stopCall();
        }
    }

    private void startCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (webRTCManager != null) {
            webRTCManager.startCall();
            btnCall.setText(R.string.stop_call);
            isCallActive = true;
        }
    }

    private void stopCall() {
        if (webRTCManager != null) {
            webRTCManager.stopCall();
            btnCall.setText(R.string.start_call);
            isCallActive = false;
        }
    }

    private void appendLog(String message) {
        if (tvLog == null) return;
        
        String currentLog = tvLog.getText().toString();
        String newLog = currentLog + "\n" + message;
        tvLog.setText(newLog);
        
        // 自动滚动到底部 - 添加空值检查
        if (tvLog.getLayout() != null) {
            final int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
            if (scrollAmount > 0) {
                tvLog.scrollTo(0, scrollAmount);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("麦克风权限已授予");
            } else {
                appendLog("麦克风权限被拒绝");
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webRTCManager != null) {
            webRTCManager.cleanup();
        }
    }
} 