package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireDutyLifeTest {
    @Test void qualifiedPlayerJoinsOneIncidentAndCompletesOnePaidTask(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.money=50;t.player.health=100;t.player.fatigue=0;
        t.player.place=TownLife.Place.SCHOOL;t.player.destination=t.player.place;
        FireTrainingLife.train(t,s);FireTrainingLife.train(t,s);
        t.player.place=FireServiceLife.DEPOT;t.player.destination=t.player.place;
        FireTrainingLife.train(t,s);t.employerFunds.put("city-maintenance",100);
        CareerLife.apply(t,FireTrainingLife.JOB,s.day);
        t.industryStocks.put(FuelLife.STOCK,20);t.cleanWater=8;
        FireDutyLife.standby(t,s);assertFalse(FireDutyLife.canJoin(t,s));
        FireServiceLife.start(t,s,TownLife.Place.FACTORY);FireServiceLife.report(t);
        assertTrue(FireServiceLife.dispatch(t,s));
        var journey=FireDutyLife.join(t,s);
        assertEquals(TownLife.Place.FACTORY,t.player.place);
        assertFalse(FireDutyLife.canAssist(t,s));
        s.minute+=journey.minutes();
        int before=FireServiceLife.get(t,"end"),cash=t.player.money,funds=t.employerFunds.get(FireServiceLife.ACCOUNT);
        FireDutyLife.assist(t,s);
        assertEquals(before-5,FireServiceLife.get(t,"end"));
        assertEquals(cash+3,t.player.money);assertEquals(funds-3,t.employerFunds.get(FireServiceLife.ACCOUNT).intValue());
        assertFalse(FireDutyLife.canAssist(t,s));
        assertEquals(1,FireDutyLife.view(t.copy(),s).completed());
    }
}
