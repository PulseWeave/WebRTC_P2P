#!/bin/bash

echo "🚀 启动 WebRTC 电脑端客户端..."
echo ""

# 检查是否安装了Python3
if command -v python3 &> /dev/null; then
    echo "✅ 使用 Python3 启动 HTTP 服务器"
    echo "📱 请在手机端输入以下IP地址和端口："
    echo "   IP地址: $(hostname -I | awk '{print $1}')"
    echo "   端口: 8000"
    echo ""
    echo "🌐 电脑端访问地址: http://localhost:8000"
    echo "📋 按 Ctrl+C 停止服务器"
    echo ""
    
    cd web-client
    python3 -m http.server 8000
    
elif command -v python &> /dev/null; then
    echo "✅ 使用 Python 启动 HTTP 服务器"
    echo "📱 请在手机端输入以下IP地址和端口："
    echo "   IP地址: $(hostname -I | awk '{print $1}')"
    echo "   端口: 8000"
    echo ""
    echo "🌐 电脑端访问地址: http://localhost:8000"
    echo "📋 按 Ctrl+C 停止服务器"
    echo ""
    
    cd web-client
    python -m SimpleHTTPServer 8000
    
else
    echo "❌ 未找到 Python，请安装 Python3 或 Python"
    echo "   或者手动在 web-client 目录中启动 HTTP 服务器"
    exit 1
fi 