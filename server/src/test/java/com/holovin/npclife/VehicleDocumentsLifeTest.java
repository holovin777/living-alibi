package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VehicleDocumentsLifeTest {
    @Test void policyAndInspectionRequireFundsOwnershipAndCondition(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.money=100;t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        assertThrows(IllegalArgumentException.class,()->VehicleDocumentsLife.action(t,s,"insurance","car"));
        t.player.carPlace=TownLife.Place.WORKSHOP;t.player.carCondition=80;
        VehicleDocumentsLife.action(t,s,"insurance","car");
        assertTrue(VehicleDocumentsLife.insured(t,s,"car"));
        assertEquals(80,t.player.money);
        assertEquals(20,t.employerFunds.get(VehicleDocumentsLife.INSURER).intValue());
        assertThrows(IllegalArgumentException.class,()->VehicleDocumentsLife.action(t,s,"insurance","car"));
        t.player.place=TownLife.Place.WORKSHOP;t.player.destination=t.player.place;t.player.carCondition=50;
        assertThrows(IllegalArgumentException.class,()->VehicleDocumentsLife.action(t,s,"inspection","car"));
        t.player.carCondition=80;
        VehicleDocumentsLife.action(t,s,"inspection","car");
        assertEquals(70,t.player.money);
        assertTrue(VehicleDocumentsLife.inspected(t.copy(),s,"car"));
        DrivingSchoolLife.set(t,"B","issued",1);
        assertEquals(0,RoadPoliceLife.check(t,s,"car",80,"LA P1").fine());
        s.day+=30;
        assertFalse(VehicleDocumentsLife.insured(t,s,"car"));
        assertTrue(VehicleDocumentsLife.inspected(t,s,"car"));
        assertEquals(25,RoadPoliceLife.check(t,s,"car",80,"LA P1").fine());
        s.day+=30;
        assertEquals(40,RoadPoliceLife.check(t,s,"car",80,"LA P1").fine());
    }
}
