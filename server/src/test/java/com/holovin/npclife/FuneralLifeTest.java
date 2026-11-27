package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FuneralLifeTest {
    @Test void burialPreservesHistoryAndDoesNotSpendControlledChildsMoney(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var dead=people.get(0);var child=people.get(1);var sibling=people.get(2);
        dead.age=70;child.age=40;sibling.age=35;child.money=1000;sibling.money=130;
        DemographyLife.set(t,child,"parent-a",dead.generationIndex);
        DemographyLife.set(t,sibling,"parent-a",dead.generationIndex);
        t.industryStocks.put(RebirthLife.CHILD,child.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        FuneralLife.setReligiousPreference(t,dead,true);
        dead.alive=false;
        s.day=100;s.minute=360;
        ArchiveLife.record(t,"death:"+dead.id,s.day,s.minute,"Population",dead.name+" died.");
        t.employerFunds.put(MunicipalLife.CITY,1000);
        FuneralLife.tick(t,s);
        assertTrue(FuneralLife.grieving(t,child,100));
        assertTrue(FuneralLife.graves(t).isEmpty());
        s.day=103;FuneralLife.tick(t,s);
        var grave=FuneralLife.graves(t).getFirst();
        assertEquals(dead.id,grave.residentId());assertEquals(100,grave.recordedDeathDay());
        assertEquals(103,grave.burialDay());assertEquals(60,grave.paid());
        assertEquals(1000,child.money);assertEquals(100,sibling.money);
        assertEquals(970,MunicipalLife.balance(t,MunicipalLife.CITY));
        assertTrue(t.archive.get("grave:"+dead.id).text().contains("Religious"));
        FuneralLife.tick(t,s);
        assertEquals(1,FuneralLife.graves(t).size());assertEquals(60,t.employerFunds.get(FuneralLife.PROVIDER));
        assertFalse(FuneralLife.grieving(t,child,200));
        assertEquals(grave,FuneralLife.graves(t.copy()).getFirst());
    }
}
