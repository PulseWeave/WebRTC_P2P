const WebSocket = require('ws');
const http = require('http');

const server = http.createServer();
const wss = new WebSocket.Server({ server });

console.log('WebRTC 信令服务器启动中...');

// 存储连接的客户端
const clients = new Map();
let clientIdCounter = 0;

wss.on('connection', (ws, req) => {
    const clientId = ++clientIdCounter;
    clients.set(clientId, ws);
    
    console.log(`客户端 ${clientId} 已连接 (${req.socket.remoteAddress})`);
    console.log(`当前连接数: ${clients.size}`);
    
    // 发送连接确认
    ws.send(JSON.stringify({
        type: 'connection',
        clientId: clientId,
        message: '连接成功'
    }));
    
    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            console.log(`客户端 ${clientId} 发送: ${data.type}`);
            
            // 广播消息给其他客户端
            broadcastToOthers(clientId, data);
            
        } catch (error) {
            console.error('解析消息失败:', error);
        }
    });
    
    ws.on('close', () => {
        clients.delete(clientId);
        console.log(`客户端 ${clientId} 已断开连接`);
        console.log(`当前连接数: ${clients.size}`);
    });
    
    ws.on('error', (error) => {
        console.error(`客户端 ${clientId} 错误:`, error);
        clients.delete(clientId);
    });
});

function broadcastToOthers(senderId, message) {
    clients.forEach((client, id) => {
        if (id !== senderId && client.readyState === WebSocket.OPEN) {
            try {
                client.send(JSON.stringify(message));
            } catch (error) {
                console.error(`发送消息到客户端 ${id} 失败:`, error);
            }
        }
    });
}

const PORT = process.env.PORT || 8081;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`信令服务器运行在端口 ${PORT}`);
    console.log(`WebSocket 地址: ws://0.0.0.0:${PORT}`);
    console.log(`本地访问: ws://localhost:${PORT}`);
    console.log(`局域网访问: ws://您的IP地址:${PORT}`);
});

// 优雅关闭
process.on('SIGINT', () => {
    console.log('\n正在关闭服务器...');
    wss.close(() => {
        console.log('WebSocket 服务器已关闭');
        server.close(() => {
            console.log('HTTP 服务器已关闭');
            process.exit(0);
        });
    });
}); 