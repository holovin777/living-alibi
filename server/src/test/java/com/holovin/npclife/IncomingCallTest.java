package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class IncomingCallTest {
    @TempDir Path directory;
    private static void friendly(TownLife.Town town,String id){
        var c=town.get(id);
        c.alive=true;c.age=35;c.sociability=75;c.activity="FREE_TIME";
        var contact=new PlayerLife.Contact(id);
        contact.knownName="Luca";contact.trust=80;contact.affinity=40;
        contact.conversations=5;contact.lastDay=1;
        town.player.contacts.put(id,contact);
        PlayerLife.learnResident(town,id);
    }
    @Test
    void consentRingingAndCoffeeInvitationSurviveReload() throws Exception {
        Path file=directory.resolve("calls.db");
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            friendly(town,"marco");
            clock.day=5;clock.minute=600;
            PhoneLife.tick(town,clock);
            assertTrue(PhoneLife.report(town).calls().isEmpty());
            assertTrue(PhoneLife.shareNumber(town,town.get("marco")));
            PhoneLife.tick(town,clock);
            assertEquals("RINGING",PhoneLife.report(town).calls().getFirst().status());
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            assertEquals("RINGING",PhoneLife.report(town).calls().getFirst().status());
            assertTrue(PhoneLife.answerCall(town,"marco","answer",5,601).contains("caffè"));
            assertEquals("ANSWERED",PhoneLife.report(town).calls().getFirst().status());
            assertTrue(PhoneLife.report(town).calls().getFirst().invitation());
            PhoneLife.answerCall(town,"marco","accept",5,602);
            assertEquals("ACCEPTED",PhoneLife.report(town).calls().getFirst().status());
            assertEquals(6,PhoneLife.report(town).meetings().getFirst().day());
            assertThrows(IllegalArgumentException.class,()->PhoneLife.answerCall(town,"marco","accept",5,603));
        }
    }
    @Test
    void outgoingCallsRequireConsentAndRespectAvailability() throws Exception {
        Path file=directory.resolve("outgoing.db");
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            friendly(town,"marco");
            assertThrows(IllegalArgumentException.class,()->PhoneLife.outgoing(town,"marco","dial",5,600));
            assertTrue(PhoneLife.shareNumber(town,town.get("marco")));
            town.get("marco").activity="WORKING";
            assertEquals("No answer.",PhoneLife.outgoing(town,"marco","dial",5,600));
            assertEquals("NO_ANSWER",PhoneLife.report(town).outgoingCalls().getFirst().status());
            town.get("marco").activity="FREE_TIME";town.get("marco").travelRemaining=0;
            assertEquals("Please wait before calling again.",PhoneLife.outgoing(town,"marco","dial",5,601));
            assertEquals("Ciao! Dimmi pure.",PhoneLife.outgoing(town,"marco","dial",5,630));
            assertTrue(PhoneLife.report(town).dialContacts().getFirst().connected());
            assertEquals("Volentieri! Domani alle 18 al bar.",PhoneLife.outgoing(town,"marco","invite-coffee",5,631));
            assertEquals(6,PhoneLife.report(town).meetings().getFirst().day());
            clock.day=5;clock.minute=632;
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());
            assertEquals("ACCEPTED",PhoneLife.report(town).outgoingCalls().getFirst().status());
            assertFalse(PhoneLife.report(town).dialContacts().getFirst().connected());
            assertEquals(6,PhoneLife.report(town).meetings().getFirst().day());
        }
    }

    @Test
    void homeVisitRequiresSharedAddressAndDinnerConsumesSupplies(){
        var clock=new WorldState();var town=TownLife.create(clock);
        friendly(town,"marco");
        var host=town.get("marco");host.travelRemaining=0;
        assertTrue(PhoneLife.shareNumber(town,host));
        PhoneLife.outgoing(town,"marco","dial",5,600);
        assertTrue(PhoneLife.outgoing(town,"marco","ask-home-visit",5,601).contains("Mi dispiace"));
        assertTrue(AddressLife.ask(town,host,town.player.contacts.get("marco")).contains("Magnolie"));
        PhoneLife.outgoing(town,"marco","dial",5,630);
        assertTrue(PhoneLife.outgoing(town,"marco","ask-home-visit",5,631).contains("Volentieri"));
        assertEquals("HOME",PhoneLife.report(town).meetings().getFirst().place());
        clock.day=6;clock.minute=1080;
        assertTrue(PhoneLife.homeVisitWindow(town,host,clock));
        assertFalse(PhoneLife.meetingWindow(town,host,clock));
        town.player.place=TownLife.Place.NORTH_RESIDENCES;
        host.place=host.home;host.activity="FREE_TIME";host.food=5;host.water=5;
        IntercomLife.ring(town,"marco",1,6,1080);
        assertTrue(IntercomLife.invite(town,"marco",1,6,1081).contains("Come in"));
        IntercomLife.enter(town,"marco",1,6,1082);
        assertEquals("MET",PhoneLife.report(town).meetings().getFirst().status());
        assertEquals("You shared dinner.",IntercomLife.socialize(town,"marco",1,"dinner",6,1083).message());
        assertEquals(3,host.food);assertEquals(3,host.water);
        assertTrue(town.player.contacts.get("marco").important.containsKey(ConversationMemory.Milestone.SHARED_MEAL));
        assertThrows(IllegalArgumentException.class,()->IntercomLife.socialize(town,"marco",1,"dinner",6,1098));
    }

    @Test
    void missedCallsExpireAndCooldownPreventsRepeatedRinging(){
        var clock=new WorldState();var town=TownLife.create(clock);
        friendly(town,"marco");assertTrue(PhoneLife.shareNumber(town,town.get("marco")));
        clock.day=5;clock.minute=600;PhoneLife.tick(town,clock);
        clock.minute=605;PhoneLife.tick(town,clock);
        assertEquals("MISSED",PhoneLife.report(town).calls().getFirst().status());
        assertThrows(IllegalArgumentException.class,()->PhoneLife.answerCall(town,"marco","answer",5,605));
        clock.day=6;clock.minute=600;PhoneLife.tick(town,clock);
        assertEquals(5,PhoneLife.report(town).calls().getFirst().day());
        clock.day=12;PhoneLife.tick(town,clock);
        assertEquals("RINGING",PhoneLife.report(town).calls().getFirst().status());
        PhoneLife.answerCall(town,"marco","decline",12,600);
        assertEquals("DECLINED",PhoneLife.report(town).calls().getFirst().status());
    }
}
