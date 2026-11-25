package com.holovin.npclife;

import static com.holovin.npclife.TownLife.Place.*;

/** One player-operated employer truck, independent of the existing NPC truck. */
final class FreightLife {
    private FreightLife() {}
    static final String JOB="freight-driver", STAGE="freight:stage", IRON="freight:iron", COPPER="freight:copper",
            WAGES="freight:wage-escrow", PAYMENT="freight:ore-escrow";
    static final int WAGE=32;
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{STAGE,IRON,COPPER,WAGES,PAYMENT})t.industryStocks.putIfAbsent(key,0);
    }
    static int stock(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int amount){t.industryStocks.put(key,amount);}
    static boolean active(TownLife.Town t){return stock(t,STAGE)>0;}
    static TownLife.Place truckPlace(TownLife.Town t) {
        return switch(stock(t,STAGE)) {case 1->QUARRY;case 2->WORKSHOP;case 3->FACTORY;default->null;};
    }
    static TownLife.Place next(TownLife.Town t) {
        return switch(stock(t,STAGE)) {case 1->WORKSHOP;case 2->FACTORY;default->null;};
    }
    static void requireDriver(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(!JOB.equals(t.player.jobId))throw new IllegalArgumentException("Apply for the freight driver job first.");
    }
    static String load(TownLife.Town t,int day,int minute) {
        requireDriver(t);
        var p=t.player;
        if(p.place!=QUARRY || minute<480 || minute>=960)throw new IllegalArgumentException("Load at the quarry 08:00–16:00.");
        if(active(t) || p.lastWorkDay==day)throw new IllegalArgumentException("Finish your existing job or return tomorrow.");
        if(p.ownsBicycle && p.bicycleParked==null)throw new IllegalArgumentException("Park your bicycle first.");
        int iron=Math.min(2,IndustryLife.stock(t,IndustryLife.QUARRY_IRON));
        int copper=Math.min(1,IndustryLife.stock(t,IndustryLife.QUARRY_COPPER));
        int units=iron+copper,cost=units*3;
        if(units==0)throw new IllegalArgumentException("No ore waiting at the quarry.");
        if(t.employerFunds.getOrDefault(JOB,0)<WAGE+4 || t.employerFunds.getOrDefault("machinist",0)<cost || FuelLife.stock(t)<2)
            throw new IllegalArgumentException("Insufficient payroll, buyer funds or truck fuel.");
        t.employerFunds.merge(JOB,-WAGE-4,Integer::sum);
        t.employerFunds.merge(FuelLife.ACCOUNT,4,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-2,Integer::sum);
        t.employerFunds.merge("machinist",-cost,Integer::sum);
        t.industryStocks.merge(IndustryLife.QUARRY_IRON,-iron,Integer::sum);
        t.industryStocks.merge(IndustryLife.QUARRY_COPPER,-copper,Integer::sum);
        set(t,IRON,iron);set(t,COPPER,copper);set(t,WAGES,WAGE);set(t,PAYMENT,cost);set(t,STAGE,1);
        p.lastWorkDay=day;
        return "Loaded "+units+" ore units. Drive via Workshop to Factory, then unload.";
    }
    static int drive(TownLife.Town t,TownLife.Place destination) {
        requireDriver(t);
        var p=t.player;
        if(!active(t) || p.place!=truckPlace(t) || destination!=next(t) || destination==null)
            throw new IllegalArgumentException("Return to the assigned truck and follow its next delivery leg.");
        if(p.ownsBicycle && p.bicycleParked==null)throw new IllegalArgumentException("Park your bicycle first.");
        set(t,STAGE,stock(t,STAGE)+1);
        p.cycling=false;p.destination=destination;p.travelRemaining=3;
        return 3;
    }
    static String unload(TownLife.Town t,int day) {
        requireDriver(t);
        var p=t.player;
        if(stock(t,STAGE)!=3 || p.place!=FACTORY)throw new IllegalArgumentException("Deliver the truck to the factory first.");
        int iron=stock(t,IRON),copper=stock(t,COPPER),wage=stock(t,WAGES);
        t.industryStocks.merge(IndustryLife.FACTORY_IRON,iron,Math::addExact);
        t.industryStocks.merge(IndustryLife.FACTORY_COPPER_ORE,copper,Math::addExact);
        t.employerFunds.merge("miner",stock(t,PAYMENT),Math::addExact);
        p.money=Math.addExact(p.money,wage);p.lastWorkDay=day;
        p.fatigue=Math.min(100,p.fatigue+8);
        clear(t);
        return "Delivered "+(iron+copper)+" ore units. Earned "+wage+" EUR.";
    }
    static void clear(TownLife.Town t) {
        for(String key:new String[]{STAGE,IRON,COPPER,WAGES,PAYMENT})set(t,key,0);
    }
    static void recover(TownLife.Town t) {
        if(!active(t))return;
        t.industryStocks.merge(IndustryLife.QUARRY_IRON,stock(t,IRON),Math::addExact);
        t.industryStocks.merge(IndustryLife.QUARRY_COPPER,stock(t,COPPER),Math::addExact);
        t.employerFunds.merge(JOB,stock(t,WAGES),Math::addExact);
        t.employerFunds.merge("machinist",stock(t,PAYMENT),Math::addExact);
        clear(t);
    }
    record Report(boolean active,String truckPlace,String nextDestination,int iron,int copper,int wage,int quarryOre) {}
    static Report report(TownLife.Town t) {
        var place=truckPlace(t);var next=next(t);
        return new Report(active(t),place==null?null:place.name(),next==null?null:next.name(),
                stock(t,IRON),stock(t,COPPER),WAGE,IndustryLife.stock(t,IndustryLife.QUARRY_IRON)+IndustryLife.stock(t,IndustryLife.QUARRY_COPPER));
    }
}
