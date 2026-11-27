package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PsychologySubstanceTest {
    @Test void privateStatePersistsAndSupportDoesNotForcePlayerUse(){
        var s=new WorldState();var t=TownLife.create(s);
        var c=PopulationGenerator.add(t,1).getFirst();
        c.age=30;c.health=100;c.activity="FREE_TIME";c.travelRemaining=0;c.money=1000;c.place=TownLife.Place.CLINIC;
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        t.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
        s.day=1;s.minute=600;
        SubstanceLife.exposure(t,c,s,SubstanceLife.Kind.OPIOIDS);
        assertEquals(8,SubstanceLife.total(t,c));assertEquals(97,c.health);
        assertThrows(IllegalArgumentException.class,()->SubstanceLife.exposure(t,c,s,SubstanceLife.Kind.STIMULANTS));
        SubstanceLife.support(t,c,s);
        s.day=2;s.minute=1260;SubstanceLife.tick(t,s);PsychologyLife.tick(t,s);
        assertEquals(5,SubstanceLife.total(t,c));assertEquals(1000,c.money);
        var copy=t.copy();assertEquals(5,SubstanceLife.total(copy,copy.get(c.id)));
        for(int day=3;day<=100;day++){s.day=day;SubstanceLife.tick(t,s);PsychologyLife.tick(t,s);}
        assertEquals(0,SubstanceLife.total(t,c));
        assertTrue(PsychologyLife.view(t,c).loneliness()>20);
    }
}
