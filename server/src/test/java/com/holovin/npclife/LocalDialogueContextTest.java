package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LocalDialogueContextTest {
    @Test void strangerDoesNotReceivePrivatePlayerIdentity(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);var child=people.get(0);var parent=people.get(1);var stranger=people.get(2);
        child.age=6;DemographyLife.set(t,child,"parent-a",parent.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.CHILDHOOD);t.industryStocks.put(RebirthLife.CHILD,child.generationIndex);
        var family=LocalDialogue.facts(t,parent,null,s);
        assertEquals(child.name,family.path("playerKnownName").asText());
        assertEquals(6,family.path("playerAge").asInt());
        assertEquals("FAMILY",family.path("relationship").asText());
        var unknown=LocalDialogue.facts(t,stranger,null,s);
        assertFalse(unknown.has("playerKnownName"));
        assertFalse(unknown.has("npcKnownName"));
        assertFalse(LocalDialogue.conversational("Sono Lorenzo",null,"Nice to meet you, Lorenzo."));
        assertTrue(LocalDialogue.conversational("Ti piace la pioggia?",null,"I'm not sure I understood. Could you ask me in a different way?"));
    }
}
