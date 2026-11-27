package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;

class CityExpansionLifeTest {
    @Test void publicDistrictsAndRoadTimesAreAvailable(){
        var p=new PlayerLife.Player();
        assertEquals(25,PlayerLife.publicPlaces().size());
        assertEquals(STAZIONE,PlayerLife.destination("stazione"));
        assertTrue(RebirthLife.adjacent(FARM,GIARDINI));
        assertTrue(RebirthLife.adjacent(GIARDINI,STAZIONE));
        assertFalse(RebirthLife.adjacent(PARK,GIARDINI));
        assertEquals(18,PlayerLife.travelMinutes(p,GIARDINI));
        assertEquals(10,PlayerLife.travelMinutes(p,MARKET));
        p.place=FARM;
        assertEquals(8,PlayerLife.travelMinutes(p,GIARDINI));
    }
}
