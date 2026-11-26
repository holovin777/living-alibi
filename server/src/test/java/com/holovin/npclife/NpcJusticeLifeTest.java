package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcJusticeLifeTest {
    @Test void confirmedCaseHasDelayedCustodyAndReleaseWithoutRevealingIdentity(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        var offender=new TownLife.Citizen("fixture","Fixture Resident","Baker",TownLife.Place.MARKET);
        offender.generationIndex=99;offender.age=35;t.people.put(offender.id,offender);
        var c=new InvestigationLife.CaseFile("case-fixture",1,600,TownLife.Place.MARKET,"elena",offender.id,"wallet",15);
        t.cases.put(c.id,c);
        NpcJusticeLife.queue(t,c);NpcJusticeLife.tick(t,s);
        assertFalse(NpcJusticeLife.confined(t,offender,s));
        c.inspected=true;c.interviews.put("marco","test");c.interviews.put("elena","test");
        InvestigationLife.accuse(t,c.id,offender.id);
        NpcJusticeLife.tick(t,s);
        assertFalse(NpcJusticeLife.confined(t,offender,s));
        s.day=2;NpcJusticeLife.tick(t,s);
        assertTrue(NpcJusticeLife.confined(t,offender,s));
        assertEquals("IN_CUSTODY",offender.activity);
        assertEquals(TownLife.Place.OFFICE,offender.place);
        assertNotEquals(TownLife.publicSpace(offender.place),offender.currentSpace());
        assertTrue(NpcJusticeLife.confined(t.copy(),offender,s));
        assertEquals(1,NpcJusticeLife.convictions(t,offender));
        s.day=4;NpcJusticeLife.tick(t,s);
        assertFalse(NpcJusticeLife.confined(t,offender,s));
        assertNotEquals("IN_CUSTODY",offender.activity);
        assertEquals(1,NpcJusticeLife.convictions(t,offender));
        assertTrue(t.archive.values().stream().noneMatch(e->e.text().contains(offender.name)));
    }
}
