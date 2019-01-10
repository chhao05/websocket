package com.cmos.websocket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.cmos.websocket.constant.Constant;
import com.cmos.websocket.init.BaseWebSocketServerHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * websocket 具体业务处理方法
 * 
 */
@Component
@Sharable
public class WebSocketServerHandler extends BaseWebSocketServerHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private WebSocketServerHandshaker handshaker;

    /**
     * 当客户端连接成功，返回个成功信息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("WebSocketServerHandler.channelActive:客户端连上了");
        // TODO websocket链接的消息接受不到
        push(ctx, "连接成功");
    }

    /**
     * 当客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("WebSocketServerHandler.channelInactive:客户端断开了");
        for (String key : Constant.PUSH_CTX_MAP.keySet()) {
            if (ctx.equals(Constant.PUSH_CTX_MAP.get(key))) {
                // 从连接池内剔除
                System.out.println(Constant.PUSH_CTX_MAP.size());
                System.out.println("剔除" + key);
                Constant.PUSH_CTX_MAP.remove(key);
                System.out.println(Constant.PUSH_CTX_MAP.size());
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("WebSocketServerHandler.channelReadComplete");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("WebSocketServerHandler.channelRead0");
        if (msg instanceof FullHttpRequest) {
            // http：//xxxx
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            // ws://xxxx
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        logger.info("WebSocketServerHandler.handlerWebSocketFrame");
        logger.info(frame.getClass().toString());
        // 关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            logger.info("WebSocketServerHandler.handlerWebSocketFrame.CloseWebSocketFrame");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // ping请求
        if (frame instanceof PingWebSocketFrame) {
            logger.info("WebSocketServerHandler.handlerWebSocketFrame.PingWebSocketFrame");
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 只支持文本格式，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            logger.info("WebSocketServerHandler.handlerWebSocketFrame.Exception");
            throw new Exception("仅支持文本格式");
        }
        logger.info("WebSocketServerHandler.handlerWebSocketFrame.TextWebSocketFrame");
        // 客服端发送过来的消息
        String request = ((TextWebSocketFrame) frame).text();
        logger.info("服务端收到：" + request);
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (jsonObject == null) {
            return;
        }
        if ("sign".equals(jsonObject.getString("type"))) {
            logger.info("添加到连接池：" + request);
            Constant.PUSH_CTX_MAP.put(request, ctx);
            Constant.aaChannelGroup.add(ctx.channel());
        } else {
            // 点发
            // push(ctx, request);
            // 群发（含自己）
            push(Constant.aaChannelGroup, request);
            // 群发（不含自己）
            // push(Constant.aaChannelGroup, request, ctx);
        }
    }

    // 第一次请求是http请求，请求头包括ws的信息
    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        logger.info("WebSocketServerHandler.handleHttpRequest");
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws:/" + ctx.channel() + "/websocket", null, false, Short.MAX_VALUE * 4);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            // 不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        logger.info("WebSocketServerHandler.sendHttpResponse");
        // 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        logger.info("WebSocketServerHandler.isKeepAlive");
        return false;
    }

    // 异常处理，netty默认是关闭channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("WebSocketServerHandler.exceptionCaught");
        // 输出日志
        logger.error("netty通信异常：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}