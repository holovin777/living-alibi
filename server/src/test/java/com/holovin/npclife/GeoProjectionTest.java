package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoProjectionTest {
    @TempDir Path folder;
    @Test void sharesExistingWorld()throws Exception{
        var graph=folder.resolve("graph.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nN\t3\t100\t100\nE\t1\t2\nE\t2\t3\n");
        var map=new GeoProjection(graph);
        var db=folder.resolve("world.db");
        try(var game=new GameSession(db,12)){
            var old=game.snapshot();var geo=game.geoNearby(map,0,0);
            assertEquals(old.player().money(),geo.money());
            assertEquals(old.player().placeId(),geo.playerDistrict().id());
            assertEquals(14,geo.population());
            assertTrue(geo.actions().stream().anyMatch(a->a.id().equals("rest")));
            assertThrows(IllegalArgumentException.class,()->game.geoAction(map,"car:buy",9999,9999));
            assertThrows(IllegalArgumentException.class,()->game.geoAction(map,"unknown",0,0));
            game.geoTick(1);
            assertEquals(old.minute()+1,game.geoNearby(map,0,0).minute());
        }
        try(var game=new GameSession(db,12)){
            assertEquals(game.snapshot().minute(),game.geoNearby(map,0,0).minute());
        }
    }
}
