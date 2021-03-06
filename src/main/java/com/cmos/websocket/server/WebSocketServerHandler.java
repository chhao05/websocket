package com.cmos.websocket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
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

    private StringBuffer prexfix = new StringBuffer();

    /**
     * 当客户端连接成功，返回个成功信息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("......................channelActive");
        Constant.aaChannelGroup.add(ctx.channel());
    }

    /**
     * 当客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("......................channelInactive");
        for (String key : Constant.PUSH_CTX_MAP.keySet()) {
            if (ctx.equals(Constant.PUSH_CTX_MAP.get(key))) {
                // 从连接池内剔除
                Constant.PUSH_CTX_MAP.remove(key);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("......................channelReadComplete");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("......................channelRead0");
        if (msg instanceof FullHttpRequest) {
            // http：//xxxx
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            // ws://xxxx
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        logger.info("......................" + frame.getClass().getSimpleName());
        // 关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // ping请求
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 文本消息
        if (frame instanceof TextWebSocketFrame) {
            String info = ((TextWebSocketFrame) frame).text();
            logger.info("接受到消息：" + info);
            if (info.endsWith("}")) {
                prexfix = null;
                push(Constant.aaChannelGroup, info);
            } else {
                prexfix = new StringBuffer(info);
            }
            return;
        }
        // 未完结的消息
        logger.info("遗留消息:" + prexfix.toString());
        if (frame instanceof ContinuationWebSocketFrame) {
            String info = ((ContinuationWebSocketFrame) frame).text();
            logger.info("接受到消息：" + info);
            prexfix.append(info);
            if (info.endsWith("}")) {
                info = prexfix.toString();
                push(Constant.aaChannelGroup, info);
            }
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
                "ws:/" + ctx.channel() + "/websocket", null, false, Short.MAX_VALUE * 6);
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
        // 输出日志
        logger.error("netty通信异常：", cause);
        ctx.close();
    }
}