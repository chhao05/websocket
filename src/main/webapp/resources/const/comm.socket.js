var socketHost = 'ws://localhost:7397';

var socket;
if (window.WebSocket) {
    socket = new WebSocket(socketHost);

    socket.onopen = function(event) {
    	console.log('WebSocket is open...............')
        socketOpen();
    };

    socket.onmessage = function(event) {
    	console.log('Receive：'+event.data);
    	var data = JSON.parse(event.data);
    	socketAccept(data);
    };
    
    socket.onclose = function(event) {
        console.log('WebSocket is closed.............')
    };
    
} else {
    console.warn("WebSocket is not support!");
}

/**
 * 发送socket消息
 * @param msgType sign 签入，text 文本，img 图片
 * @param msgContent
 * @returns
 */
function socketSend(msgType, msgContent) {
    var data = {
        "id": id,
        "type": msgType,
        "info": msgContent
    };
    socket.send(JSON.stringify(data));
}