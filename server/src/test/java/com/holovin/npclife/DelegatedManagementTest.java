package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelegatedManagementTest {
    @Test void managerRunsWithoutOwnerAndCannotSpendPersonalWallet(){
        var t=TownLife.create(new WorldState());
        var people=PopulationGenerator.add(t,2);
        var owner=people.get(0);var w=people.get(1);
        owner.money=200;owner.age=35;owner.place=TownLife.Place.PARK;
        w.age=30;w.money=30;w.employed=true;w.occupation=ITStaffLife.OCCUPATION;
        w.workplace=TownLife.Place.OFFICE;w.place=w.workplace;w.travelRemaining=0;w.activity="WORKING";w.fatigue=0;
        ITBusinessLife.set(t,owner,"registered",1);ITBusinessLife.set(t,owner,"cash",100);
        ITBusinessLife.set(t,owner,"rent-until",8);ITBusinessLife.set(t,owner,"managed",1);
        ITBusinessLife.set(t,owner,"employee",w.generationIndex);ITBusinessLife.set(t,w,"employer",owner.generationIndex);
        EducationLife.set(t,w,"technical",100);
        int day=2;w.personalSeed=(long)day*0x9E3779B97F4A7C15L;
        t.industryStocks.put(SoftwareLife.key("retail-api"),3);
        t.employerFunds.put("shop-assistant",500);
        ITStaffLife.managedShift(t,owner,day,900);
        assertEquals(200,owner.money);assertEquals(42,w.money);
        assertEquals(120,ITBusinessLife.get(t,owner,"cash"));
        assertEquals(460,t.employerFunds.get("shop-assistant"));
        ITStaffLife.managedShift(t,owner,day,900);
        assertEquals(120,ITBusinessLife.get(t,owner,"cash"));
        ITBusinessLife.set(t,owner,"cash",0);
        ITStaffLife.tick(t,9,390);
        assertTrue(ITBusinessLife.registered(t,owner));
        assertEquals(1,ITBusinessLife.get(t,owner,"managed"));
    }
}
