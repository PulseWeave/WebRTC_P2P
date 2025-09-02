package com.example.webrtc_p2p;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;

public class WebRTCManager {
    private static final String TAG = "WebRTCManager";
    
    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack audioTrack;
    private SignalingClient signalingClient;
    private StatusCallback statusCallback;
    
    private EglBase eglBase;
    private boolean isInitiator = false;
    private String remoteSessionDescription;
    private String remoteSessionDescriptionType;

    public interface StatusCallback {
        void onStatusChanged(String status);
        void onLog(String message);
    }

    public WebRTCManager(Context context) {
        this.context = context;
        initPeerConnectionFactory();
        initSignaling();
    }

    // 添加带IP和端口参数的构造函数
    public WebRTCManager(Context context, String serverIp, int serverPort) {
        this.context = context;
        initPeerConnectionFactory();
        initSignaling(serverIp, serverPort);
    }

    private void initPeerConnectionFactory() {
        eglBase = EglBase.create();
        
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory();
        
        log("PeerConnectionFactory 初始化完成");
    }

    private void initSignaling() {
        signalingClient = new SignalingClient();
        setupSignalingCallbacks();
    }

    private void initSignaling(String serverIp, int serverPort) {
        signalingClient = new SignalingClient();
        signalingClient.setServerAddress(serverIp, serverPort);
        setupSignalingCallbacks();
    }

    private void setupSignalingCallbacks() {
        signalingClient.setSignalingCallback(new SignalingClient.SignalingCallback() {
            @Override
            public void onOfferReceived(String sdp) {
                log("收到 Offer: " + sdp);
                handleRemoteDescription(sdp, "offer");
            }

            @Override
            public void onAnswerReceived(String sdp) {
                log("收到 Answer: " + sdp);
                handleRemoteDescription(sdp, "answer");
            }

            @Override
            public void onIceCandidateReceived(String candidate) {
                log("收到 ICE Candidate: " + candidate);
                handleRemoteIceCandidate(candidate);
            }

            @Override
            public void onConnected() {
                log("信令服务器连接成功");
                updateStatus("信令已连接");
            }

            @Override
            public void onDisconnected() {
                log("信令服务器断开连接");
                updateStatus("信令已断开");
            }
        });
    }

    public void setStatusCallback(StatusCallback callback) {
        this.statusCallback = callback;
    }

    public void connectToSignalingServer() {
        if (signalingClient != null) {
            signalingClient.connect();
        }
    }

    public void startCall() {
        log("开始建立通话...");
        
        // 检查信令连接状态
        if (signalingClient == null || !signalingClient.isConnected()) {
            log("信令服务器未连接，尝试重新连接...");
            if (signalingClient != null) {
                signalingClient.connect();
            }
            // 延迟启动通话，等待连接建立
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (signalingClient != null && signalingClient.isConnected()) {
                    startCallInternal();
                } else {
                    log("信令服务器连接失败，无法启动通话");
                }
            }, 2000);
            return;
        }
        
        startCallInternal();
    }
    
    private void startCallInternal() {
        try {
            // 创建 PeerConnection
            createPeerConnection();
            
            // 创建音频源
            audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
            audioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource);
            
            log("音频轨道创建完成");
            
            // 添加音频轨道到 PeerConnection（指定streamId）
            java.util.List<String> streamIds = java.util.Collections.singletonList("audio_stream");
            peerConnection.addTrack(audioTrack, streamIds);
            log("音频轨道已添加到 PeerConnection");
            
            // 创建 Offer
            peerConnection.createOffer(new SimpleSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    log("创建 Offer 成功");
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            log("设置本地描述成功");
                            // 发送 Offer 到信令服务器
                            if (signalingClient != null) {
                                signalingClient.sendOffer(sdp.description);
                            }
                        }
                    }, sdp);
                }
            }, new MediaConstraints());
            
        } catch (Exception e) {
            log("启动通话失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopCall() {
        log("停止通话");
        updateStatus("已断开");
        
        if (signalingClient != null) {
            signalingClient.disconnect();
        }
        
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        
        if (audioTrack != null) {
            audioTrack.dispose();
            audioTrack = null;
        }
        
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                log("本地 ICE Candidate: " + candidate.sdp);
                signalingClient.sendIceCandidate(candidate.sdp);
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                log("连接状态变化: " + newState);
                switch (newState) {
                    case CONNECTED:
                        updateStatus("已连接");
                        break;
                    case DISCONNECTED:
                        updateStatus("连接断开");
                        break;
                    case FAILED:
                        updateStatus("连接失败");
                        break;
                }
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState newState) {
                log("信令状态变化: " + newState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
                log("ICE 连接状态变化: " + newState);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
                log("ICE 收集状态变化: " + newState);
            }

            @Override
            public void onAddStream(MediaStream stream) {
                log("收到远程媒体流");
            }

            @Override
            public void onRemoveStream(MediaStream stream) {
                log("移除远程媒体流");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                log("收到数据通道");
            }

            @Override
            public void onRenegotiationNeeded() {
                log("需要重新协商");
            }

            @Override
            public void onAddTrack(org.webrtc.RtpReceiver receiver, MediaStream[] mediaStreams) {
                log("收到远程音频轨道");
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {
                log("ICE Candidates 已移除");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {
                log("ICE 连接接收状态变化: " + receiving);
            }
        });
    }

    private void createAudioTrack() {
        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule();
        
        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        audioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource);
        
        // 启用音频采集
        // 注意：adaptOutputFormat 方法在新版本的 WebRTC 中可能已被移除
        // 音频格式配置现在通过其他方式处理
        
        log("音频轨道创建完成");
    }

    private void addAudioTrackToPeerConnection() {
        if (peerConnection != null && audioTrack != null) {
            java.util.List<String> streamIds = java.util.Collections.singletonList("audio_stream");
            peerConnection.addTrack(audioTrack, streamIds);
            log("音频轨道已添加到 PeerConnection");
        }
    }

    private void createOffer() {
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                log("创建 Offer 成功");
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        log("设置本地描述成功");
                        signalingClient.sendOffer(sdp.description);
                    }
                }, sdp);
            }
        }, new MediaConstraints());
    }

    private void createAnswer() {
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                log("创建 Answer 成功");
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        log("设置本地描述成功");
                        signalingClient.sendAnswer(sdp.description);
                    }
                }, sdp);
            }
        }, new MediaConstraints());
    }

    private void handleRemoteDescription(String sdp, String type) {
        if (peerConnection == null) {
            log("PeerConnection 未初始化");
            return;
        }

        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), sdp);

        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                log("设置远程描述成功");
                if (type.equals("offer")) {
                    // 收到 Offer，创建 Answer
                    createAnswer();
                }
            }
        }, sessionDescription);
    }

    private void handleRemoteIceCandidate(String candidate) {
        if (peerConnection == null) {
            log("PeerConnection 未初始化");
            return;
        }

        try {
            // 我们的信令仅发送了candidate字符串，SDP里a=mid:0，因此直接使用mid="0"，mLineIndex=0
            String sdpMid = "0";
            int sdpMLineIndex = 0;

            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            peerConnection.addIceCandidate(iceCandidate);
            log("添加远程 ICE Candidate 成功");
        } catch (Exception e) {
            log("解析/添加 ICE Candidate 失败: " + e.getMessage());
        }
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.onStatusChanged(status);
        }
    }

    private void log(String message) {
        Log.d(TAG, message);
        if (statusCallback != null) {
            statusCallback.onLog(message);
        }
    }

    public void cleanup() {
        stopCall();
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    // 简化的 SDP Observer
    private static class SimpleSdpObserver implements org.webrtc.SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sdp) {}

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "SDP 创建失败: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "SDP 设置失败: " + s);
        }
    }
} 