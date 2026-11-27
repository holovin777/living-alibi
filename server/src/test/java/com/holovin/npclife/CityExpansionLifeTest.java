package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;

class CityExpansionLifeTest {
    @Test void publicDistrictsAndRoadTimesAreAvailable(){
        var p=new PlayerLife.Player();
        assertEquals(121,PlayerLife.publicPlaces().size());
        assertEquals(96,CityExpansionLife.outerPlaces().size());
        assertEquals(25,PlayerLife.publicPlaces().indexOf(OUTER_05_00));
        assertEquals(121,PlayerLife.publicPlaces().stream().map(CityExpansionLife::cell).distinct().count());
        assertTrue(RebirthLife.adjacent(WORKSHOP,OUTER_05_01));
        assertEquals(136,CityExpansionLife.travelTime(PARK,OUTER_10_10));
        assertEquals(8,CityExpansionLife.travelTime(OUTER_09_10,OUTER_10_10));
        assertEquals(STAZIONE,PlayerLife.destination("stazione"));
        assertEquals(OUTER_10_10,PlayerLife.destination("outer_10_10"));
        assertTrue(RebirthLife.adjacent(FARM,GIARDINI));
        assertTrue(RebirthLife.adjacent(GIARDINI,STAZIONE));
        assertFalse(RebirthLife.adjacent(PARK,GIARDINI));
        assertEquals(18,PlayerLife.travelMinutes(p,GIARDINI));
        assertEquals(10,PlayerLife.travelMinutes(p,MARKET));
        p.place=FARM;
        assertEquals(8,PlayerLife.travelMinutes(p,GIARDINI));
    }
}
