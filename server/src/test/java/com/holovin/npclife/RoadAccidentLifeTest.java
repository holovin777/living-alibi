package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoadAccidentLifeTest {
    @Test void speedInjuryLimitedInsuranceAndTreatment(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.money=30;t.player.health=100;
        t.player.carPlace=TownLife.Place.CLINIC;
        t.player.place=TownLife.Place.CLINIC;t.player.destination=t.player.place;
        assertTrue(RoadAccidentLife.damage(60)>RoadAccidentLife.damage(10));
        assertTrue(RoadAccidentLife.injury(60,true)>RoadAccidentLife.injury(60,false));
        DrivingSchoolLife.set(t,"B","issued",1);
        VehicleDocumentsLife.set(t,"car","insurance",32);
        t.employerFunds.put(VehicleDocumentsLife.INSURER,12);
        var result=RoadAccidentLife.record(t,s,"car",false,15,48);
        assertEquals(12,result.injury());assertEquals(12,result.compensation());
        assertEquals(88,t.player.health);assertEquals(42,t.player.money);
        assertEquals(0,t.employerFunds.get(VehicleDocumentsLife.INSURER).intValue());
        assertEquals(1,RoadAccidentLife.view(t.copy()).claims());
        RoadAccidentLife.treat(t,s);
        assertEquals(100,t.player.health);assertEquals(34,t.player.money);
        assertFalse(RoadAccidentLife.canTreat(t,s));
        t.employerFunds.put(VehicleDocumentsLife.INSURER,50);
        assertEquals(0,RoadAccidentLife.record(t,s,"car",true,15,48).compensation());
        s.day=32;
        assertEquals(0,RoadAccidentLife.record(t,s,"car",false,15,48).compensation());
        assertEquals(50,t.employerFunds.get(VehicleDocumentsLife.INSURER).intValue());
    }
}
