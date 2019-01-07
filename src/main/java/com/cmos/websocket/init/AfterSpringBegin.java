package com.cmos.websocket.init;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.cmos.websocket.server.WebSocketServer;

/**
 * 
 * spring加载后改方法的子类
 */
public abstract class AfterSpringBegin extends TimerTask implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info(">>>>>>>>>>>AfterSpringBegin");
        // if (event.getApplicationContext().getParent() == null) {
        // logger.info(">>>>>>>>>>>AfterSpringBeginSchedule");
        // }
        new Timer().schedule(this, 0);
    }
}
