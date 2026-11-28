package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoadPoliceLifeTest {
    @Test void checkFinesCooldownPaymentAndCleanDriver(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.money=15;t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        var result=RoadPoliceLife.check(t,s,"car",20,"LA P1");
        assertTrue(result.checked());assertEquals(55,result.fine());
        assertEquals(55,RoadPoliceLife.view(t).outstanding());assertEquals(15,t.player.money);
        assertFalse(RoadPoliceLife.check(t,s,"car",20,"LA P1").checked());
        assertEquals(1,RoadPoliceLife.view(t).checks());
        int funds=t.employerFunds.getOrDefault("city-maintenance",0);
        RoadPoliceLife.pay(t,s);
        assertEquals(40,RoadPoliceLife.view(t).outstanding());assertEquals(15,RoadPoliceLife.view(t).paid());
        assertEquals(funds+15,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(0,t.player.money);
        assertFalse(JusticeLife.confined(t));assertEquals(0,JusticeLife.thefts(t));
        s.minute+=30;DrivingSchoolLife.set(t,"B","issued",1);
        assertEquals(0,RoadPoliceLife.check(t,s,"car",90,"LA P1").fine());
        assertEquals(2,RoadPoliceLife.view(t).checks());assertEquals(1,RoadPoliceLife.view(t).violations());
        assertEquals(40,RoadPoliceLife.view(t.copy()).outstanding());
        t.player.incarnation++;assertEquals(0,RoadPoliceLife.view(t).outstanding());
    }
}
