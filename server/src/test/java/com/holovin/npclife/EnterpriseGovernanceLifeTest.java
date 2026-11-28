package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnterpriseGovernanceLifeTest {
    @Test void directorAuthorizesInterviewAndCashReportSurvivesCopy(){
        var s=new WorldState();s.day=2;s.minute=600;
        var t=TownLife.create(s);
        var residents=PopulationGenerator.add(t,4);
        t.industryStocks.put(StaffingLife.ENABLED,1);
        String a="shop-assistant";
        var director=residents.get(0);var candidate=residents.get(1);
        for(var c:residents){c.age=35;c.employed=false;c.sickUntilDay=0;c.fatigue=0;}
        director.employed=true;director.occupation="Market vendor";director.workplace=TownLife.Place.MARKET;
        director.place=director.workplace;director.travelRemaining=0;director.activity="WORKING";
        candidate.carefulness=80;candidate.sociability=80;
        var role=StaffingLife.role("Market vendor");
        EducationLife.learn(t,candidate,EducationLife.subject(role.name()),100);
        for(var c:residents)if(c!=candidate&&c!=director)c.age=10;
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        t.industryStocks.put("business-owner:"+a,t.player.incarnation);t.employerFunds.put(a,500);
        BusinessManagementLife.action(t,s,a,"director");
        s.minute=390;EnterpriseGovernanceLife.tick(t,s);
        s.minute=600;EnterpriseGovernanceLife.tick(t,s);
        var first=EnterpriseGovernanceLife.view(t,a);
        assertEquals("RECRUIT",first.decision());
        assertEquals(candidate.name,first.candidate());
        assertEquals(3,first.appointmentDay());
        assertFalse(candidate.employed);
        BusinessSolvencyLife.payHour(t,director,s);
        s.day=3;s.minute=390;EnterpriseGovernanceLife.tick(t,s);
        var report=EnterpriseGovernanceLife.view(t.copy(),a).reports().getFirst();
        assertEquals(500,report.openingCash());assertEquals(496,report.closingCash());
        assertEquals(4,report.payrollPaid());assertEquals(-4,report.netCashChange());
        s.minute=600;EnterpriseGovernanceLife.tick(t,s);
        assertTrue(candidate.employed);
        assertEquals("HIRED",EnterpriseGovernanceLife.view(t,a).interviewResult());
        EnterpriseGovernanceLife.tick(t,s);
        assertEquals(2,BusinessManagementLife.members(t,a).size());
    }
}
