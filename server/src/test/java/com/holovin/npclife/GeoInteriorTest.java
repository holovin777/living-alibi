package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoInteriorTest {
    @TempDir Path folder;
    @Test void publicRoomShowsOnlyPresentResidents()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);
        var s=new WorldState();var t=TownLife.create(s);
        var worker=t.get("marco");worker.place=TownLife.Place.BAKERY;worker.destination=worker.place;
        worker.travelRemaining=0;worker.activity="WORKING";
        t.player.place=TownLife.Place.BAKERY;
        var room=map.view(t,s,map.point(t.player.place),null,t.player.place);
        assertNotNull(room.interior());
        assertTrue(room.residents().stream().anyMatch(a->a.id().equals(worker.id)));
        assertFalse(map.view(t,s,map.point(t.player.place)).residents().stream().anyMatch(a->a.id().equals(worker.id)));
        worker.travelRemaining=2;
        assertFalse(map.interiorActors(t,t.player.place).stream().anyMatch(a->a.id().equals(worker.id)));
        worker.travelRemaining=0;worker.place=worker.home;
        assertFalse(map.interiorActors(t,t.player.place).stream().anyMatch(a->a.id().equals(worker.id)));
        assertFalse(GeoProjection.hasInterior(TownLife.Place.MARCO_HOME));
    }
}
