package com.holovin.npclife;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/geo")
@ConditionalOnProperty(name="living-alibi.geo-graph")
public final class GeoController {
    private final GameSession session;
    private final GeoProjection map;
    private long lastTick=System.nanoTime();
    GeoController(GameSession session,@Value("${living-alibi.geo-graph}") String graph)throws Exception{
        this.session=session;this.map=new GeoProjection(Path.of(graph));
    }
    static void validate(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>100000||Math.abs(z)>100000)
            throw new IllegalArgumentException("Invalid map coordinates");
    }
    @GetMapping("/nearby")
    public synchronized GeoProjection.View nearby(@RequestParam(name="x",defaultValue="0") double x,@RequestParam(name="z",defaultValue="0") double z)throws SQLException{
        validate(x,z);
        long now=System.nanoTime();int minutes=(int)Math.min(3,(now-lastTick)/5_000_000_000L);
        if(minutes>0){lastTick=now;session.geoTick(minutes);}
        return session.geoNearby(map,x,z);
    }
    public record Enter(String destination,double x,double z){}
    public record Speak(String id,String message,String language,double x,double z){}
    @PostMapping("/enter")
    public Map<String,String> enter(@RequestBody Enter r)throws SQLException{
        validate(r.x,r.z);session.geoEnter(map,r.destination,r.x,r.z);
        return Map.of("message","District synchronized.");
    }
    @PostMapping("/speak")
    public Map<String,String> speak(@RequestBody Speak r)throws SQLException{
        validate(r.x,r.z);
        if(r.message==null||r.message.isBlank()||r.message.length()>400)throw new IllegalArgumentException("Enter a message up to 400 characters.");
        return Map.of("message",session.geoSpeak(map,r.id,r.message,r.language,r.x,r.z));
    }
    public record Action(String id,double x,double z){}
    @PostMapping("/action")
    public GeoProjection.ActionResult action(@RequestBody Action r)throws SQLException{
        validate(r.x,r.z);return session.geoAction(map,r.id,r.x,r.z);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String,String> error(IllegalArgumentException e){return Map.of("message",e.getMessage());}
}
