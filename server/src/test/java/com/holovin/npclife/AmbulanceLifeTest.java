package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbulanceLifeTest {
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
