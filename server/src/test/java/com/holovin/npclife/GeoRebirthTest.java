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
    @Test void familyTreeUsesPersistedParentLinksAndKeepsDeceasedAncestors(){
        var t=TownLife.create(new WorldState());
        var p=PopulationGenerator.add(t,8);
        var grand=p.get(0);var parent=p.get(1);var uncle=p.get(2);var other=p.get(3);
        var self=p.get(4);var sibling=p.get(5);var cousin=p.get(6);var stranger=p.get(7);
        DemographyLife.set(t,parent,"parent-a",grand.generationIndex);
        DemographyLife.set(t,uncle,"parent-a",grand.generationIndex);
        for(var c:java.util.List.of(self,sibling)){
            DemographyLife.set(t,c,"parent-a",parent.generationIndex);
            DemographyLife.set(t,c,"parent-b",other.generationIndex);
        }
        DemographyLife.set(t,cousin,"parent-a",uncle.generationIndex);
        grand.alive=false;
        DemographyLife.set(t,grand,"played-incarnation",1);
        var relatives=GenealogyLife.relatives(t,self);
        assertEquals(6,relatives.size());
        assertTrue(relatives.stream().anyMatch(r->r.id().equals(grand.id)&&r.role().equals("Grandparent")&&!r.alive()&&r.incarnation()==1));
        assertTrue(relatives.stream().anyMatch(r->r.id().equals(sibling.id)&&r.role().equals("Sibling")));
        assertTrue(relatives.stream().anyMatch(r->r.id().equals(uncle.id)&&r.role().equals("Aunt/uncle")));
        assertTrue(relatives.stream().anyMatch(r->r.id().equals(cousin.id)&&r.role().equals("Cousin")));
        assertTrue(relatives.stream().noneMatch(r->r.id().equals(stranger.id)||r.id().equals(self.id)));
        assertEquals(relatives,GenealogyLife.relatives(t.copy(),t.copy().get(self.id)));
        for(long seed=-100;seed<100;seed++){
            int trait=GenealogyLife.inheritedTrait(40,60,seed);
            assertTrue(trait>=35&&trait<=65);
            assertTrue(GenealogyLife.inheritedTrait(15,15,seed)>=15);
            assertTrue(GenealogyLife.inheritedTrait(90,90,seed)<=90);
        }
    }
    @Test void upbringingUsesActualHomeContactAndPersistsInWorldCopy(){
        var clock=new WorldState();var t=TownLife.create(clock);
        var residents=PopulationGenerator.add(t,3);
        var a=residents.get(0);var b=residents.get(1);var c=residents.get(2);
        a.age=30;b.age=30;c.age=8;
        for(var person:residents){
            person.home=a.home;person.homeUnit=a.homeUnit;
            person.place=a.home;person.destination=a.home;person.travelRemaining=0;
            person.activity="AT_HOME";person.money=0;
        }
        DemographyLife.set(t,c,"parent-a",a.generationIndex);
        DemographyLife.set(t,c,"parent-b",b.generationIndex);
        var relationship=new SocialLife.Relationship(a.id,b.id);
        t.peerRelationships.put(SocialNetwork.key(a.id,b.id),relationship);
        for(int day=1;day<=7;day++){
            clock.day=day;clock.minute=1080;ChildhoodLife.tick(t,clock);
            clock.minute=1260;ChildhoodLife.tick(t,clock);ChildhoodLife.tick(t,clock);
        }
        var v=ChildhoodLife.view(t,c);
        assertEquals(7,v.observedDays());assertEquals(7,v.supportedDays());assertEquals(0,v.conflictDays());
        assertEquals(51,v.security());assertEquals(51,v.confidence());assertEquals(51,v.independence());
        assertEquals(0,a.money);assertEquals(0,b.money);
        relationship.tension=80;
        for(int day=8;day<=14;day++){
            clock.day=day;clock.minute=1080;ChildhoodLife.tick(t,clock);
            clock.minute=1260;ChildhoodLife.tick(t,clock);
        }
        assertEquals(7,ChildhoodLife.view(t,c).conflictDays());
        assertEquals(51,ChildhoodLife.view(t,c).confidence());
        var saved=t.copy();var adult=saved.get(c.id);adult.age=18;
        clock.day=15;clock.minute=1260;ChildhoodLife.tick(saved,clock);
        assertEquals(ChildhoodLife.view(t,c),ChildhoodLife.view(saved,adult));
    }
    @Test void lateChildIsSearchedForByActualParentWithoutTeleporting(){
        var s=new WorldState();var t=TownLife.create(s);
        var pair=PopulationGenerator.add(t,2);var p=pair.get(0);var c=pair.get(1);
        p.age=30;c.age=10;p.activity="AT_HOME";p.place=p.home;p.destination=p.home;p.travelRemaining=0;
        c.place=TownLife.Place.LIBRARY;c.destination=c.place;c.activity="READING";c.travelRemaining=0;
        DemographyLife.set(t,c,"parent-a",p.generationIndex);
        s.minute=1080;ChildSearchLife.tick(t,s);
        assertEquals("SEARCHING",ChildSearchLife.view(t,c,s).status());
        assertEquals(TownLife.Place.PARK,ChildSearchLife.destination(t,p,s));
        assertEquals(TownLife.Place.LIBRARY,c.place);
        assertEquals(p.home,p.place);
        s.minute=1140;p.place=TownLife.Place.LIBRARY;p.destination=p.place;p.activity="SEARCHING_FOR_CHILD";
        ChildSearchLife.tick(t,s);
        assertEquals("LOCATED",ChildSearchLife.view(t,c,s).status());
        assertNull(ChildSearchLife.destination(t,p,s));
        assertEquals(TownLife.Place.LIBRARY,c.place);
        c.place=c.home;c.destination=c.home;c.activity="AT_HOME";
        s.minute=1155;ChildSearchLife.tick(t,s);
        assertEquals("NONE",ChildSearchLife.view(t,c,s).status());
        assertEquals(1,ChildSearchLife.view(t,c,s).incidents());
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
