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

    private static TownLife.Citizen worker(TownLife.Town town,String id,String role) {
        var person=new TownLife.Citizen(id,id,role,TownLife.Place.OFFICE);
        person.activity="WORKING";
        town.people.put(id,person);
        return person;
    }
}
