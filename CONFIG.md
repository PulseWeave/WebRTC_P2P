# 项目配置说明

## 快速配置步骤

### 1. 信令服务器配置

#### 启动信令服务器
```bash
# 方法1: 使用启动脚本（推荐）
./start-signaling-server.sh

# 方法2: 手动启动
cd signaling-server
npm install
npm start
```

#### 修改服务器地址
如果您的设备不在同一网络，需要修改 `SignalingClient.java` 中的服务器地址：

```java
// 修改为您的实际 IP 地址
private static final String SIGNALING_SERVER_URL = "ws://YOUR_IP:8080";
```

**获取本机 IP 地址：**
- macOS/Linux: `ifconfig` 或 `ip addr`
- Windows: `ipconfig`

### 2. Android 应用配置

#### 使用 Android Studio 打开项目
1. 启动 Android Studio
2. 选择 "Open an existing Android Studio project"
3. 选择项目根目录 `WebRTC_P2P/`
4. 等待 Gradle 同步完成

#### 配置设备
1. 连接 Android 设备或启动模拟器
2. 确保设备已启用开发者选项和 USB 调试
3. 在 Android Studio 中选择目标设备

#### 构建和运行
1. 点击 "Run" 按钮（绿色三角形）
2. 选择目标设备
3. 等待应用安装和启动

### 3. 网络配置

#### 防火墙设置
确保端口 8080 可访问：
- macOS: 系统偏好设置 → 安全性与隐私 → 防火墙
- Windows: 控制面板 → 系统和安全 → Windows Defender 防火墙
- Linux: `sudo ufw allow 8080`

#### 网络测试
测试 WebSocket 连接：
```bash
# 使用 curl 测试（需要支持 WebSocket）
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==" http://localhost:8080
```

### 4. 权限配置

#### Android 权限
应用会自动请求以下权限：
- `RECORD_AUDIO`: 麦克风录音权限
- `INTERNET`: 网络访问权限
- `ACCESS_NETWORK_STATE`: 网络状态访问权限

#### 运行时权限
首次运行时，应用会请求麦克风权限。如果被拒绝，可以在系统设置中手动授予。

### 5. 故障排除

#### 常见问题

**1. 信令服务器连接失败**
- 检查服务器是否正在运行
- 确认 IP 地址和端口正确
- 检查防火墙设置

**2. WebRTC 连接失败**
- 确认 STUN 服务器可访问
- 检查网络 NAT 类型
- 查看应用日志输出

**3. 音频无声音**
- 确认麦克风权限已授予
- 检查设备音频设置
- 测试麦克风是否正常工作

#### 调试信息
应用界面会显示详细的连接日志，包括：
- 连接状态变化
- SDP 交换过程
- ICE 连接状态
- 错误信息

### 6. 性能优化

#### 音频质量设置
可以在 `WebRTCManager.java` 中调整音频参数：

```java
// 音频采样率设置
audioSource.adaptOutputFormat(16000, 1);  // 16kHz, 单声道
audioSource.adaptOutputFormat(32000, 1);  // 32kHz, 单声道
audioSource.adaptOutputFormat(48000, 1);  // 48kHz, 单声道
```

#### 网络优化
- 使用更近的 STUN 服务器
- 配置 TURN 服务器（如果需要）
- 优化 ICE 收集策略

### 7. 安全考虑

#### 生产环境
- 使用 HTTPS/WSS 加密通信
- 实现用户认证和授权
- 添加速率限制和 DDoS 防护
- 使用强密码和证书

#### 开发环境
- 仅在开发网络中使用
- 定期更新依赖库
- 监控网络流量和日志

## 下一步

配置完成后，您可以：
1. 测试基本的音频通话功能
2. 扩展为视频通话
3. 添加多人通话支持
4. 集成到现有应用中

如有问题，请查看应用日志或联系技术支持。 