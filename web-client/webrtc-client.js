// WebRTC P2P 语音接收端客户端
class WebRTCClient {
    constructor() {
        this.websocket = null;
        this.peerConnection = null;
        this.remoteStream = null;
        this.audioElement = null;
        this.isConnected = false;
        this.isMuted = false;
        this.mediaTimeout = null;
        
        this.initAudioElement();
        this.initVolumeControl();
        this.log("WebRTC 客户端已初始化");
    }

    // 初始化音频元素
    initAudioElement() {
        this.audioElement = document.createElement('audio');
        this.audioElement.autoplay = true;
        this.audioElement.controls = false;
        this.audioElement.playsInline = true;
        document.body.appendChild(this.audioElement);
    }

    // 初始化音量控制
    initVolumeControl() {
        const volumeSlider = document.getElementById('volumeSlider');
        const volumeValue = document.getElementById('volumeValue');
        
        volumeSlider.addEventListener('input', (e) => {
            const volume = e.target.value / 100;
            this.audioElement.volume = volume;
            volumeValue.textContent = e.target.value + '%';
        });
    }

    // 连接到信令服务器
    connectToServer() {
        const serverIp = document.getElementById('serverIp').value.trim();
        const serverPort = document.getElementById('serverPort').value.trim();
        
        if (!serverIp || !serverPort) {
            this.log("请输入服务器IP地址和端口");
            return;
        }

        const wsUrl = `ws://${serverIp}:${serverPort}`;
        this.log(`正在连接到信令服务器: ${wsUrl}`);
        
        this.updateConnectionStatus('connecting', '连接中...');
        
        try {
            this.websocket = new WebSocket(wsUrl);
            
            this.websocket.onopen = () => {
                this.log("已连接到信令服务器");
                this.updateConnectionStatus('connected', '已连接');
                this.isConnected = true;
                document.getElementById('connectBtn').disabled = true;
                // 准备 PeerConnection（收端）
                this.preparePeerConnection();
            };
            
            this.websocket.onmessage = (event) => {
                this.handleSignalingMessage(event.data);
            };
            
            this.websocket.onclose = () => {
                this.log("信令服务器连接已断开");
                this.updateConnectionStatus('disconnected', '未连接');
                this.isConnected = false;
                document.getElementById('connectBtn').disabled = false;
            };
            
            this.websocket.onerror = (error) => {
                this.log("连接错误: " + error);
                this.updateConnectionStatus('disconnected', '连接失败');
                this.isConnected = false;
                document.getElementById('connectBtn').disabled = false;
            };
            
        } catch (error) {
            this.log("连接失败: " + error.message);
            this.updateConnectionStatus('disconnected', '连接失败');
        }
    }

    // 准备接收端的PeerConnection
    preparePeerConnection() {
        // 如果已有PC则先关闭
        if (this.peerConnection) {
            try { this.peerConnection.close(); } catch { /* noop */ }
            this.peerConnection = null;
        }

        this.peerConnection = new RTCPeerConnection({
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' }
            ]
        });

        // 明确声明接收端音频transceiver，避免某些浏览器不触发ontrack
        try {
            this.peerConnection.addTransceiver('audio', { direction: 'recvonly' });
        } catch (e) {
            this.log('addTransceiver 不支持或失败: ' + e.message);
        }

        // 监听远程轨道
        this.peerConnection.ontrack = (event) => {
            this.log("收到远程音频轨道");
            this.remoteStream = event.streams[0] || new MediaStream([event.track]);
            this.audioElement.srcObject = this.remoteStream;
            document.getElementById('muteBtn').disabled = false;
            this.tryAutoPlay();
        };

        // 监听ICE候选并回传给对端
        this.peerConnection.onicecandidate = (event) => {
            if (event.candidate) {
                this.sendSignalingMessage({
                    type: 'ice-candidate',
                    candidate: event.candidate.candidate
                });
            }
        };

        this.peerConnection.onconnectionstatechange = () => {
            this.log(`连接状态: ${this.peerConnection.connectionState}`);
        };

        // 如果长时间没有收到媒体，提示用户
        clearTimeout(this.mediaTimeout);
        this.mediaTimeout = setTimeout(() => {
            if (!this.remoteStream) {
                this.log('未收到远程媒体，请确认手机端已开始通话且网络可达');
            }
        }, 8000);
    }

    // 处理信令消息
    handleSignalingMessage(message) {
        try {
            const data = JSON.parse(message);
            this.log(`收到信令消息: ${data.type}`);
            
            switch (data.type) {
                case 'offer':
                    this.handleOffer(data.sdp);
                    break;
                case 'answer':
                    this.handleAnswer(data.sdp);
                    break;
                case 'ice-candidate':
                    this.handleIceCandidate(data.candidate);
                    break;
                case 'connection':
                    this.log(`服务器分配的客户端ID: ${data.clientId}`);
                    break;
                default:
                    this.log(`未知消息类型: ${data.type}`);
            }
        } catch (error) {
            this.log("解析信令消息失败: " + error.message);
        }
    }

    // 处理Offer
    async handleOffer(sdp) {
        this.log("收到通话请求，准备应答...");
        
        try {
            if (!this.peerConnection) this.preparePeerConnection();

            await this.peerConnection.setRemoteDescription({ type: 'offer', sdp });
            const answer = await this.peerConnection.createAnswer();
            await this.peerConnection.setLocalDescription(answer);

            this.sendSignalingMessage({ type: 'answer', sdp: answer.sdp });
            this.log("已发送Answer");
        } catch (error) {
            this.log("处理Offer失败: " + error.message);
        }
    }

    // 处理Answer（理论上网页端为接收方通常不会走到）
    async handleAnswer(sdp) {
        try {
            if (this.peerConnection) {
                await this.peerConnection.setRemoteDescription({ type: 'answer', sdp });
                this.log("已设置远程应答");
            }
        } catch (error) {
            this.log("设置远程应答失败: " + error.message);
        }
    }

    // 处理远端ICE
    async handleIceCandidate(candidate) {
        if (!this.peerConnection) return;
        try {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate({
                candidate: candidate,
                sdpMid: '0',
                sdpMLineIndex: 0
            }));
            this.log("已添加远端ICE");
        } catch (error) {
            this.log("添加远端ICE失败: " + error.message);
        }
    }

    // 自动播放尝试，若失败则显示“允许播放”按钮
    tryAutoPlay() {
        const resumeBtn = document.getElementById('resumeAudioBtn');
        const play = () => this.audioElement.play().then(() => {
            resumeBtn.style.display = 'none';
            this.log('音频自动播放成功');
        }).catch((e) => {
            this.log('自动播放被阻止: ' + e.message);
            resumeBtn.style.display = 'inline-block';
        });
        play();
    }

    resumeAudio() {
        this.audioElement.play().then(() => {
            document.getElementById('resumeAudioBtn').style.display = 'none';
            this.log('已允许播放');
        }).catch((e) => this.log('播放失败: ' + e.message));
    }

    // 发送信令消息
    sendSignalingMessage(message) {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            this.websocket.send(JSON.stringify(message));
            this.log(`发送信令消息: ${message.type}`);
        } else {
            this.log("WebSocket未连接，无法发送消息");
        }
    }

    // 切换静音状态
    toggleMute() {
        if (this.audioElement) {
            this.isMuted = !this.isMuted;
            this.audioElement.muted = this.isMuted;
            const muteBtn = document.getElementById('muteBtn');
            muteBtn.textContent = this.isMuted ? '取消静音' : '静音';
            this.log(this.isMuted ? '已静音' : '已取消静音');
        }
    }

    // 更新连接状态
    updateConnectionStatus(status, text) {
        const statusElement = document.getElementById('connectionStatus');
        statusElement.className = `status ${status}`;
        statusElement.textContent = text;
    }

    // 记录日志
    log(message) {
        const logElement = document.getElementById('log');
        const timestamp = new Date().toLocaleTimeString();
        logElement.textContent += `[${timestamp}] ${message}\n`;
        logElement.scrollTop = logElement.scrollHeight;
        console.log(message);
    }

    // 断开连接
    disconnect() {
        if (this.websocket) this.websocket.close();
        if (this.peerConnection) this.peerConnection.close();
        this.isConnected = false;
        this.updateConnectionStatus('disconnected', '未连接');
        document.getElementById('connectBtn').disabled = false;
        document.getElementById('muteBtn').disabled = true;
        this.log("已断开连接");
    }
}

// 全局变量
let webrtcClient = null;

document.addEventListener('DOMContentLoaded', () => {
    webrtcClient = new WebRTCClient();
});

function connectToServer() { webrtcClient && webrtcClient.connectToServer(); }
function toggleMute() { webrtcClient && webrtcClient.toggleMute(); }
function resumeAudio() { webrtcClient && webrtcClient.resumeAudio(); }

window.addEventListener('beforeunload', () => { webrtcClient && webrtcClient.disconnect(); }); 