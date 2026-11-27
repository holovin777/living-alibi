package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoHousingTest {
    @TempDir Path folder;
    @Test void homeAccessRequiresLeaseOrInvitation()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);
        var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);
        t.player.place=HousingLife.DISTRICT;t.player.money=100;
        var point=map.point(t.player.place);
        assertFalse(GeoProjection.ownHome(t,s));
        assertTrue(map.homeChoices(t,s,point,null).stream().anyMatch(a->a.id().equals("home-rent")));
        assertFalse(map.homeChoices(t,s,point,null).stream().anyMatch(a->a.id().equals("home-enter")));
        assertTrue(map.homeActors(t,s,t.player.place).isEmpty());
        HousingLife.rent(t,s.day,s.minute);
        assertTrue(GeoProjection.ownHome(t,s));
        assertEquals(65,t.player.money);
        assertTrue(map.homeChoices(t,s,point,null).stream().anyMatch(a->a.id().equals("home-enter")));
        var view=map.view(t,s,point,null,t.player.place);
        assertEquals("HOME",view.interior().id());
        assertTrue(view.actions().stream().anyMatch(a->a.id().equals("home-wash")));
        assertTrue(view.residents().isEmpty());
        assertFalse(view.actions().stream().anyMatch(a->a.id().startsWith("drive:")));
    }
}
