package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class ApartmentAccessTest {
    @Test void keyRequiresConsentAndExpiresOrIsRevoked(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        t.player.place=NORTH_RESIDENCES;
        var host=t.get("marco");host.place=host.home;host.travelRemaining=0;host.activity="FREE_TIME";host.carefulness=50;
        var contact=new PlayerLife.Contact(host.id);contact.knownName="Visitor";contact.trust=98;contact.affinity=80;contact.conversations=12;
        t.player.contacts.put(host.id,contact);PlayerLife.learnResident(t,host.id);
        AddressLife.ask(t,host,contact);
        assertFalse(IntercomLife.sharedKey(t,host.id,1));
        assertThrows(IllegalArgumentException.class,()->IntercomLife.requestKey(t,host.id,1,1,600));
        IntercomLife.ring(t,host.id,1,1,600);IntercomLife.invite(t,host.id,1,1,601);
        IntercomLife.enter(t,host.id,1,1,602);
        IntercomLife.requestKey(t,host.id,1,1,603);
        assertTrue(IntercomLife.sharedKey(t,host.id,1));
        assertTrue(IntercomLife.sharedKey(t.copy(),host.id,1));
        assertFalse(IntercomLife.sharedKey(t,host.id,8));
        IntercomLife.leave(t,host.id,1,1,604);
        host.activity="SLEEPING";
        assertTrue(IntercomLife.useKey(t,host.id,1,1,605).contains("locked"));
        host.activity="FREE_TIME";
        IntercomLife.useKey(t,host.id,1,1,606);
        assertNotNull(IntercomLife.apartment(t,1,606));
        contact.trust=60;IntercomLife.tick(t,s);
        assertFalse(IntercomLife.sharedKey(t,host.id,1));assertNull(IntercomLife.apartment(t,1,606));
        contact.trust=99;assertFalse(IntercomLife.sharedKey(t,host.id,1));
    }
}
