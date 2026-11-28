package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessManagementLifeTest {
    @Test void ownerAppointsActualWorkerAndChangesPayrollAndHiringTarget(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);t.industryStocks.put(StaffingLife.ENABLED,1);
        var workers=t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(2).toList();
        String a="shop-assistant";
        for(var c:workers){c.age=35;c.employed=true;c.occupation="Market vendor";c.workplace=TownLife.Place.MARKET;c.sickUntilDay=0;}
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        t.industryStocks.put("business-owner:"+a,t.player.incarnation);t.employerFunds.put(a,200);
        BusinessManagementLife.action(t,s,a,"director");var director=BusinessManagementLife.director(t,a);
        assertNotNull(director);assertEquals("Market vendor",director.occupation);
        BusinessManagementLife.action(t,s,a,"raise");assertEquals(5,BusinessManagementLife.wage(t,a));
        int cash=director.money;BusinessSolvencyLife.payHour(t,director,s);
        assertEquals(cash+5,director.money);
        var role=StaffingLife.role("Market vendor");int target=BusinessManagementLife.target(t,role);
        BusinessManagementLife.action(t,s,a,"more");assertEquals(target+1,BusinessManagementLife.target(t,role));
        BusinessManagementLife.action(t,s,a,"dismiss");
        assertTrue(director.employed);assertTrue(workers.stream().anyMatch(c->!c.employed));
        assertEquals(director.name,BusinessManagementLife.view(t.copy(),a).director());
    }
}
