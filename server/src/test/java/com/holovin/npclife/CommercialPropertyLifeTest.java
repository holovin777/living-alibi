package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommercialPropertyLifeTest {
    @Test void constructionUsesRealStockAndRentGoesToHeir(){
        var s=new WorldState();s.day=10;s.minute=840;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
        var people=PopulationGenerator.add(t,6);
        var owner=people.get(0);var engineer=people.get(1);var child=people.get(2);var tenant=people.get(3);
        var driver=people.get(4);var builder=people.get(5);
        owner.age=40;owner.money=500;owner.carefulness=90;owner.curiosity=90;owner.sickUntilDay=0;
        engineer.age=35;engineer.employed=true;engineer.occupation=ConstructionCrewLife.ARCHITECT;engineer.workplace=TownLife.Place.WORKSHOP;
        engineer.place=engineer.workplace;engineer.travelRemaining=0;engineer.activity="WORKING";engineer.fatigue=0;engineer.sickUntilDay=0;
        for(var w:java.util.List.of(driver,builder)){w.age=35;w.employed=true;w.workplace=TownLife.Place.WORKSHOP;w.place=w.workplace;w.travelRemaining=0;w.activity="WORKING";w.fatigue=0;w.sickUntilDay=0;}
        driver.occupation=ConstructionCrewLife.DRIVER;builder.occupation=ConstructionCrewLife.BUILDER;
        child.age=20;DemographyLife.set(t,child,"parent-a",owner.generationIndex);
        String p=CommercialPropertyLife.PLOTS.getFirst();
        assertTrue(CommercialPropertyLife.buy(t,s,p,owner));assertEquals(410,owner.money);
        t.industryStocks.put(IndustryLife.FACTORY_STEEL,3);FactoryNetworkLife.change(t,"building-material",3);SupplyChainLife.set(t,FuelLife.STOCK,3);
        for(int day=10;day<13;day++){s.day=day;assertTrue(CommercialPropertyLife.build(t,s,p));assertFalse(CommercialPropertyLife.build(t,s,p));}
        assertEquals(410-ConstructionCrewLife.total(),owner.money);assertEquals(0,FuelLife.stock(t));assertEquals(0,IndustryLife.stock(t,IndustryLife.FACTORY_STEEL));
        assertEquals(0,FactoryNetworkLife.stock(t,"building-material"));
        assertEquals(3,CommercialPropertyLife.view(t.copy(),13).getFirst().stage());
        owner.alive=false;CommercialPropertyLife.estate(t,p);
        int before=child.money;CommercialPropertyLife.rent(t,tenant,13);
        assertEquals(before+ITBusinessLife.RENT,child.money);
        assertEquals(child.name,CommercialPropertyLife.view(t,13).getFirst().owner());
        assertEquals(tenant.name,CommercialPropertyLife.view(t,13).getFirst().tenant());
    }
}
