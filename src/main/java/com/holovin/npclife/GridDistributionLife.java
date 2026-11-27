package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** District distribution feeders, simulated condition and paid maintenance. */
final class GridDistributionLife {
    private GridDistributionLife(){}
    static boolean role(String job){return List.of("Centre grid technician","North grid technician","South grid technician","Industrial grid technician").contains(job); }
    static final int MIN_CONDITION=25, SERVICE_AT=60, PART_PRICE=5, LABOR=4;
    record Feeder(String id,TownLife.Place site,boolean industrial){}
    static final List<Feeder> FEEDERS=List.of(
        new Feeder("centre",TownLife.Place.OFFICE,false),
        new Feeder("north",TownLife.Place.NORTH_RESIDENCES,false),
        new Feeder("south",TownLife.Place.SOUTH_RESIDENCES,false),
        new Feeder("industry",TownLife.Place.FACTORY,true));
    static String key(Feeder f,String field){return "grid:"+f.id()+":"+field;}
    static int condition(TownLife.Town t,Feeder f){return t.industryStocks.getOrDefault("energy:"+key(f,"condition"),100);}
    static boolean operational(TownLife.Town t,Feeder f){return condition(t,f)>MIN_CONDITION;}
    static Feeder feeder(TownLife.Place p){
        return switch(p){
            case NORTH_RESIDENCES -> FEEDERS.get(1);
            case SOUTH_RESIDENCES,MARCO_HOME,ELENA_HOME,BORGO_SUD -> FEEDERS.get(2);
            case FACTORY,WORKSHOP,QUARRY,LOGISTICS_DEPOT,FARM,FUEL_STATION,RIVER -> FEEDERS.get(3);
            default -> FEEDERS.get(0);
        };
    }
    static boolean power(TownLife.Town t,TownLife.Place p){
        if(!EnergyLife.enabled(t))return true;
        var f=feeder(p);
        return operational(t,f)&&(f.industrial()?EnergyLife.get(t,"battery")>0:EnergyLife.publicPower(t));
    }
    static void repair(TownLife.Town t,WorldState s,Feeder f){
        if(condition(t,f)>SERVICE_AT||EnergyLife.cash(t)<PART_PRICE+LABOR
            ||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return;
        var tech=t.people.values().stream().filter(c->role(c.occupation)&&c.workplace==f.site()
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<80
            &&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c)
            &&EnergyLife.get(t,"grid-tech:"+c.id)<s.day)
            .min(Comparator.comparing(c->c.id)).orElse(null);
        if(tech==null)return;
        if(!SupplyChainLife.pay(t,EnergyLife.ACCOUNT,"city-maintenance",PART_PRICE))return;
        t.employerFunds.merge(EnergyLife.ACCOUNT,-LABOR,Math::addExact);
        tech.money=Math.addExact(tech.money,LABOR);
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        EnergyLife.set(t,key(f,"condition"),Math.min(100,condition(t,f)+45));
        EnergyLife.set(t,"grid-tech:"+tech.id,s.day);
        EnergyLife.add(t,key(f,"repairs"),1);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!EnergyLife.enabled(t)||EnergyLife.get(t,"grid-day")>=s.day)return;
        EnergyLife.set(t,"grid-day",s.day);
        for(var f:FEEDERS){
            repair(t,s,f);
            if(power(t,f.site()))EnergyLife.set(t,key(f,"condition"),Math.max(0,condition(t,f)-2));
            if(!power(t,f.site()))EnergyLife.add(t,key(f,"outage-days"),1);
        }
    }
    record FeederView(String id,String district,int condition,boolean powered,int outageDays,int repairs){}
    static List<FeederView> view(TownLife.Town t){
        return FEEDERS.stream().map(f->new FeederView(f.id(),f.site().name(),condition(t,f),power(t,f.site()),
            EnergyLife.get(t,key(f,"outage-days")),EnergyLife.get(t,key(f,"repairs")))).toList();
    }
}
