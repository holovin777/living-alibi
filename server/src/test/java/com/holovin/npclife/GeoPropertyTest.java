package com.holovin.npclife;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class GeoPropertyTest {
    @TempDir Path folder;
    @Test void propertyUsesOriginalOwnershipAndLocalActions()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);t.player.money=500;t.player.place=HousingLife.HOUSE_DISTRICT;
        var at=map.point(t.player.place);
        assertNull(GeoProperty.view(t,s,map,at,null).garden());
        t.player.ownsHouse=true;
        assertTrue(GeoProperty.view(t,s,map,at,null).actions().stream().anyMatch(a->a.id().equals("property:build-greenhouse")));
        GardenLife.act(t,"build-greenhouse",s.day,s.minute);
        assertTrue(GeoProperty.view(t,s,map,at,null).garden().greenhouse());
        assertEquals(475,t.player.money);
        t.player.place=TownLife.Place.OFFICE;at=map.point(t.player.place);
        assertTrue(GeoProperty.view(t,s,map,at,null).actions().isEmpty());
        assertTrue(GeoProperty.view(t,s,map,at,t.player.place).actions().stream().anyMatch(a->a.id().equals("property:rental-buy")));
        RentalLife.act(t,"rental-buy",s.minute);
        assertTrue(GeoProperty.view(t,s,map,at,t.player.place).rental().owned());
        assertEquals(295,t.player.money);
    }
}
