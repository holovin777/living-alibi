package com.holovin.npclife;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Private single-player VPS clock: 24 game hours per real hour.
 * Existing synchronous actions may advance time early; their elapsed minutes
 * count against the target rather than being added to it a second time.
 */
@Component
@ConditionalOnProperty(name="living-alibi.persistent-world",havingValue="true")
final class PersistentWorldClock extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(PersistentWorldClock.class);
    static final long NANOS_PER_GAME_MINUTE=2_500_000_000L;
    private static final long IDLE_NS=30_000_000_000L;
    private final GameSession session;
    private final long originMinute,originNanos;
    private long lastRequest;
    private int inFlight=0;
    private boolean away=false;
    PersistentWorldClock(GameSession session){
        this.session=session;
        originMinute=session.persistentMinute();
        originNanos=System.nanoTime();
        lastRequest=originNanos;
    }
    static long target(long startMinute,long elapsedNanos){
        return startMinute+Math.max(0,elapsedNanos)/NANOS_PER_GAME_MINUTE;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        boolean client=request.getRequestURI().startsWith("/api/v1/")||request.getRequestURI().startsWith("/geo/");
        if(client)synchronized(this){lastRequest=System.nanoTime();inFlight++;away=false;}
        try{chain.doFilter(request,response);}
        finally{if(client)synchronized(this){inFlight--;lastRequest=System.nanoTime();}}
    }
    @Scheduled(fixedDelayString="${living-alibi.clock-poll-ms:500}")
    synchronized void tick(){
        if(inFlight!=0)return;
        long now=System.nanoTime();
        boolean offline=now-lastRequest>=IDLE_NS;
        try{
            session.persistentTickTo(target(originMinute,now-originNanos),offline&&!away);
            away=offline;
        }catch(Exception e){LOG.error("Persistent world tick failed; saved world retained",e);}
    }
}
