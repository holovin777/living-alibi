package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireTrainingLifeTest {
    @Test void qualificationEnablesPaidContractAndDoesNotTransferToNextLife(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.money=50;t.player.health=100;t.player.fatigue=0;t.player.lastWorkDay=0;
        t.player.place=FireServiceLife.DEPOT;t.player.destination=t.player.place;
        t.employerFunds.put("city-maintenance",100);
        assertThrows(IllegalArgumentException.class,()->CareerLife.apply(t,FireTrainingLife.JOB,s.day));
        assertFalse(FireTrainingLife.canTrain(t,s));
        t.player.place=TownLife.Place.SCHOOL;t.player.destination=t.player.place;
        FireTrainingLife.train(t,s);FireTrainingLife.train(t,s);
        assertFalse(FireTrainingLife.canTrain(t,s));
        t.player.place=FireServiceLife.DEPOT;t.player.destination=t.player.place;
        FireTrainingLife.train(t,s);
        assertTrue(FireTrainingLife.qualified(t.copy()));
        assertEquals(35,t.player.money);
        assertEquals(105,t.employerFunds.get("city-maintenance").intValue());
        CareerLife.apply(t,FireTrainingLife.JOB,s.day);
        CareerLife.work(t,s.day,s.minute);
        assertEquals(FireTrainingLife.JOB,t.player.jobId);
        assertEquals(87,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(s.day,t.player.lastWorkDay);
        assertThrows(IllegalArgumentException.class,()->CareerLife.work(t,s.day,s.minute));
        t.player.incarnation++;assertFalse(FireTrainingLife.qualified(t));
    }
}
