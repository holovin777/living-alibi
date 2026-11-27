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

/** Optional private headless server. The existing action-driven clock remains
 * active during client play; idle simulation is serialized with client requests.
 */
@Component
@ConditionalOnProperty(name="living-alibi.persistent-world",havingValue="true")
final class PersistentWorldClock extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(PersistentWorldClock.class);
    private static final long IDLE_NS=30_000_000_000L;
    private final GameSession session;
    private long lastRequest=System.nanoTime();
    private int inFlight=0;
    private boolean away=false;
    PersistentWorldClock(GameSession session){this.session=session;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        boolean client=request.getRequestURI().startsWith("/api/v1/")||request.getRequestURI().startsWith("/geo/");
        if(client)synchronized(this){lastRequest=System.nanoTime();inFlight++;away=false;}
        try{chain.doFilter(request,response);}
        finally{if(client)synchronized(this){inFlight--;lastRequest=System.nanoTime();}}
    }
    @Scheduled(fixedDelayString="${living-alibi.idle-tick-ms:10000}")
    synchronized void tick(){
        if(inFlight!=0||System.nanoTime()-lastRequest<IDLE_NS)return;
        try{
            session.persistentIdleTick(1,!away);
            away=true;
        }catch(Exception e){LOG.error("Persistent world tick failed; saved world retained",e);}
    }
}
