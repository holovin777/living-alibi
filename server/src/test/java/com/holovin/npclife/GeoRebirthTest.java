package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoRebirthTest {
    @TempDir Path folder;
    @Test void familyChoiceShowsRealHouseholdAndDoesNotChangeWorld(){
        var s=new WorldState();var t=TownLife.create(s);
        var pair=PopulationGenerator.add(t,2);
        var a=pair.get(0);var b=pair.get(1);
        a.age=30;b.age=29;a.money=250;b.money=180;
        var relation=new SocialLife.Relationship(a.id,b.id);
        relation.affinity=80;relation.trust=75;relation.tension=10;
        t.peerRelationships.put(SocialNetwork.key(a.id,b.id),relation);
        assertTrue(DemographyLife.pair(t,a,b,1));
        var sibling=DemographyLife.birth(t,a,b,271);
        assertNotNull(sibling);sibling.age=3;
        RentalLife.moveHome(t,b,a.home,a.homeUnit);
        a.unpaidBills=12;b.unpaidBills=8;
        FamilyBudgetLife.autonomousDeposit(t,a);
        s.day=1366;
        t.player.alive=false;t.player.health=0;
        RebirthLife.prepare(t,s.day,s.minute);
        int residents=t.people.size(),balance=a.money+b.money;
        var f=RebirthLife.report(t,s.day).families().stream()
                .filter(x->x.first()==a.generationIndex&&x.second()==b.generationIndex).findFirst().orElseThrow();
        assertEquals(30,f.firstAge());assertEquals(29,f.secondAge());
        assertEquals(balance,f.walletTotal());assertEquals(10,f.familySavings());
        assertEquals(20,f.unpaidBills());assertTrue(f.sharedHome());
        assertEquals(1,f.children().size());assertEquals(sibling.id,f.children().getFirst().id());
        assertEquals(3,f.children().getFirst().age());assertEquals(Integer.valueOf(75),f.trust());
        assertTrue(GeoLife.familyLabel(f).contains("1 children / figli"));
        assertEquals(residents,t.people.size());assertEquals(balance,a.money+b.money);
        assertEquals(1,t.pastLives.size());
        b.alive=false;
        assertThrows(IllegalArgumentException.class,()->RebirthLife.choose(t,s.day,f.first(),f.second()));
        assertEquals(residents,t.people.size());
        b.alive=true;
        RebirthLife.choose(t,s.day,f.first(),f.second());
        var newborn=RebirthLife.child(t);
        assertEquals(0,newborn.age);assertEquals(a.homeUnit,newborn.homeUnit);
        assertEquals(a.generationIndex,DemographyLife.get(t,newborn,"parent-a"));
        assertEquals(b.generationIndex,DemographyLife.get(t,newborn,"parent-b"));
        assertEquals(residents+1,t.people.size());assertEquals(balance-20,a.money+b.money);
        assertEquals(1,t.pastLives.size());
    }
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
        assertFalse(view.life().canWalk());
        assertTrue(view.actions().stream().anyMatch(a->a.id().equals("rebirth-adult")));
        assertTrue(view.actions().stream().noneMatch(a->a.id().startsWith("bank-")));
        assertEquals(1,t.pastLives.size());
        RebirthLife.adultFallback(t);
        assertTrue(t.player.alive);
        assertEquals("ADULT",GeoLife.view(t,s).phase());
        assertEquals(1,t.pastLives.size());
    }
}
