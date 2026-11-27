package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceDialogueTest {
    @Test void workplaceQuestionsRecognizedInThreeLanguages() {
        assertEquals("@coworker",NaturalDialogue.canonical("Siamo colleghi?",null));
        assertEquals("@shift-team",NaturalDialogue.canonical("Chi è di turno?",null));
        assertEquals("@work-advice",NaturalDialogue.canonical("What is our task?",null));
        assertEquals("@work-break",NaturalDialogue.canonical("Зробимо перерву?",null));
    }

    @Test void shiftCountExcludesOffDutySickUnemployedAndDifferentRoles() {
        var town=new TownLife.Town();
        var lead=worker(town,"lead","Programmer");
        var other=worker(town,"other","Programmer");
        worker(town,"different","Teacher");
        var sick=worker(town,"sick","Programmer");sick.sickUntilDay=2;
        var absent=worker(town,"absent","Programmer");absent.activity="FREE_TIME";
        var unemployed=worker(town,"unemployed","Programmer");unemployed.employed=false;
        var contact=new PlayerLife.Contact(lead.id);
        assertEquals("There are 1 other workers in my role on shift here right now.",
            NaturalDialogue.reply(town,lead,contact,"@shift-team",1,600));
        other.travelRemaining=5;
        assertEquals("I'm the only person in my role working here right now.",
            NaturalDialogue.reply(town,lead,contact,"@shift-team",1,600));
        lead.activity="FREE_TIME";
        assertEquals("I'm not on shift right now. Ask me at work.",
            NaturalDialogue.reply(town,lead,contact,"@shift-team",1,600));
        lead.employed=false;
        assertEquals("I don't have a job at the moment.",
            NaturalDialogue.reply(town,lead,contact,"@work-advice",1,600));
    }

    @Test void workplaceMoodAndJokesRecognizedInThreeLanguages() {
        for(String phrase:new String[]{"How is your shift?","Come va il lavoro?","Як робота?"})
            assertEquals("@work-mood",NaturalDialogue.canonical(phrase,null));
        for(String phrase:new String[]{"Tell me a joke","Raccontami una barzelletta","Розкажи жарт"})
            assertEquals("@work-joke",NaturalDialogue.canonical(phrase,null));
    }

    @Test void workplacePersonalityAndNeedsChangeReplies() {
        var town=new TownLife.Town();
        var person=worker(town,"joker","Programmer");
        var contact=new PlayerLife.Contact(person.id);
        person.sociability=90;person.carefulness=45;contact.trust=65;
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-mood",1,600).contains("conversation"));
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-joke",1,600).contains("tests"));
        contact.trust=20;
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-joke",1,600).contains("know each other"));
        contact.trust=65;person.carefulness=90;
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-mood",1,600).contains("concentrating"));
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-joke",1,600).contains("concentrate"));
        person.fatigue=85;
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-mood",1,600).contains("break"));
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-joke",1,600).contains("worn out"));
        person.activity="FREE_TIME";
        assertTrue(NaturalDialogue.reply(town,person,contact,"@work-mood",1,600).contains("off duty"));
    }

    @Test void hierarchyFollowsTenureSkillsAndCurrentEmployment() {
        var town=new TownLife.Town();
        var a=worker(town,"a","Programmer");
        var b=worker(town,"b","Programmer");
        var c=worker(town,"c","Teacher");
        a.lastHiredDay=1;b.lastHiredDay=38;c.lastHiredDay=1;
        EducationLife.set(town,a,"technical",70);
        EducationLife.set(town,b,"technical",65);
        EducationLife.set(town,c,"academic",100);
        assertEquals(WorkplaceHierarchy.Rank.TRAINEE,WorkplaceHierarchy.rank(town,b,40));
        assertEquals(WorkplaceHierarchy.Rank.TEAM_LEAD,WorkplaceHierarchy.rank(town,a,40));
        assertSame(a,WorkplaceHierarchy.lead(town,b,40));
        assertNull(WorkplaceHierarchy.lead(town,c,40));
        assertEquals(WorkplaceHierarchy.Rank.SENIOR,WorkplaceHierarchy.rank(town,b,55));
        a.activity="FREE_TIME";
        assertTrue(NaturalDialogue.reply(town,b,new PlayerLife.Contact(b.id),"@work-lead",55,600).contains("isn't on shift"));
        a.employed=false;
        assertNull(WorkplaceHierarchy.lead(town,b,55));
        a.employed=true;a.occupation="Teacher";
        assertNull(WorkplaceHierarchy.lead(town,b,55));
        assertEquals("@work-lead",NaturalDialogue.canonical("Chi è il responsabile?",null));
        assertEquals("@work-rank",NaturalDialogue.canonical("Яка твоя посада в команді?",null));
    }

    @Test void managementMentorsOnceAndRequiresActualAttendance() {
        var town=new TownLife.Town();
        var lead=worker(town,"lead","Programmer");
        var novice=worker(town,"novice","Programmer");
        var absent=worker(town,"absent","Programmer");
        lead.home=TownLife.Place.NORTH_RESIDENCES;novice.home=lead.home;absent.home=lead.home;
        lead.lastHiredDay=1;novice.lastHiredDay=38;absent.lastHiredDay=38;
        absent.sickUntilDay=40;
        EducationLife.set(town,lead,"technical",80);
        EducationLife.set(town,novice,"technical",45);
        EducationLife.set(town,absent,"technical",40);
        var s=new WorldState();s.day=40;s.minute=540;
        var events=new java.util.ArrayList<TownLife.Event>();
        var seen=new java.util.ArrayList<TownLife.Observation>();
        WorkplaceManagement.tick(town,s,events,seen);
        WorkplaceManagement.tick(town,s,events,seen);
        assertEquals(1,events.size());
        s.minute=600;
        WorkplaceManagement.tick(town,s,events,seen);
        WorkplaceManagement.tick(town,s,events,seen);
        assertEquals(46,EducationLife.get(town,novice,"technical"));
        assertEquals(40,EducationLife.get(town,absent,"technical"));
        assertEquals(2,events.size());
        assertEquals(40,town.industryStocks.get(WorkplaceManagement.teamKey(lead,"mentoring")).intValue());
        s.minute=840;
        lead.activity="FREE_TIME";
        WorkplaceManagement.tick(town,s,events,seen);
        assertEquals(2,events.size());
        lead.activity="WORKING";
        WorkplaceManagement.tick(town,s,events,seen);
        assertEquals(3,events.size());
    }

    @Test void performanceUsesWorkPresenceAndNeedsWithoutPunishingAbsence() {
        var t=new TownLife.Town();
        var good=worker(t,"good","Programmer");
        var tired=worker(t,"tired","Programmer");
        var absent=worker(t,"absent","Programmer");
        good.carefulness=80;tired.carefulness=80;
        tired.fatigue=95;tired.hunger=90;tired.thirst=90;tired.health=60;
        absent.activity="FREE_TIME";
        EducationLife.set(t,good,"technical",80);
        EducationLife.set(t,tired,"technical",80);
        var s=new WorldState();s.day=40;s.minute=600;
        WorkplacePerformance.tick(t,s);
        WorkplacePerformance.tick(t,s);
        var g=WorkplacePerformance.report(t,good);
        var bad=WorkplacePerformance.report(t,tired);
        assertEquals(1,g.samples());
        assertTrue(g.quality()>bad.quality());
        assertTrue(g.pace()>bad.pace());
        assertEquals(0,WorkplacePerformance.report(t,absent).samples());
        assertEquals(g,WorkplacePerformance.report(t.copy(),good));
        good.jobChanges++;
        assertEquals(0,WorkplacePerformance.report(t,good).samples());
        s.minute=660;
        WorkplacePerformance.tick(t,s);
        assertEquals(1,WorkplacePerformance.report(t,good).samples());
        good.sickUntilDay=40;s.minute=720;
        WorkplacePerformance.tick(t,s);
        assertEquals(1,WorkplacePerformance.report(t,good).samples());
    }

    private static TownLife.Citizen worker(TownLife.Town town,String id,String role) {
        var person=new TownLife.Citizen(id,id,role,TownLife.Place.OFFICE);
        person.activity="WORKING";
        town.people.put(id,person);
        return person;
    }
}
