package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireHousingLifeTest {
    @Test void shelterAllocatesWholeHouseholdAndChargesOncePerDay(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);
        var residents=t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(2).toList();
        assertEquals(2,residents.size());
        for(var c:t.people.values())if(c.generationIndex>0)c.home=TownLife.Place.PARK;
        for(var c:residents){c.home=TownLife.Place.NORTH_RESIDENCES;c.homeUnit="fire-test-household";}
        FireRecoveryLife.damage(t,TownLife.Place.NORTH_RESIDENCES,50);
        t.employerFunds.put("city-maintenance",3);
        FireHousingLife.prepare(t,s);assertEquals(0,FireHousingLife.view(t).accommodated());
        t.employerFunds.put("city-maintenance",100);
        FireHousingLife.prepare(t,s);assertEquals(2,FireHousingLife.view(t).accommodated());
        assertEquals(96,t.employerFunds.get("city-maintenance").intValue());
        for(var c:residents)assertEquals(FireHousingLife.PLACE,FireEvacuationLife.destination(t,c,c.home));
        FireHousingLife.prepare(t,s);assertEquals(96,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(2,FireHousingLife.view(t.copy()).reserved());
        s.day++;FireHousingLife.prepare(t,s);assertEquals(92,t.employerFunds.get("city-maintenance").intValue());
        t.player.place=TownLife.Place.NORTH_RESIDENCES;t.player.destination=t.player.place;t.player.engineeringSkill=3;
        int stock=FactoryNetworkLife.stock(t,"building-material");if(stock<1)FactoryNetworkLife.change(t,"building-material",1);
        FireRecoveryLife.playerRepair(t,s,TownLife.Place.NORTH_RESIDENCES);
        assertEquals(0,FireHousingLife.view(t).accommodated());
    }
}
