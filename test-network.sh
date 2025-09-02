#!/bin/bash

echo "🔍 网络连接诊断工具"
echo "=================="

# 获取本机IP地址
echo "📱 本机IP地址："
ifconfig | grep "inet " | grep -v 127.0.0.1 | head -1

echo ""
echo "🌐 检查8080端口状态："
if lsof -i :8080 > /dev/null 2>&1; then
    echo "✅ 端口8080正在监听"
    lsof -i :8080
else
    echo "❌ 端口8080未在监听"
fi

echo ""
echo "🔒 检查防火墙状态："
if command -v /usr/libexec/ApplicationFirewall/socketfilterfw > /dev/null; then
    echo "防火墙状态："
    /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
else
    echo "未找到防火墙工具"
fi

echo ""
echo "📡 测试本地连接："
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ 本地连接正常"
else
    echo "❌ 本地连接失败"
fi

echo ""
echo "💡 建议："
echo "1. 确保信令服务器正在运行"
echo "2. 检查防火墙设置"
echo "3. 确认手机和电脑在同一WiFi网络"
echo "4. 尝试使用电脑的IP地址而不是localhost" 