package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommercialPropertyLifeTest {
    @Test void constructionUsesRealStockAndRentGoesToHeir(){
        var s=new WorldState();s.day=10;s.minute=840;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
        var people=PopulationGenerator.add(t,4);
        var owner=people.get(0);var engineer=people.get(1);var child=people.get(2);var tenant=people.get(3);
        owner.age=40;owner.money=500;owner.carefulness=90;owner.curiosity=90;owner.sickUntilDay=0;
        engineer.age=35;engineer.employed=true;engineer.occupation="Engineer";engineer.workplace=TownLife.Place.WORKSHOP;
        engineer.place=engineer.workplace;engineer.travelRemaining=0;engineer.activity="WORKING";engineer.fatigue=0;engineer.sickUntilDay=0;
        child.age=20;DemographyLife.set(t,child,"parent-a",owner.generationIndex);
        String p=CommercialPropertyLife.PLOTS.getFirst();
        assertTrue(CommercialPropertyLife.buy(t,s,p,owner));assertEquals(410,owner.money);
        t.industryStocks.put(IndustryLife.FACTORY_STEEL,3);FactoryNetworkLife.change(t,"building-material",3);
        for(int day=10;day<13;day++){s.day=day;assertTrue(CommercialPropertyLife.build(t,s,p));assertFalse(CommercialPropertyLife.build(t,s,p));}
        assertEquals(356,owner.money);assertEquals(0,IndustryLife.stock(t,IndustryLife.FACTORY_STEEL));
        assertEquals(0,FactoryNetworkLife.stock(t,"building-material"));
        assertEquals(3,CommercialPropertyLife.view(t.copy(),13).getFirst().stage());
        owner.alive=false;CommercialPropertyLife.estate(t,p);
        int before=child.money;CommercialPropertyLife.rent(t,tenant,13);
        assertEquals(before+ITBusinessLife.RENT,child.money);
        assertEquals(child.name,CommercialPropertyLife.view(t,13).getFirst().owner());
        assertEquals(tenant.name,CommercialPropertyLife.view(t,13).getFirst().tenant());
    }
}
