package com.cmos.websocket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 发消息方式 抽象出来
 * 
 */
public abstract class BaseWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    /**
     * 推送单个
     * 
     */
    public static final void push(final ChannelHandlerContext ctx, final String message) {
        logger.info("BaseWebSocketServerHandler.push——》single");
        TextWebSocketFrame tws = new TextWebSocketFrame(message);
        ctx.channel().writeAndFlush(tws);
    }

    /**
     * 群发（含自己）
     * 
     */
    public static final void push(final ChannelGroup ctxGroup, final String message) {
        logger.info("BaseWebSocketServerHandler.push——》group");
        TextWebSocketFrame tws = new TextWebSocketFrame(message);
        ctxGroup.writeAndFlush(tws);
    }

    /**
     * 群发（不含自己）
     * 
     * @param ctxGroup
     * @param message
     * @param ctx
     */
    public static final void push(final ChannelGroup ctxGroup, final String message, ChannelHandlerContext ctx) {
        ctxGroup.remove(ctx.channel());
        push(ctxGroup, message);
        ctxGroup.add(ctx.channel());
    }
}