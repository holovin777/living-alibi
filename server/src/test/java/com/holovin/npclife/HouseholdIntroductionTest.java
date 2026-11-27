package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdIntroductionTest {
    @TempDir Path directory;
    @Test
    void privateFamilyIntroductionsAndSeparateMemoriesSurviveReload() throws Exception {
        Path file=directory.resolve("family-visit.db");
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            var host=town.get("marco");var partner=town.get("elena");
            int maxIndex=town.people.values().stream().mapToInt(x->x.generationIndex).max().orElse(0);
            host.generationIndex=maxIndex+1;partner.generationIndex=maxIndex+2;
            host.age=35;partner.age=34;
            for(var c:List.of(host,partner)){
                c.alive=true;c.travelRemaining=0;c.activity="FREE_TIME";
                c.hunger=20;c.thirst=20;c.fatigue=20;c.health=90;c.sociability=80;
            }
            partner.home=host.home;partner.homeUnit=host.homeUnit;
            host.place=host.home;partner.place=partner.home;
            DemographyLife.set(town,host,"partner",partner.generationIndex);
            DemographyLife.set(town,partner,"partner",host.generationIndex);
            var contact=new PlayerLife.Contact(host.id);
            contact.knownName="Luca";contact.trust=85;contact.affinity=45;
            town.player.contacts.put(host.id,contact);
            PlayerLife.learnResident(town,host.id);
            assertTrue(AddressLife.ask(town,host,contact).contains("Magnolie"));
            town.player.place=TownLife.Place.NORTH_RESIDENCES;town.player.travelRemaining=0;
            clock.day=6;clock.minute=1080;
            assertFalse(IntercomLife.introduceFamily(town,host.id,6,1080,"it").contains(partner.name));
            assertThrows(IllegalArgumentException.class,()->PlayerLife.nearby(town,partner.id,6,1080));
            assertTrue(IntercomLife.ring(town,host.id,1,6,1080).contains("Hello"));
            assertTrue(IntercomLife.invite(town,host.id,1,6,1081).contains("Come in"));
            assertTrue(IntercomLife.apartment(town,6,1081).household().visibleResidents().isEmpty());
            IntercomLife.enter(town,host.id,1,6,1082);
            var visible=IntercomLife.apartment(town,6,1082).household().visibleResidents();
            assertEquals("Unknown resident",visible.stream().filter(x->x.id().equals(partner.id)).findFirst().orElseThrow().name());
            partner.activity="SLEEPING";
            assertFalse(IntercomLife.introduceFamily(town,host.id,6,1083,"it").contains(partner.name));
            assertThrows(IllegalArgumentException.class,()->PlayerLife.nearby(town,partner.id,6,1083));
            partner.activity="FREE_TIME";
            assertTrue(IntercomLife.introduceFamily(town,host.id,6,1084,"it").contains(partner.name));
            var introduced=town.player.contacts.get(partner.id);
            assertNull(introduced.knownName);
            assertEquals(50,introduced.trust);assertEquals(0,introduced.affinity);
            assertTrue(ConversationMemory.firstMeeting(introduced,"it").contains(host.name));
            assertEquals(partner,PlayerLife.nearby(town,partner.id,6,1085));
            PlayerLife.speak(town,partner.id,"Mi chiamo Alex.",6,1085);
            assertEquals("Alex",introduced.knownName);
            assertEquals("Luca",contact.knownName);
            IntercomLife.leave(town,host.id,1,6,1086);
            assertThrows(IllegalArgumentException.class,()->PlayerLife.nearby(town,partner.id,6,1086));
            // A host who becomes busy ends a visit permanently.
            assertTrue(IntercomLife.ring(town,host.id,1,6,1096).contains("Hello"));
            assertTrue(IntercomLife.invite(town,host.id,1,6,1097).contains("Come in"));
            IntercomLife.enter(town,host.id,1,6,1098);
            host.activity="WORKING";
            clock.minute=1099;
            IntercomLife.tick(town,clock);
            assertNull(IntercomLife.apartment(town,6,1099));
            host.activity="FREE_TIME";
            assertNull(IntercomLife.apartment(town,6,1100));
            assertThrows(IllegalArgumentException.class,()->IntercomLife.enter(town,host.id,1,6,1100));
            // A pending doorstep invitation is also revoked when the visitor leaves.
            assertTrue(IntercomLife.ring(town,host.id,1,6,1106).contains("Hello"));
            town.player.travelRemaining=1;
            clock.minute=1107;
            IntercomLife.tick(town,clock);
            town.player.travelRemaining=0;
            assertThrows(IllegalArgumentException.class,()->IntercomLife.visit(town,host.id,1,6,1108));
            clock.minute=1108;
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());
            assertTrue(PlayerLife.knowsResident(town,"elena"));
            assertEquals("Alex",town.player.contacts.get("elena").knownName);
            assertEquals("Luca",town.player.contacts.get("marco").knownName);
            assertTrue(town.player.contacts.get("elena").important.containsKey(ConversationMemory.Milestone.FIRST_MEETING));
        }
    }
}
