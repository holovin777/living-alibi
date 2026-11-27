package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Equipment condition and paid local maintenance for network plants.
 * Abstract batches, not component-level machines or a technician travel service. */
final class FactoryMaintenance {
    private FactoryMaintenance(){}
    static final int STOP=25, SERVICE_AT=65, WEAR=7, PART_PRICE=5, LABOR=4;
    static String key(FactoryNetworkLife.Plant p,String field){
        return "factory-maintenance:"+p.id()+":"+field;
    }
    static int condition(TownLife.Town t,FactoryNetworkLife.Plant p){
        return t.industryStocks.getOrDefault(key(p,"condition"),100);
    }
    static boolean operational(TownLife.Town t,FactoryNetworkLife.Plant p){
        return condition(t,p)>STOP;
    }
    static void wear(TownLife.Town t,FactoryNetworkLife.Plant p){
        t.industryStocks.put(key(p,"condition"),Math.max(0,condition(t,p)-WEAR));
    }
    static boolean service(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p){
        if(condition(t,p)>SERVICE_AT||t.industryStocks.getOrDefault(key(p,"last-repair"),0)>=s.day)return false;
        boolean factory=p.site()==TownLife.Place.FACTORY;
        String supplier=factory?"machinist":"city-maintenance";
        int parts=factory?SupplyChainLife.get(t,IndustryLife.FACTORY_PARTS):t.technicalMaterials.getOrDefault("spare-parts",0);
        String account=FactoryNetworkLife.account(p);
        int cash=t.employerFunds.getOrDefault(account,0);
        if(parts<1||cash<PART_PRICE+LABOR)return false;
        var tech=t.people.values().stream().filter(c->WorkplaceHierarchy.onDuty(c,s.day)
                &&c.workplace==p.site()&&c.fatigue<80
                &&List.of("Mechanic","Electrician","Factory machinist").contains(c.occupation)
                &&!RebirthLife.linkedAdult(t,c)&&!TownLife.inPlayerConversation(t,c)
                &&t.industryStocks.getOrDefault("factory-maintenance:tech:"+c.id,0)<s.day)
            .min(Comparator.comparing(c->c.id)).orElse(null);
        if(tech==null)return false;
        int wallet=Math.addExact(tech.money,LABOR);
        int supplierCash=Math.addExact(t.employerFunds.getOrDefault(supplier,0),PART_PRICE);
        t.employerFunds.put(account,cash-PART_PRICE-LABOR);
        t.employerFunds.put(supplier,supplierCash);
        tech.money=wallet;
        if(factory)SupplyChainLife.set(t,IndustryLife.FACTORY_PARTS,parts-1);
        else t.technicalMaterials.put("spare-parts",parts-1);
        t.industryStocks.put(key(p,"condition"),Math.min(100,condition(t,p)+40));
        t.industryStocks.put(key(p,"last-repair"),s.day);
        t.industryStocks.put("factory-maintenance:tech:"+tech.id,s.day);
        return true;
    }
}
