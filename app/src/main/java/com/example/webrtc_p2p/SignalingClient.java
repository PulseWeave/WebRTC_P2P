package com.example.webrtc_p2p;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import android.os.Handler;
import android.os.Looper;

public class SignalingClient {
    private static final String TAG = "SignalingClient";
    // 移除硬编码的服务器地址
    // private static final String SIGNALING_SERVER_URL = "ws://192.168.1.100:8080";
    
    private WebSocketClient webSocketClient;
    private SignalingCallback signalingCallback;
    private Gson gson;
    private boolean isConnected = false;
    private String serverIp = "192.168.0.105"; // 默认IP
    private int serverPort = 8081; // 默认端口

    public interface SignalingCallback {
        void onOfferReceived(String sdp);
        void onAnswerReceived(String sdp);
        void onIceCandidateReceived(String candidate);
        void onConnected();
        void onDisconnected();
    }

    public SignalingClient() {
        gson = new Gson();
    }

    public void setSignalingCallback(SignalingCallback callback) {
        this.signalingCallback = callback;
    }

    // 设置服务器IP地址
    public void setServerIp(String ip) {
        this.serverIp = ip;
    }

    // 设置服务器端口
    public void setServerPort(int port) {
        this.serverPort = port;
    }

    // 设置服务器地址（IP和端口）
    public void setServerAddress(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }
    
    public boolean isConnected() {
        return isConnected && webSocketClient != null && webSocketClient.isOpen();
    }

    public void connect() {
        if (isConnected && webSocketClient != null) {
            Log.d(TAG, "WebSocket 已连接，无需重复连接");
            return;
        }
        
        try {
            URI uri = new URI("ws://" + serverIp + ":" + serverPort);
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "WebSocket 连接已建立");
                    isConnected = true;
                    if (signalingCallback != null) {
                        signalingCallback.onConnected();
                    }
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "收到消息: " + message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket 连接已关闭: " + reason + " (code: " + code + ")");
                    isConnected = false;
                    if (signalingCallback != null) {
                        signalingCallback.onDisconnected();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket 错误: " + ex.getMessage());
                    isConnected = false;
                }
            };
            
            Log.d(TAG, "正在连接到: ws://" + serverIp + ":" + serverPort);
            webSocketClient.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "无效的 WebSocket URL: " + e.getMessage());
            isConnected = false;
        } catch (Exception e) {
            Log.e(TAG, "连接失败: " + e.getMessage());
            isConnected = false;
        }
    }

    public void disconnect() {
        if (webSocketClient != null && isConnected) {
            webSocketClient.close();
            webSocketClient = null;
            isConnected = false;
        }
    }

    public void sendOffer(String sdp) {
        if (!isConnected || webSocketClient == null) {
            Log.w(TAG, "WebSocket 未连接，尝试重新连接...");
            // 尝试重新连接
            connect();
            // 延迟发送，等待连接建立
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isConnected && webSocketClient != null) {
                    sendOfferInternal(sdp);
                } else {
                    Log.e(TAG, "重连失败，无法发送 Offer");
                }
            }, 1000);
            return;
        }
        
        sendOfferInternal(sdp);
    }
    
    private void sendOfferInternal(String sdp) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "offer");
        message.addProperty("sdp", sdp);
        
        String jsonMessage = gson.toJson(message);
        try {
            webSocketClient.send(jsonMessage);
            Log.d(TAG, "发送 Offer: " + jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "发送 Offer 失败: " + e.getMessage());
            // 发送失败时尝试重连
            isConnected = false;
            connect();
        }
    }

    public void sendAnswer(String sdp) {
        if (!isConnected || webSocketClient == null) {
            Log.w(TAG, "WebSocket 未连接，无法发送 Answer");
            return;
        }
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "answer");
        message.addProperty("sdp", sdp);
        
        String jsonMessage = gson.toJson(message);
        try {
            webSocketClient.send(jsonMessage);
            Log.d(TAG, "发送 Answer: " + jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "发送 Answer 失败: " + e.getMessage());
            isConnected = false;
        }
    }

    public void sendIceCandidate(String candidate) {
        if (!isConnected || webSocketClient == null) {
            Log.w(TAG, "WebSocket 未连接，无法发送 ICE Candidate");
            return;
        }
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "ice-candidate");
        message.addProperty("candidate", candidate);
        
        String jsonMessage = gson.toJson(message);
        try {
            webSocketClient.send(jsonMessage);
            Log.d(TAG, "发送 ICE Candidate: " + jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "发送 ICE Candidate 失败: " + e.getMessage());
            isConnected = false;
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            String type = jsonMessage.get("type").getAsString();
            
            switch (type) {
                case "offer":
                    if (signalingCallback != null) {
                        String sdp = jsonMessage.get("sdp").getAsString();
                        signalingCallback.onOfferReceived(sdp);
                    }
                    break;
                    
                case "answer":
                    if (signalingCallback != null) {
                        String sdp = jsonMessage.get("sdp").getAsString();
                        signalingCallback.onAnswerReceived(sdp);
                    }
                    break;
                    
                case "ice-candidate":
                    if (signalingCallback != null) {
                        String candidate = jsonMessage.get("candidate").getAsString();
                        signalingCallback.onIceCandidateReceived(candidate);
                    }
                    break;
                    
                default:
                    Log.w(TAG, "未知的消息类型: " + type);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析消息失败: " + e.getMessage());
        }
    }
} 