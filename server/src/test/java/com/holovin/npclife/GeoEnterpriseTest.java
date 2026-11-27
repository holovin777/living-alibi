package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoEnterpriseTest {
    @TempDir Path folder;
    @Test void choicesRequireLocalCounterAndReuseEconomy()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);
        var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);t.player.money=100;
        t.player.place=TownLife.Place.OFFICE;
        var at=map.point(t.player.place);
        assertTrue(map.enterpriseChoices(t,s,at,null).isEmpty());
        assertTrue(map.enterpriseChoices(t,s,at,t.player.place).stream().anyMatch(a->a.id().equals("bank-deposit-personal")));
        BankLife.act(t,"bank-deposit-personal",s.day,s.minute);
        assertEquals(90,t.player.money);
        assertEquals(10,map.phone(t,s).bank().personal());
        s.minute=1080;
        assertTrue(map.enterpriseChoices(t,s,at,t.player.place).isEmpty());
        t.player.place=TownLife.Place.WORKSHOP;
        var workshop=map.point(t.player.place);
        assertTrue(map.enterpriseChoices(t,s,workshop,t.player.place).stream().anyMatch(a->a.id().equals("enterprise:open")));
        BusinessLife.open(t);
        BusinessLife.invest(t,25);
        assertTrue(map.enterpriseChoices(t,s,workshop,t.player.place).stream().anyMatch(a->a.id().equals("enterprise:machine")));
        BusinessLife.machine(t);
        var view=map.view(t,s,workshop,null,t.player.place);
        assertTrue(view.enterprise().yours());
        assertEquals(100,view.enterprise().machineCondition());
        assertEquals(0,view.enterprise().cash());
    }
}
