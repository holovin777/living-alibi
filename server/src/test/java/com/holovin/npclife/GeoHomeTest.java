package com.holovin.npclife;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoHomeTest {
    @Test void activitiesUseAppropriateZones(){
        var used=new HashSet<GeoHome.Slot>();
        var cook=GeoHome.allocate("COOKING",used);
        assertTrue(cook.x()>0&&cook.z()<0);
        assertEquals("SERVE",cook.pose());
        var drink=GeoHome.allocate("DRINKING",used);
        assertNotEquals(cook,drink);
        var dinner=GeoHome.allocate("FAMILY_DINNER",used);
        assertTrue(dinner.x()<0&&dinner.z()>0);
        assertEquals("DESK",dinner.pose());
        var reading=GeoHome.allocate("READING",used);
        assertNotEquals(dinner,reading);
    }

    @Test void crowdedDinnerHasUniquePositions(){
        var used=new HashSet<GeoHome.Slot>();
        for(int i=0;i<8;i++)GeoHome.allocate("FAMILY_DINNER",used);
        assertEquals(8,used.size());
        assertEquals(8,used.stream().map(s->s.x()+":"+s.z()).distinct().count());
        assertEquals(4,used.stream().filter(s->s.pose().equals("DESK")).count());
    }

    @Test void fallbackWorksForEverySupportedActivity(){
        for(String activity:java.util.List.of("COOKING","DRINKING","HOMEWORK","READING","PLAYING","CLEANING","RESTING","WATCHING_TV","UNKNOWN")){
            var used=new HashSet<GeoHome.Slot>();
            for(int i=0;i<8;i++)GeoHome.allocate(activity,used);
            assertEquals(8,used.stream().map(s->s.x()+":"+s.z()).distinct().count());
        }
    }
}
