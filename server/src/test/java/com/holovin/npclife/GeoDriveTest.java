package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class GeoDriveTest {
    @TempDir Path dir;
    @Test void accelerationCoastBrakeAndPauseAreBounded(){
        var drive=new GeoDrive(false,"1");
        assertEquals(0.6,drive.controlBudget("step",Math.sqrt(0.4)),.001);
        assertTrue(drive.speed>0);
        for(int i=0;i<20;i++)drive.controlBudget("step",0.65);
        assertEquals(GeoDrive.MAX_SPEED,drive.speed,.001);
        double before=drive.speed;
        drive.controlBudget("coast",0.65);
        assertTrue(drive.speed<before);
        drive.controlBudget("brake",0.8);
        drive.controlBudget("brake",0.8);
        assertEquals(0,drive.speed,.001);
        assertEquals(0,drive.controlBudget("coast",0.65),.001);
        assertTrue(drive.controlBudget("step",60)<1);
        assertEquals(drive.speed,drive.copy().speed,.001);
    }
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
        var owner=new TownLife.Citizen("resident-test","Test Resident","Mechanic",t.player.place);
        owner.generationIndex=3;t.people.put(owner.id,owner);
        VehicleTheftLife.take(t,s,owner,new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.missing(t,owner));
        assertEquals(3,VehicleTheftLife.get(t,"owner"));
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
