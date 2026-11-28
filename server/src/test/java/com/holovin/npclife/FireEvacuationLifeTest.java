package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireEvacuationLifeTest {
    @Test void affectedResidentsStayAtShelterUntilIncidentClears(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);
        var c=t.people.values().stream().filter(x->x.generationIndex>0&&x.alive).findFirst().orElseThrow();
        c.place=TownLife.Place.FACTORY;c.destination=c.place;c.travelRemaining=0;
        assertNull(FireEvacuationLife.destination(t,c,c.place));
        FireServiceLife.start(t,s,TownLife.Place.FACTORY);
        assertEquals(TownLife.Place.PARK,FireEvacuationLife.destination(t,c,c.place));
        assertEquals(1,FireEvacuationLife.affectedCount(t));assertEquals(0,FireEvacuationLife.shelteredCount(t));
        c.place=TownLife.Place.PARK;c.destination=c.place;
        assertEquals(TownLife.Place.PARK,FireEvacuationLife.destination(t,c,TownLife.Place.MARKET));
        assertEquals(1,FireEvacuationLife.shelteredCount(t));
        assertEquals(1,FireServiceLife.get(t,"evacuated-total"));
        assertTrue(FireEvacuationLife.affected(t.copy(),c));
        t.employerFunds.put("city-maintenance",100);t.industryStocks.put(FuelLife.STOCK,20);t.cleanWater=8;
        FireServiceLife.report(t);assertTrue(FireServiceLife.dispatch(t,s));
        s.minute+=FireServiceLife.responseMinutes(TownLife.Place.FACTORY)+15;
        FireServiceLife.tick(t,s);
        assertNull(FireEvacuationLife.destination(t,c,TownLife.Place.FACTORY));
        assertEquals(0,FireEvacuationLife.affectedCount(t));
        assertEquals(1,FireServiceLife.get(t,"evacuated-total"));
    }
}
