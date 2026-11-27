package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LostChildAidTest {
    private record Fixture(TownLife.Town town,WorldState clock,TownLife.Citizen child,TownLife.Citizen adult){}
    private static Fixture fixture(){
        var clock=new WorldState();clock.day=2;clock.minute=600;
        var town=TownLife.create(clock);
        var people=PopulationGenerator.add(town,2);
        var child=people.get(0);var adult=people.get(1);
        child.age=8;child.place=TownLife.Place.PARK;child.activity="FREE_TIME";child.travelRemaining=0;
        adult.age=40;adult.place=TownLife.Place.PARK;adult.activity="FREE_TIME";adult.travelRemaining=0;
        adult.sociability=80;adult.carefulness=70;adult.occupation="Job seeker";adult.employed=false;
        town.industryStocks.put(RebirthLife.PHASE,RebirthLife.CHILDHOOD);
        town.industryStocks.put(RebirthLife.CHILD,child.generationIndex);
        return new Fixture(town,clock,child,adult);
    }
    @Test void italianLostChildIntentIsNotOrdinarySmallTalk(){
        var f=fixture();
        assertTrue(LostChildAidLife.request(f.town,"Scusi, mi sono perso nel parco. Mi aiuta?"));
        assertTrue(LostChildAidLife.request(f.town,"Non trovo i miei genitori."));
        assertTrue(LostChildAidLife.request(f.town,"I'm lost, can you help me?"));
        assertFalse(LostChildAidLife.request(f.town,"Non sono perso."));
        assertFalse(LostChildAidLife.request(f.town,"Sono Lorenzo."));
    }
    @Test void helpfulAdultMakesRealPoliceReportAndDoesNotMoveChild(){
        var f=fixture();
        var place=f.child.place;
        assertFalse(ChildSearchLife.view(f.town,f.child,f.clock).policeReported());
        String response=LostChildAidLife.respond(f.town,f.adult,f.clock,"it");
        assertTrue(response.contains("polizia"));
        var search=ChildSearchLife.view(f.town,f.child,f.clock);
        assertTrue(search.policeReported());
        assertEquals(1,search.incidents());
        assertEquals(place,f.child.place);
        assertEquals(0,f.child.travelRemaining);
        LostChildAidLife.respond(f.town,f.adult,f.clock,"it");
        assertEquals(1,ChildSearchLife.view(f.town,f.child,f.clock).incidents());
    }
    @Test void requestIsRecognizedEvenAfterConversationBudgetExpires(){
        var f=fixture();
        for(int i=0;i<ConversationAvailability.turns(f.town,f.adult,2,600);i++)
            ConversationAvailability.record(f.town,f.adult,2,600);
        assertFalse(ConversationAvailability.allowed(f.town,f.adult,2,601));
        assertTrue(LostChildAidLife.request(f.town,"Mi sono perso"));
        assertTrue(LostChildAidLife.respond(f.town,f.adult,f.clock,"it").contains("polizia"));
    }
    @Test void notEveryBystanderAgreesToHelp(){
        var f=fixture();
        f.adult.sociability=10;f.adult.carefulness=10;
        String response=LostChildAidLife.respond(f.town,f.adult,f.clock,"it");
        assertTrue(response.contains("agente"));
        assertFalse(ChildSearchLife.view(f.town,f.child,f.clock).policeReported());
    }
}
