package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AthleticsLifeTest {
    @Test void actualContestPaysOnlyOnceAndSeparatesYouth(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var adult=people.get(0);var rival=people.get(1);var youth=people.get(2);
        adult.age=30;rival.age=31;youth.age=12;
        var sport=AthleticsLife.Sport.RUN;
        for(var c:people){
            c.place=TownLife.Place.PARK;c.home=TownLife.Place.NORTH_RESIDENCES;c.travelRemaining=0;c.activity="FREE_TIME";
            c.health=100;c.hunger=10;c.thirst=10;c.fatigue=0;
            AthleticsLife.set(t,c,"club",sport.ordinal()+1);
        }
        AthleticsLife.set(t,adult,"skill:RUN",100);
        AthleticsLife.set(t,rival,"skill:RUN",0);
        t.employerFunds.put("school",100);
        int cash=adult.money;s.day=6;s.minute=660;
        AthleticsLife.tick(t,s);
        assertEquals(1,AthleticsLife.get(t,adult,"wins"));
        assertEquals(0,AthleticsLife.get(t,youth,"contests"));
        assertEquals(cash+15,adult.money);assertEquals(85,t.employerFunds.get("school"));
        AthleticsLife.tick(t,s);assertEquals(cash+15,adult.money);
        assertEquals(1,AthleticsLife.get(t.copy(),adult,"wins"));
    }
}
