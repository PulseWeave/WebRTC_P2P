#!/bin/bash

echo "启动 WebRTC 信令服务器..."
echo "================================"

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo "错误: 未找到 Node.js，请先安装 Node.js"
    echo "访问 https://nodejs.org/ 下载安装"
    exit 1
fi

# 检查 npm 是否安装
if ! command -v npm &> /dev/null; then
    echo "错误: 未找到 npm，请先安装 npm"
    exit 1
fi

echo "Node.js 版本: $(node --version)"
echo "npm 版本: $(npm --version)"
echo ""

# 进入信令服务器目录
cd signaling-server

# 检查是否已安装依赖
if [ ! -d "node_modules" ]; then
    echo "安装依赖包..."
    npm install
fi

echo "启动信令服务器..."
echo "WebSocket 地址: ws://localhost:8080"
echo "按 Ctrl+C 停止服务器"
echo ""

# 启动服务器
npm start 