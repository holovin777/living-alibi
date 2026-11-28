package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResidentialDevelopmentLifeTest {
 @Test void unbuiltHomeCannotBeOccupiedAndPurchaseUsesPersonalCash(){
    var s=new WorldState();s.day=10;s.minute=660;
    var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);
    var people=PopulationGenerator.add(t,2);var owner=people.get(0);var child=people.get(1);
    owner.age=40;owner.carefulness=90;owner.curiosity=90;owner.sickUntilDay=0;owner.money=600;owner.unpaidBills=0;
    owner.homeUnit=TownLife.privateUnit(owner.id);child.homeUnit=owner.homeUnit;child.home=owner.home;
    child.age=8;
    var p=ResidentialDevelopmentLife.LOTS.getFirst();int city=t.employerFunds.getOrDefault("city-maintenance",0);
    assertTrue(ResidentialDevelopmentLife.buy(t,s,p,owner));
    assertEquals(500,owner.money);assertEquals(city+100,t.employerFunds.get("city-maintenance"));
    assertFalse(ResidentialDevelopmentLife.buy(t,s,p,owner));
    assertFalse(ResidentialDevelopmentLife.move(t,p));assertFalse(ResidentialDevelopmentLife.connect(t,s,p));
    assertEquals(owner.homeUnit,child.homeUnit);
    assertEquals(owner.name,ResidentialDevelopmentLife.view(t.copy()).getFirst().owner());
 }
}
