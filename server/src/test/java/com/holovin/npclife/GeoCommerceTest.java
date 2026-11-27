package com.holovin.npclife;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class GeoCommerceTest {
    @TempDir Path folder;
    @Test void commerceRequiresLocationAndUsesExistingAssets()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);t.player.money=100;t.player.place=TownLife.Place.MARKET;
        var at=map.point(t.player.place);
        assertTrue(GeoCommerce.view(t,s,map,at,null).actions().isEmpty());
        assertTrue(GeoCommerce.view(t,s,map,at,t.player.place).actions().stream().anyMatch(a->a.id().equals("stall-register")));
        MarketStallLife.act(t,"stall-register",s.day,s.minute);
        assertEquals(80,t.player.money);
        assertTrue(GeoCommerce.view(t,s,map,at,t.player.place).stall().registered());
        assertTrue(GeoCommerce.view(t,s,map,at,t.player.place).actions().stream().anyMatch(a->a.id().equals("stall-open")));
        t.player.place=TownLife.Place.LOGISTICS_DEPOT;
        at=map.point(t.player.place);
        assertTrue(GeoCommerce.view(t,s,map,at,null).actions().stream().anyMatch(a->a.id().equals("freight:register:")));
        LogisticsLife.manage(t,"register");
        assertEquals(60,t.player.money);
        assertTrue(GeoCommerce.view(t,s,map,at,null).logistics().yours());
        assertTrue(GeoCommerce.view(t,s,map,at,null).actions().stream().anyMatch(a->a.id().equals("freight:invest-25:")));
        assertTrue(GeoCommerce.view(t,s,map,new GeoProjection.Point(at.x()+100,at.z()),null).actions().isEmpty());
    }
}
