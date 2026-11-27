package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class GeoDriveTest {
    @TempDir Path dir;
    @Test void directedRoadAndTheftAreSeparateFromLegalCar()throws Exception{
        var graph=dir.resolve("graph.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\nM\t1\t0\t0\nM\t2\t100\t0\nD\t1\t2\n");
        var map=new GeoProjection(graph);
        var drive=new GeoDrive(false,"1");
        assertEquals(8,drive.advance(map,0,8),.01);
        assertEquals(8,drive.position(map).x(),.01);
        drive.advance(map,0,200);
        assertEquals(100,drive.position(map).x(),.01);
        assertEquals(0,drive.advance(map,0,10),.01);
        var s=new WorldState();var t=TownLife.create(s);
        t.player.carPlace=TownLife.Place.WORKSHOP;t.player.carFuel=4;t.player.carCondition=80;
        VehicleTheftLife.take(t,s,new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.active(t));
        assertEquals(1,JusticeLife.thefts(t));
        assertEquals(4,t.player.carFuel);
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
        s.minute+=61;
        JusticeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertTrue(JusticeLife.confined(t));assertFalse(VehicleTheftLife.active(t));
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
    }
}
