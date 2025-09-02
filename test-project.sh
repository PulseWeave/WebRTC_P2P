#!/bin/bash

echo "WebRTC P2P 项目结构验证"
echo "========================"

# 检查关键文件
echo "检查项目文件..."
files_to_check=(
    "build.gradle"
    "settings.gradle"
    "gradle.properties"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/example/webrtc_p2p/MainActivity.java"
    "app/src/main/java/com/example/webrtc_p2p/WebRTCManager.java"
    "app/src/main/java/com/example/webrtc_p2p/SignalingClient.java"
    "signaling-server/server.js"
    "signaling-server/package.json"
    "README.md"
)

missing_files=()
for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (缺失)"
        missing_files+=("$file")
    fi
done

echo ""
if [ ${#missing_files[@]} -eq 0 ]; then
    echo "✅ 所有文件都存在！"
else
    echo "❌ 以下文件缺失："
    for file in "${missing_files[@]}"; do
        echo "  - $file"
    done
fi

echo ""
echo "检查目录结构..."
echo "项目根目录: $(pwd)"
echo "Android 应用: app/"
echo "信令服务器: signaling-server/"
echo "Gradle 配置: gradle/"

echo ""
echo "下一步操作："
echo "1. 启动信令服务器: ./start-signaling-server.sh"
echo "2. 修改 SignalingClient.java 中的服务器地址"
echo "3. 使用 Android Studio 打开项目"
echo "4. 构建并运行应用" 