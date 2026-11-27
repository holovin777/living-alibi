package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoRebirthTest {
    @TempDir Path folder;
    @Test void nextLifeUsesExistingPhaseAndNeverRevivesShell()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);
        var s=new WorldState();var t=TownLife.create(s);
        t.player.alive=false;t.player.health=0;
        RebirthLife.prepare(t,s.day,s.minute);
        var view=map.view(t,s,map.point(t.player.place));
        assertEquals("CHOICE",view.life().phase());
        assertFalse(view.alive());
        assertTrue(view.actions().stream().anyMatch(a->a.id().equals("rebirth-adult")));
        assertTrue(view.actions().stream().noneMatch(a->a.id().startsWith("bank-")));
        assertEquals(1,t.pastLives.size());
        RebirthLife.adultFallback(t);
        assertTrue(t.player.alive);
        assertEquals("ADULT",GeoLife.view(t).phase());
        assertEquals(1,t.pastLives.size());
    }
}
