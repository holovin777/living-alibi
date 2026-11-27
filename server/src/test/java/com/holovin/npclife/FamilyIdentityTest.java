package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FamilyIdentityTest {
    @Test void accountOwnedChildIsNeverInheritedEvenOffline(){
        var t=TownLife.create(new WorldState());
        var people=PopulationGenerator.add(t,3);
        var parent=people.get(0);var older=people.get(1);var younger=people.get(2);
        older.age=30;younger.age=25;
        for(var c:java.util.List.of(older,younger))DemographyLife.set(t,c,"parent-a",parent.generationIndex);
        FirstLifeChoice.reserveForAccount(t,older,42);
        assertThrows(IllegalArgumentException.class,()->FirstLifeChoice.reserveForAccount(t,older,43));
        assertSame(younger,RebirthLife.eldestSuccessor(t,parent,100,1));
        var copy=t.copy();
        assertTrue(FirstLifeChoice.accountOwned(copy,copy.get(older.id)));
        assertEquals(younger.id,RebirthLife.eldestSuccessor(copy,copy.get(parent.id),100,1).id);
    }
    @Test void firstBirthPreferenceFiltersExistingNewborns(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,2);
        var a=people.get(0);var b=people.get(1);
        for(var c:people){c.age=0;DemographyLife.set(t,c,"born",s.day);}
        RebirthLife.enableRandomBirth(t,true);FirstLifeChoice.require(t);
        assertFalse(RebirthLife.acceptNaturalBirths(t,s,people));
        FirstLifeChoice.choose(t,2);
        assertTrue(RebirthLife.acceptNaturalBirths(t,s,people));
        assertEquals(2,FirstLifeChoice.birthSex(RebirthLife.child(t)));
        assertThrows(IllegalArgumentException.class,()->FirstLifeChoice.choose(t,1));
    }
}
