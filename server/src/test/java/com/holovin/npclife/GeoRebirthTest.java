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
    @Test void firstPassportIsPrivateAndSurvivesDatabaseRestart()throws Exception{
        var file=folder.resolve("passport.db");
        PassportLife.View initial;
        try(var game=new GameSession(file)){
            initial=game.snapshot().player().passport();
            assertNotNull(initial);assertNotNull(initial.name());
            assertTrue(initial.age()>=18&&initial.age()<=35);
            assertNull(game.snapshot().player().privateName());
        }
        try(var game=new GameSession(file)){
            assertEquals(initial,game.snapshot().player().passport());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(initial,PassportLife.view(t,s));
            assertEquals(initial.age()+1,PassportLife.visitorAge(t,1,s.day+365));
            assertTrue(t.player.contacts.isEmpty());
            var resident=t.people.values().stream().filter(c->c.generationIndex>0).findFirst().orElseThrow();
            t.industryStocks.put("childhood:"+resident.id+":days",7);
            t.industryStocks.put("child-search:"+resident.id+":incidents",2);
            db.save(s,java.util.List.of(),java.util.List.of(),t,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var t=db.loadTown(db.load());
            var resident=t.people.values().stream().filter(c->c.generationIndex>0).findFirst().orElseThrow();
            assertEquals(7,ChildhoodLife.view(t,resident).observedDays());
            assertEquals(Integer.valueOf(2),t.industryStocks.get("child-search:"+resident.id+":incidents"));
        }
    }
    @Test void randomBirthIsNaturalAndCannotBeChosenOrRepeated()throws Exception{
        var clock=new WorldState();var t=TownLife.create(clock);
        var people=PopulationGenerator.add(t,2);
        var a=people.get(0);var b=people.get(1);
        a.age=30;b.age=29;a.money=300;b.money=300;
        var rel=new SocialLife.Relationship(a.id,b.id);
        rel.affinity=80;rel.trust=80;rel.tension=0;
        t.peerRelationships.put(SocialNetwork.key(a.id,b.id),rel);
        assertTrue(DemographyLife.pair(t,a,b,1));
        RebirthLife.enableRandomBirth(t,true);
        assertEquals(1,t.player.incarnation);
        assertTrue(t.pastLives.isEmpty());
        assertEquals(RebirthLife.CHOICE,RebirthLife.phase(t));
        assertTrue(RebirthLife.report(t,271).families().isEmpty());
        assertTrue(RebirthLife.report(t,271).randomBirth());
        assertThrows(IllegalArgumentException.class,()->RebirthLife.choose(t,271,a.generationIndex,b.generationIndex));
        assertThrows(IllegalArgumentException.class,()->RebirthLife.adultFallback(t));
        clock.day=271;clock.minute=360;
        assertFalse(RebirthLife.acceptNaturalBirths(t,clock,java.util.List.of()));
        int count=t.people.size();
        DemographyLife.tick(t,clock,new java.util.ArrayList<>(),new java.util.ArrayList<>());
        var child=RebirthLife.child(t);
        assertNotNull(child);assertEquals(0,child.age);
        assertEquals(count+1,t.people.size());
        assertEquals(RebirthLife.CHILDHOOD,RebirthLife.phase(t));
        assertEquals(1,DemographyLife.get(t,child,"played-incarnation"));
        assertFalse(RebirthLife.acceptNaturalBirths(t,clock,java.util.List.of(child)));
        assertEquals(child.id,RebirthLife.child(t.copy()).id);
    }
    @Test void productionBirthPolicySurvivesFirstLaunchAndRestart()throws Exception{
        var file=folder.resolve("natural-birth.db");
        String id;PassportLife.View passport;int births;
        try(var game=new GameSession(file,12,true)){
            var world=game.snapshot();
            assertEquals("CHILDHOOD",world.rebirth().phase());
            assertEquals(0,world.rebirth().age());
            assertEquals(1,world.day());
            assertTrue(world.rebirth().pastLives().isEmpty());
            assertNotNull(world.rebirth().parentA());assertNotNull(world.rebirth().parentB());
            id=world.rebirth().identity().residentId();
            passport=world.player().passport();assertNotNull(passport);
            births=world.demography().births();assertTrue(births>=1);
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-adult"));
        }
        try(var game=new GameSession(file,12,true)){
            assertEquals("CHILDHOOD",game.snapshot().rebirth().phase());
            assertEquals(id,game.snapshot().rebirth().identity().residentId());
            assertEquals(passport,game.snapshot().player().passport());
            assertEquals(births,game.snapshot().demography().births());
            assertTrue(game.snapshot().rebirth().pastLives().isEmpty());
        }
    }
    @Test void deathContinuesAsOldestLivingChildWithoutCreatingResident(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,5);
        var parent=people.get(0);var oldest=people.get(1);var twin=people.get(2);
        var younger=people.get(3);var deadOlder=people.get(4);
        parent.age=60;oldest.age=30;twin.age=30;younger.age=25;deadOlder.age=35;
        for(var c:java.util.List.of(oldest,twin,younger,deadOlder))
            DemographyLife.set(t,c,"parent-a",parent.generationIndex);
        DemographyLife.set(t,oldest,"born",100);DemographyLife.set(t,twin,"born",100);
        DemographyLife.set(t,younger,"born",1925);deadOlder.alive=false;
        oldest.money=123;twin.money=456;
        t.industryStocks.put(RebirthLife.CHILD,parent.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        DemographyLife.set(t,parent,"played-incarnation",t.player.incarnation);
        RebirthLife.enableRandomBirth(t,false);
        parent.alive=false;parent.money=0;
        int count=t.people.size(),incarnation=t.player.incarnation;
        int day=12000;
        var expected=RebirthLife.eldestSuccessor(t,parent,day,incarnation);
        assertTrue(expected==oldest||expected==twin);
        var copy=t.copy();
        assertEquals(expected.id,RebirthLife.eldestSuccessor(copy,copy.get(parent.id),day,incarnation).id);
        int wallet=expected.money;
        assertTrue(RebirthLife.settleLinkedDeath(t,day,360));
        assertTrue(RebirthLife.returnPending(t));assertNull(RebirthLife.child(t));
        assertEquals(0,DemographyLife.get(t,expected,"played-incarnation"));
        var waiting=t.copy();assertTrue(RebirthLife.returnPending(waiting));
        RebirthLife.resumeAfterDeath(t,day);
        assertSame(expected,RebirthLife.child(t));
        assertEquals(RebirthLife.LINKED_ADULT,RebirthLife.phase(t));
        assertEquals(30,expected.age);assertEquals(wallet,expected.money);
        assertEquals(count,t.people.size());
        assertEquals(incarnation+1,t.player.incarnation);
        assertEquals(incarnation+1,DemographyLife.get(t,expected,"played-incarnation"));
        assertTrue(t.pastLives.containsKey(incarnation));
        assertFalse(RebirthLife.settleLinkedDeath(t,day,360));
    }
    @Test void successorCanBeMinorAndUsesExactBirthDate(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var parent=people.get(0);var older=people.get(1);var younger=people.get(2);
        older.age=12;younger.age=12;
        for(var c:java.util.List.of(older,younger))
            DemographyLife.set(t,c,"parent-b",parent.generationIndex);
        DemographyLife.set(t,older,"born",100);DemographyLife.set(t,younger,"born",150);
        t.industryStocks.put(RebirthLife.CHILD,parent.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        DemographyLife.set(t,parent,"played-incarnation",t.player.incarnation);
        parent.alive=false;
        assertTrue(RebirthLife.settleLinkedDeath(t,4600,360));
        RebirthLife.resumeAfterDeath(t,4600);
        assertSame(older,RebirthLife.child(t));
        assertEquals(12,older.age);assertEquals(RebirthLife.CHILDHOOD,RebirthLife.phase(t));
    }
    @Test void returnChoosesOnlyChildrenStillAvailable(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var parent=people.get(0);var older=people.get(1);var younger=people.get(2);
        older.age=40;younger.age=30;
        for(var c:java.util.List.of(older,younger))DemographyLife.set(t,c,"parent-a",parent.generationIndex);
        t.industryStocks.put(RebirthLife.CHILD,parent.generationIndex);
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        DemographyLife.set(t,parent,"played-incarnation",t.player.incarnation);
        parent.alive=false;
        assertTrue(RebirthLife.settleLinkedDeath(t,100,360));
        assertNull(RebirthLife.child(t));
        FirstLifeChoice.reserveForAccount(t,older,99);
        younger.age=35;
        RebirthLife.resumeAfterDeath(t,1925);
        assertSame(younger,RebirthLife.child(t));assertEquals(35,younger.age);
    }
    @Test void initialFamiliesDoNotAlterLaterLives(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var child=RebirthLife.child(t);
        int count=t.people.size();
        assertFalse(InitialFamilyLife.start(t,s));
        assertEquals(count,t.people.size());
        child.alive=false;
        assertTrue(RebirthLife.settleLinkedDeath(t,1,s.minute));
        assertFalse(InitialFamilyLife.start(t,s));
        assertEquals(RebirthLife.CHOICE,RebirthLife.phase(t));
        assertEquals(count,t.people.size());
    }
    @Test void earlyChildhoodUsesActualPresentParentsAndFiniteSupplies(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var child=RebirthLife.child(t);
        var a=DemographyLife.byIndex(t,DemographyLife.get(t,child,"parent-a"));
        var b=DemographyLife.byIndex(t,DemographyLife.get(t,child,"parent-b"));
        for(var p:java.util.List.of(a,b)){p.place=p.home;p.destination=p.home;p.activity="AT_HOME";p.travelRemaining=0;p.food=3;p.water=3;}
        child.food=0;child.water=0;
        var view=EarlyChildhoodLife.view(t);
        assertTrue(view.atHome());assertTrue(view.parentsPresent().contains(a.name));
        assertTrue(view.actions().contains("play"));
        EarlyChildhoodLife.act(t,"call");
        assertEquals(1,child.food);assertEquals(2,a.food);
        a.activity="SLEEPING";b.activity="SLEEPING";
        assertFalse(EarlyChildhoodLife.view(t).actions().contains("play"));
        child.age=6;
        assertNotNull(EarlyChildhoodLife.view(t)); // The prologue now lasts until adulthood.
    }
    @Test void acceleratedProloguePreservesResidentAndFinishesAtFamilyHome(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var child=RebirthLife.child(t);String id=child.id;String home=child.homeUnit;
        for(int i=0;i<179;i++)ChildhoodPrologue.step(t);
        assertEquals(17,child.age);
        assertEquals(1790,ChildhoodPrologue.view(t).elapsedSeconds());
        assertEquals(1790,ChildhoodPrologue.view(t.copy()).elapsedSeconds());
        ChildhoodPrologue.step(t);
        assertEquals(18,child.age);
        assertEquals(RebirthLife.LINKED_ADULT,RebirthLife.phase(t));
        assertEquals(id,RebirthLife.child(t).id);
        assertEquals(home,child.homeUnit);
        assertTrue(RebirthLife.stayingHome(t,child));
        assertNull(ChildhoodPrologue.view(t));
        assertEquals(0,EducationLife.get(t,child,"school-days"));
    }
    @Test void parentsHaveDifferentExpectationsAndRealHouseholdContributions(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var p=people.get(0);var c=people.get(1);var other=people.get(2);
        p.age=40;c.age=18;other.age=40;
        p.personalSeed=20;p.sociability=70;p.carefulness=90;p.curiosity=50;
        other.personalSeed=20;other.sociability=70;other.carefulness=40;other.curiosity=40;
        c.home=p.home;c.homeUnit=p.homeUnit;
        for(var person:java.util.List.of(c,p)){person.place=person.home;person.destination=person.home;person.activity="AT_HOME";person.travelRemaining=0;}
        DemographyLife.set(t,c,"parent-a",p.generationIndex);
        c.employed=false;c.money=100;p.money=100;
        assertEquals(ParentingLife.Style.STRICT,ParentingLife.style(p));
        assertEquals(ParentingLife.Style.WARM,ParentingLife.style(other));
        ParentingLife.prompt(t,c,1);
        assertEquals("FIND_WORK",ParentingLife.view(t,c).getFirst().request());
        assertFalse(ParentingLife.view(t,c).getFirst().canAccept());
        ParentingLife.respond(t,c,p,false);
        assertTrue(ParentingLife.relation(t,c,p).tension>8);
        ParentingLife.prompt(t,c,1);
        assertTrue(ParentingLife.view(t,c).isEmpty());
        c.employed=true;ParentingLife.prompt(t,c,2);
        assertEquals("CONTRIBUTE",ParentingLife.view(t,c).getFirst().request());
        ParentingLife.respond(t,c,p,true);
        assertEquals(90,c.money);assertEquals(110,p.money);
        assertEquals(c.homeUnit,p.homeUnit);
        assertTrue(ParentingLife.view(t,c).isEmpty());
    }
    @Test void newbornCanLeaveHomeAndMissingReportDoesNotMoveTheChild(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var c=RebirthLife.child(t);
        s.minute=420;
        RebirthLife.childExit(t,s);
        assertFalse(EarlyChildhoodLife.atHome(c));
        assertEquals("OUTSIDE_HOME",c.activity);
        s.minute=435;ChildSearchLife.tick(t,s);
        var p=ParentingLife.parents(t,c).getFirst();
        // No parent is physically in the child's public space.
        s.minute=465;ChildSearchLife.tick(t,s);
        assertTrue(ChildSearchLife.view(t,c,s).policeReported());
        assertEquals(c.home,c.place);
        assertEquals(0,c.age);
        RebirthLife.childReturn(t);
        s.minute=480;ChildSearchLife.tick(t,s);
        assertTrue(EarlyChildhoodLife.atHome(c));
        assertEquals("NONE",ChildSearchLife.view(t,c,s).status());
    }
    @Test void spontaneousParentSpeechUsesRealPresenceAndDoesNotForceAnAction(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var c=RebirthLife.child(t);
        var parent=ParentingLife.parents(t,c).getFirst();
        parent.place=c.home;parent.destination=c.home;parent.homeUnit=c.homeUnit;parent.home=c.home;
        parent.activity="AT_HOME";parent.travelRemaining=0;parent.health=100;parent.hunger=0;parent.thirst=0;parent.fatigue=0;
        c.activity="AT_HOME";
        var before=t.industryStocks.size();
        var offers=SpontaneousSpeechLife.view(t,1,600);
        assertTrue(offers.stream().anyMatch(o->o.residentId().equals(parent.id)&&o.kind().equals("FAMILY_GREETING")));
        assertEquals(offers,SpontaneousSpeechLife.view(t,1,600));
        assertEquals(before,t.industryStocks.size());
        parent.activity="SLEEPING";
        assertTrue(SpontaneousSpeechLife.view(t,1,600).stream().noneMatch(o->o.residentId().equals(parent.id)));
        parent.activity="AT_HOME";parent.travelRemaining=3;
        assertTrue(SpontaneousSpeechLife.view(t,1,600).stream().noneMatch(o->o.residentId().equals(parent.id)));
    }
    @Test void offlineRoutineIsBoundedAndKeepsTheLinkedIdentity(){
        assertEquals(0,OfflineRoutineLife.minutes(0,9999));
        assertEquals(0,OfflineRoutineLife.minutes(1000,999));
        assertEquals(0,OfflineRoutineLife.minutes(1000,1059));
        assertEquals(2,OfflineRoutineLife.minutes(1000,1120));
        assertEquals(180,OfflineRoutineLife.minutes(1000,100000));
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var c=RebirthLife.child(t);
        int money=c.money;
        RebirthLife.childExit(t,s);
        OfflineRoutineLife.prepare(t);
        assertEquals(RebirthLife.CHILDHOOD,RebirthLife.phase(t));
        assertSame(c,RebirthLife.child(t));
        assertEquals(money,c.money);
        assertEquals(0,ChildhoodPrologue.view(t).elapsedSeconds());
        assertNull(RebirthLife.childOuting(t,c,s.day,s.minute));
    }
    @Test void establishedPlayerHouseholdCanHaveNaturalBirthWithoutManualAction(){
        var s=new WorldState();var t=TownLife.create(s);
        PopulationGenerator.add(t,20);
        RebirthLife.enableRandomBirth(t,true);
        assertTrue(InitialFamilyLife.start(t,s));
        var c=RebirthLife.child(t);
        for(int i=0;i<180;i++)ChildhoodPrologue.step(t);
        c.age=25;c.money=500;
        var p=PopulationGenerator.add(t,1).getFirst();
        p.age=25;p.money=500;p.home=c.home;p.homeUnit=c.homeUnit;
        var r=ParentingLife.relation(t,c,p);r.affinity=90;r.trust=90;r.tension=0;
        assertTrue(DemographyLife.pair(t,c,p,1));
        assertTrue(DemographyLife.automaticBirthAllowed(t,c,p));
        assertFalse(RebirthLife.canHaveChild(t,c,p,31));
        s.day=31;s.minute=360;
        DemographyLife.tick(t,s,new java.util.ArrayList<>(),new java.util.ArrayList<>());
        var children=t.people.values().stream().filter(x->DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex).toList();
        assertEquals(1,children.size());
        assertEquals(0,children.getFirst().age);
        assertEquals(c.homeUnit,children.getFirst().homeUnit);
        assertEquals(0,DemographyLife.get(t,children.getFirst(),"played-incarnation"));
        assertSame(c,RebirthLife.child(t));
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
