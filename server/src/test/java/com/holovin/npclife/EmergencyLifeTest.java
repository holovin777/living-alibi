package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmergencyLifeTest {
    @Test void treatmentRequiresStaffAndRestoresOnlyRecordedInjury(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        var patient=t.get("marco");patient.health=62;s.health=62;
        var clinician=t.get("elena");clinician.occupation="Nurse";clinician.employed=true;
        clinician.place=TownLife.Place.CLINIC;clinician.travelRemaining=0;clinician.activity="FREE_TIME";
        EmergencyLife.request(t,s,patient,78);
        assertTrue(EmergencyLife.pending(t.copy(),patient));
        s.minute=620;EmergencyLife.tick(t,s);assertEquals(62,patient.health);
        clinician.activity="WORKING";
        EmergencyLife.tick(t,s);assertEquals(70,patient.health);assertEquals(70,s.health);
        s.minute=640;EmergencyLife.tick(t,s);assertEquals(78,patient.health);
        assertFalse(EmergencyLife.pending(t,patient));assertEquals(1,EmergencyLife.completed(t));
        EmergencyLife.tick(t,s);assertEquals(1,EmergencyLife.completed(t));
    }
}
