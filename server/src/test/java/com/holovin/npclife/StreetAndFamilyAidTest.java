package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StreetAndFamilyAidTest {
    @Test void donationIsOptionalAndHabitStopsWhenHired(){
        var s=new WorldState();s.day=2;s.minute=600;
        var t=TownLife.create(s);
        var c=new TownLife.Citizen("aid-test","Test Resident","Job seeker",TownLife.Place.BAKERY);
        c.generationIndex=9000;c.home=TownLife.Place.SOUTH_RESIDENCES;c.employed=false;c.money=5;
        c.activity="ASKING_FOR_HELP";c.personalSeed=0;t.people.put(c.id,c);
        t.player.place=TownLife.Place.BAKERY;t.player.money=50;
        assertEquals(TownLife.Place.BAKERY,StreetAidLife.destination(t,c,2,600));
        assertEquals("No problem. Have a good day.",PlayerLife.speak(t,c.id,"Decline request",2,600));
        assertEquals(50,t.player.money);
        assertEquals("Thank you. That helps.",PlayerLife.speak(t,c.id,"Give 2 EUR",2,600));
        assertEquals(48,t.player.money);assertEquals(7,c.money);
        assertEquals("You have already helped me today.",PlayerLife.speak(t,c.id,"Give 2 EUR",2,600));
        assertEquals(48,t.player.money);
        c.employed=true;
        assertNull(StreetAidLife.destination(t,c,2,600));
    }
    @Test void familyCareTransfersRealSuppliesAtHomeOnly(){
        var t=TownLife.create(new WorldState());
        var elder=new TownLife.Citizen("elder-test","Test Elder","Retired",TownLife.Place.SOUTH_RESIDENCES);
        var adult=new TownLife.Citizen("adult-test","Test Adult","Mechanic",TownLife.Place.SOUTH_RESIDENCES);
        elder.generationIndex=9001;adult.generationIndex=9002;elder.age=75;adult.age=40;
        elder.food=0;elder.water=0;adult.food=4;adult.water=4;
        elder.activity="AT_HOME";adult.activity="AT_HOME";
        t.people.put(elder.id,elder);t.people.put(adult.id,adult);
        DemographyLife.set(t,adult,"parent-a",elder.generationIndex);
        assertFalse(FamilyCareLife.help(t,adult,elder,2));
        adult.homeUnit=elder.homeUnit;
        assertTrue(FamilyCareLife.help(t,adult,elder,2));
        assertEquals(1,elder.food);assertEquals(1,elder.water);
        assertEquals(3,adult.food);assertEquals(3,adult.water);
        assertFalse(FamilyCareLife.help(t,adult,elder,2));
    }
}
