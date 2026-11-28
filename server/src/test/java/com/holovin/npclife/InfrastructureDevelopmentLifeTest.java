package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfrastructureDevelopmentLifeTest {
 @Test void projectsRequireBudgetAndCompletedStagesChangeCapacity(){
    var s=new WorldState();s.day=10;s.minute=660;var t=TownLife.create(s);
    t.industryStocks.put(StaffingLife.ENABLED,1);
    var p=InfrastructureDevelopmentLife.SOLAR;
    InfrastructureDevelopmentLife.set(t,p,"pressure",4);
    t.employerFunds.put(MunicipalLife.CITY,40);
    assertFalse(InfrastructureDevelopmentLife.approve(t,s,p));
    t.employerFunds.put(MunicipalLife.CITY,200);
    assertTrue(InfrastructureDevelopmentLife.approve(t,s,p));
    assertFalse(InfrastructureDevelopmentLife.work(t,s,p));
    assertEquals(200,t.employerFunds.get(MunicipalLife.CITY));
    assertEquals(32,EnergyLife.capacity(t));
    InfrastructureDevelopmentLife.set(t,p,"stage",3);
    assertEquals(44,EnergyLife.capacity(t.copy()));
    InfrastructureDevelopmentLife.set(t,InfrastructureDevelopmentLife.WATER,"stage",3);
    InfrastructureDevelopmentLife.set(t,InfrastructureDevelopmentLife.SEWER,"stage",3);
    assertEquals(72,WaterNetworkLife.capacity(t));assertEquals(48,WaterNetworkLife.sewerCapacity(t));
 }
}
