package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IdentityDialogueRegressionTest {
    @Test void understandsShortItalianIntroductionsWithoutTreatingMoodAsName(){
        assertEquals("My name is Lorenzo.",NaturalDialogue.canonical("Sono Lorenzo",null));
        assertEquals("My name is Lorenzo Galli.",NaturalDialogue.canonical("Ciao, sono Lorenzo Galli!",null));
        assertEquals("@comfort",NaturalDialogue.canonical("Sono triste",null));
        assertEquals("@comfort",NaturalDialogue.canonical("Sono Triste",null));
    }
    @Test void biologicalBirthRequiresDifferentSexes(){
        var t=TownLife.create(new WorldState());
        var residents=PopulationGenerator.add(t,2);
        var a=residents.get(0);var b=residents.get(1);
        a.age=30;b.age=30;a.money=200;b.money=200;
        DemographyLife.set(t,a,"sex",ResidentSexLife.MALE);
        DemographyLife.set(t,b,"sex",ResidentSexLife.MALE);
        DemographyLife.set(t,a,"partner",b.generationIndex);
        DemographyLife.set(t,b,"partner",a.generationIndex);
        assertFalse(DemographyLife.canBirth(t,a,b,300));
        DemographyLife.set(t,b,"sex",ResidentSexLife.FEMALE);
        assertTrue(DemographyLife.canBirth(t,a,b,300));
    }
}
