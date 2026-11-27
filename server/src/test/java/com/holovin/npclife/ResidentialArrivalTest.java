package com.holovin.npclife;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResidentialArrivalTest {
    @Test void onlyActualHomeArrivalsAppearBrieflyInTheirOwnLobby(){
        var clock=new WorldState();clock.day=3;clock.minute=1020;
        var t=TownLife.create(clock);
        var c=t.get("marco");
        t.player.place=TownLife.Place.NORTH_RESIDENCES;t.player.travelRemaining=0;t.player.cycling=false;
        c.alive=true;c.home=TownLife.Place.MARCO_HOME;c.place=TownLife.Place.CENTRO;
        c.destination=c.home;c.travelRemaining=1;c.activity="FREE_TIME";
        assertTrue(ResidentialArrivalLife.visible(t,clock,1).isEmpty());
        TownLife.move(t,c,c.home,clock,new ArrayList<>(),new ArrayList<>());
        assertEquals(c.home,c.place);
        assertTrue(ResidentialArrivalLife.visible(t,clock,1).contains(c));
        assertTrue(ResidentialArrivalLife.visible(t,clock,2).isEmpty());
        assertFalse(PlayerLife.visible(t).contains(c));
        assertThrows(IllegalArgumentException.class,()->PlayerLife.speak(t,c.id,"Hello",clock.day,clock.minute));
        assertNotNull(PlayerLife.speak(t,c.id,"My name is Alex.",clock.day,clock.minute,true));
        assertEquals("Alex",t.player.contacts.get(c.id).knownName);
        assertEquals(1,t.player.contacts.get(c.id).conversations);
        assertTrue(ResidentialArrivalLife.visible(t.copy(),clock,1).stream().anyMatch(x->x.id.equals(c.id)));
        c.travelRemaining=1;
        assertTrue(ResidentialArrivalLife.visible(t,clock,1).isEmpty());
        c.travelRemaining=0;
        t.player.place=TownLife.Place.PARK;
        assertTrue(ResidentialArrivalLife.visible(t,clock,1).isEmpty());
        t.player.place=TownLife.Place.NORTH_RESIDENCES;
        clock.minute+=ResidentialArrivalLife.WINDOW;
        assertTrue(ResidentialArrivalLife.visible(t,clock,1).isEmpty());
        assertThrows(IllegalArgumentException.class,()->PlayerLife.speak(t,c.id,"Hello",clock.day,clock.minute,true));
    }
}
