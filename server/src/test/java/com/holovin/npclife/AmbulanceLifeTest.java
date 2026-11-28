package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbulanceLifeTest {
    @Test void visibleAmbulanceFollowsMappedRoadAndHoldsAtPickup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)throws Exception{
        var graph=dir.resolve("ambulance.tsv");
        java.nio.file.Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nN\t3\t100\t100\nE\t1\t2\nE\t2\t3\nM\t1\t0\t0\nM\t2\t100\t0\nM\t3\t100\t100\nD\t1\t2\nD\t2\t3\nD\t3\t2\n");
        var map=new GeoProjection(graph);
        map.anchors.put(TownLife.Place.CLINIC,"1");map.anchors.put(TownLife.Place.PARK,"3");
        var s=new WorldState();s.day=2;s.minute=600;
        var t=TownLife.create(s);t.player.place=TownLife.Place.PARK;t.player.destination=t.player.place;
        t.player.health=60;RoadAccidentLife.add(t,"injury",40);
        t.employerFunds.put("city-maintenance",100);t.industryStocks.put(FuelLife.STOCK,20);
        assertNull(map.ambulanceVehicle(t,s));
        AmbulanceLife.call(t,s);
        var start=map.ambulanceVehicle(t,s);assertEquals("AMBULANCE",start.kind());assertEquals(0,start.distance(),.001);
        s.minute+=AmbulanceLife.responseMinutes(t.player.place)/2;
        var middle=map.ambulanceVehicle(t,s);assertTrue(middle.distance()>0&&middle.distance()<200);
        assertEquals(3,middle.route().size());
        s.minute=600+AmbulanceLife.responseMinutes(t.player.place);
        var arrived=map.ambulanceVehicle(t,s);assertEquals(100,arrived.x(),.001);assertEquals(100,arrived.z(),.001);assertEquals(200,arrived.distance(),.001);
        assertNotEquals(start.trip(),arrived.trip());
        AmbulanceLife.board(t,s);assertNull(map.ambulanceVehicle(t,s));
    }
    @Test void dispatchUsesResourcesAndBoardingMovesPatientNotVehicle(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        var origin=TownLife.Place.PARK;
        t.player.place=origin;t.player.destination=origin;t.player.carPlace=origin;
        t.player.health=60;t.player.money=0;
        RoadAccidentLife.add(t,"injury",40);
        t.employerFunds.put("city-maintenance",100);
        t.industryStocks.put(FuelLife.STOCK,20);
        int units=AmbulanceLife.fuelUnits(origin);
        AmbulanceLife.call(t,s);
        assertEquals(20-units,FuelLife.stock(t));
        assertEquals(100-units*FuelLife.PRICE-AmbulanceLife.CARE_COST,t.employerFunds.get("city-maintenance").intValue());
        assertFalse(AmbulanceLife.canBoard(t,s));
        assertThrows(IllegalArgumentException.class,()->AmbulanceLife.call(t,s));
        s.minute+=AmbulanceLife.responseMinutes(origin);
        assertEquals("ARRIVED",AmbulanceLife.view(t.copy(),s).status());
        AmbulanceLife.board(t,s);
        assertEquals(TownLife.Place.CLINIC,t.player.place);assertEquals(origin,t.player.carPlace);
        assertEquals(75,t.player.health);assertEquals(0,t.player.money);
        assertEquals(1,AmbulanceLife.view(t,s).completed());
        assertEquals("NONE",AmbulanceLife.view(t,s).status());
    }
    @Test void leavingPickupCancelsAndRefundsUnusedCareOnly(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.place=TownLife.Place.PARK;t.player.destination=t.player.place;
        t.player.health=70;RoadAccidentLife.add(t,"injury",30);
        t.employerFunds.put("city-maintenance",100);t.industryStocks.put(FuelLife.STOCK,20);
        int units=AmbulanceLife.fuelUnits(t.player.place);
        AmbulanceLife.call(t,s);t.player.place=TownLife.Place.MARKET;
        AmbulanceLife.tick(t,s);
        assertEquals("NONE",AmbulanceLife.view(t,s).status());
        assertEquals(100-units*FuelLife.PRICE,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(20-units,FuelLife.stock(t));
    }
}
