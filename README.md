# WebRTC P2P 音频传输 Demo

这是一个最简化的 Android WebRTC 音频流传输 Demo，专注于麦克风音频采集和 P2P 传输。


手机端（局域网 IP，如 192.168.1.100）
     |
     | WebSocket (信令，局域网服务器或直接 P2P)
     v
信令服务器（局域网内，可直接用局域网 IP）
     |
     | 直接 P2P 音频流（RTP/UDP）
     v
笔记本端（局域网 IP，如 192.168.1.101）


## 功能特性

- 🎤 麦克风音频采集
- 🔗 WebRTC P2P 连接
- 📡 简单信令服务器
- 📱 简洁的 Android UI
- 🚀 基于官方 WebRTC SDK

## 项目结构

```
WebRTC_P2P/
├── app/                          # Android 应用模块
│   ├── src/main/
│   │   ├── java/com/example/webrtc_p2p/
│   │   │   ├── MainActivity.java      # 主活动
│   │   │   ├── WebRTCManager.java     # WebRTC 管理
│   │   │   └── SignalingClient.java   # 信令客户端
│   │   ├── res/                       # 资源文件
│   │   └── AndroidManifest.xml        # 应用清单
│   └── build.gradle                   # 应用构建配置
├── signaling-server/              # 信令服务器
│   ├── server.js                  # WebSocket 服务器
│   └── package.json               # Node.js 依赖
├── build.gradle                   # 项目构建配置
├── settings.gradle                # 项目设置
└── gradle.properties              # Gradle 属性
```

## 快速开始

### 1. 启动信令服务器

```bash
cd signaling-server
npm install
npm start
```

服务器将在 `ws://localhost:8080` 启动。

### 2. 配置 Android 应用

在 `SignalingClient.java` 中修改信令服务器地址：

```java
private static final String SIGNALING_SERVER_URL = "ws://YOUR_IP:8080";
```

### 3. 构建并运行 Android 应用

```bash
./gradlew assembleDebug
```

## 使用说明

1. **启动应用**：安装并运行 Android 应用
2. **授予权限**：允许麦克风访问权限
3. **开始通话**：点击"开始通话"按钮
4. **建立连接**：应用将自动连接到信令服务器并建立 P2P 连接
5. **音频传输**：麦克风音频将通过 WebRTC 传输到远端

## 技术架构

### WebRTC 流程

1. **初始化**：创建 PeerConnectionFactory 和 PeerConnection
2. **音频采集**：创建 AudioSource 和 AudioTrack
3. **信令交换**：通过 WebSocket 交换 SDP 和 ICE 信息
4. **连接建立**：建立 P2P 连接
5. **音频传输**：开始音频流传输

### 信令协议

- `offer`: 发送 SDP Offer
- `answer`: 发送 SDP Answer  
- `ice-candidate`: 交换 ICE Candidate

## 依赖库

- **WebRTC**: `org.webrtc:google-webrtc:1.0.32006`
- **WebSocket**: `org.java-websocket:Java-WebSocket:1.5.3`
- **JSON**: `com.google.code.gson:gson:2.10.1`

## 注意事项

1. **网络配置**：确保设备在同一网络或可以相互访问
2. **权限设置**：需要麦克风录音权限
3. **防火墙**：确保 WebSocket 端口（8080）可访问
4. **NAT 穿透**：使用 STUN 服务器进行 NAT 穿透

## 故障排除

### 常见问题

1. **连接失败**：检查信令服务器地址和网络连接
2. **音频无声音**：确认麦克风权限已授予
3. **ICE 连接失败**：检查 STUN 服务器配置

### 调试信息

应用界面会显示详细的连接日志，包括：
- 连接状态变化
- SDP 交换过程
- ICE 连接状态
- 错误信息

## 扩展功能

可以基于此 Demo 扩展：
- 视频通话功能
- 多人通话
- 云端录制
- 音频处理（降噪、回声消除等）

## 许可证

MIT License 