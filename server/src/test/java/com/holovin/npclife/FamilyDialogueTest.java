package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FamilyDialogueTest {
    @Test void newbornRecognizedByParentButNotStrangers(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);var parent=people.get(0);var child=people.get(1);var stranger=people.get(2);
        child.age=0;child.hunger=10;child.fatigue=10;
        DemographyLife.set(t,child,"parent-a",parent.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.CHILDHOOD);
        t.industryStocks.put(RebirthLife.CHILD,child.generationIndex);
        assertTrue(FamilyDialogueLife.family(t,parent));
        assertEquals(parent.name,PlayerLife.displayName(t,parent));
        assertNotNull(FamilyDialogueLife.reply(t,parent,"ciao","it"));
        assertFalse(FamilyDialogueLife.reply(t,parent,"ciao","it").contains("sconosciuto"));
        assertNull(FamilyDialogueLife.reply(t,stranger,"ciao","it"));
        assertEquals("Unknown resident",PlayerLife.displayName(t,stranger));
    }
}
