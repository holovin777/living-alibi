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

    private static TownLife.Citizen worker(TownLife.Town town,String id,String role) {
        var person=new TownLife.Citizen(id,id,role,TownLife.Place.OFFICE);
        person.activity="WORKING";
        town.people.put(id,person);
        return person;
    }
}
