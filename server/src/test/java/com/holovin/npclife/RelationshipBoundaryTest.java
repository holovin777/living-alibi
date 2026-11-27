package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipBoundaryTest {
    @Test void ownedPartnershipDoesNotEndAutomatically(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,4);
        var a=people.get(0);var b=people.get(1);var child=people.get(2);var candidate=people.get(3);
        a.age=30;b.age=30;candidate.age=30;child.age=5;
        var rel=ParentingLife.relation(t,a,b);rel.affinity=85;rel.trust=85;rel.tension=5;
        assertTrue(DemographyLife.pair(t,a,b,1));
        FirstLifeChoice.reserveForAccount(t,a,42);
        DemographyLife.set(t,child,"parent-a",a.generationIndex);
        DemographyLife.set(t,child,"parent-b",b.generationIndex);
        rel.affinity=20;rel.trust=20;rel.tension=85;
        var home=a.homeUnit;
        s.minute=360;
        for(int day=10;day<16;day++){s.day=day;RelationshipBoundaryLife.tick(t,s,people);RelationshipBoundaryLife.tick(t,s,people);}
        assertSame(b,RebirthLife.partner(t,a));
        s.day=16;RelationshipBoundaryLife.tick(t,s,people);
        assertSame(b,RebirthLife.partner(t,a));assertSame(a,RebirthLife.partner(t,b));
        assertFalse(t.archive.values().stream().anyMatch(x->x.id().startsWith("separation:")));
        RelationshipBoundaryLife.separate(t,a,b,s.day,s.minute);
        assertNull(RebirthLife.partner(t,a));assertNull(RebirthLife.partner(t,b));
        assertEquals(a.generationIndex,DemographyLife.get(t,child,"parent-a"));
        assertEquals(b.generationIndex,DemographyLife.get(t,child,"parent-b"));
        assertEquals(home,a.homeUnit);
        assertFalse(RelationshipBoundaryLife.canAutoPair(t,a,candidate));
        assertFalse(RelationshipBoundaryLife.canAutoPair(t,candidate,a));
        assertTrue(t.archive.values().stream().anyMatch(x->x.id().startsWith("separation:")));
        assertFalse(RelationshipBoundaryLife.canAutoPair(t.copy(),a,candidate));
    }
}
