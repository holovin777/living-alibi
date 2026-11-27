package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppearanceLifeTest {
    @Test void inheritedTraitsAreStableAndComeFromParents(){
        var t=TownLife.create(new WorldState());
        var people=PopulationGenerator.add(t,4);
        var a=people.get(0);var b=people.get(1);var child=people.get(2);var sibling=people.get(3);
        for(var c:java.util.List.of(child,sibling)){
            DemographyLife.set(t,c,"parent-a",a.generationIndex);
            DemographyLife.set(t,c,"parent-b",b.generationIndex);
        }
        var x=AppearanceLife.profile(t,a);var y=AppearanceLife.profile(t,b);
        var z=AppearanceLife.profile(t,child);
        assertTrue(z.hair()==x.hair()||z.hair()==y.hair());
        assertTrue(z.eye()==x.eye()||z.eye()==y.eye());
        assertTrue(Math.abs(z.height()-(x.height()+y.height())/2)<=5);
        assertTrue(Math.abs(z.build()-(x.build()+y.build())/2)<=6);
        assertEquals(z,AppearanceLife.profile(t,child));
        child.age=80;a.alive=false;
        assertEquals(z,AppearanceLife.profile(t,child));
        var copy=t.copy();
        assertEquals(z,AppearanceLife.profile(copy,copy.get(child.id)));
        assertNotNull(AppearanceLife.profile(t,sibling));
    }
}
