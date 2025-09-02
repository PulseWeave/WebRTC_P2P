#!/bin/bash

echo "🔍 详细网络连接诊断"
echo "=================="

# 获取本机IP地址
LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | head -1 | awk '{print $2}')
echo "📱 本机IP地址: $LOCAL_IP"

echo ""
echo "🌐 检查8080端口详细状态："
if lsof -i :8080 > /dev/null 2>&1; then
    echo "✅ 端口8080正在监听"
    lsof -i :8080
else
    echo "❌ 端口8080未在监听"
    exit 1
fi

echo ""
echo "🔌 测试WebSocket连接："
echo "正在测试 ws://$LOCAL_IP:8080..."

# 使用netcat测试端口连通性
if nc -z $LOCAL_IP 8080 2>/dev/null; then
    echo "✅ 端口8080可以访问"
else
    echo "❌ 端口8080无法访问"
fi

echo ""
echo "📡 测试HTTP连接："
if curl -s --connect-timeout 5 http://$LOCAL_IP:8080 > /dev/null 2>&1; then
    echo "✅ HTTP连接成功"
else
    echo "❌ HTTP连接失败"
fi

echo ""
echo "🌍 测试本地回环："
if curl -s --connect-timeout 5 http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ 本地回环连接成功"
else
    echo "❌ 本地回环连接失败"
fi

echo ""
echo "📱 网络接口信息："
ifconfig | grep -A 1 "inet " | grep -v 127.0.0.1

echo ""
echo "💡 下一步操作："
echo "1. 在电脑浏览器中访问: http://localhost:8000/test-websocket.html"
echo "2. 在手机浏览器中访问: http://$LOCAL_IP:8000/test-websocket.html"
echo "3. 点击'测试连接'按钮"
echo "4. 查看连接日志"

echo ""
echo "🔧 如果连接失败，请检查："
echo "- 信令服务器是否正在运行"
echo "- 防火墙设置"
echo "- 路由器设置"
echo "- 手机和电脑是否在同一网络" 