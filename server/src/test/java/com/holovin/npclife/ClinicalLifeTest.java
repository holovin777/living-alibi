package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClinicalLifeTest {
    @Test void realSpecialistDiagnosesAndFollowupPersists(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);var patient=people.get(0);var doctor=people.get(1);var child=people.get(2);
        patient.age=35;patient.place=TownLife.Place.CLINIC;patient.travelRemaining=0;patient.activity="FREE_TIME";
        doctor.age=40;doctor.employed=true;doctor.occupation="Doctor";doctor.place=TownLife.Place.CLINIC;
        doctor.travelRemaining=0;doctor.activity="WORKING";doctor.personalSeed=ClinicalLife.Specialty.NEUROLOGY.ordinal();
        var kind=ClinicalLife.Condition.NEUROLOGICAL;
        ClinicalLife.set(t,patient,ClinicalLife.field(kind,"severity"),40);
        ClinicalLife.inherit(t,child,patient,doctor);
        assertTrue(ClinicalLife.get(t,child,ClinicalLife.field(kind,"risk"))>0);
        assertEquals(0,ClinicalLife.severity(t,child,kind));
        s.day=1;s.minute=600;ClinicalLife.request(t,patient,s,ClinicalLife.Specialty.NEUROLOGY);
        assertTrue(ClinicalLife.view(t,patient,s).diagnosed().isEmpty());
        doctor.activity="FREE_TIME";ClinicalLife.tick(t,s);
        assertEquals(0,ClinicalLife.view(t,patient,s).visits());
        doctor.activity="WORKING";ClinicalLife.tick(t,s);
        assertEquals(1,ClinicalLife.view(t,patient,s).visits());
        assertEquals("FOLLOW-UP",ClinicalLife.view(t,patient,s).diagnosed().getFirst().status());
        var copy=t.copy();assertEquals(1,ClinicalLife.view(copy,copy.get(patient.id),s).visits());
        s.day=2;s.minute=360;ClinicalLife.tick(t,s);
        assertEquals(39,ClinicalLife.severity(t,patient,kind));
    }
}
