package com.holovin.npclife;

import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** Persisted player freight manifest; legacy kind 0 remains the ore route. */
final class FreightLife {
    private FreightLife() {}
    static final String JOB="freight-driver", STAGE="freight:stage", KIND="freight:kind",
            IRON="freight:iron", COPPER="freight:copper",
            WAGES="freight:wage-escrow", PAYMENT="freight:ore-escrow";
    static final int WAGE=32, CONTRACT_FEE=40;
    static final String COMPANY_RUN="freight:company-run";
    record Route(int kind,String name,List<TownLife.Place> places,String source,String buyer,
                 String first,String second,String firstLabel,String secondLabel,int firstPrice,int secondPrice) {}
    static final List<Route> ROUTES=List.of(
            new Route(0,"Ore",List.of(QUARRY,WORKSHOP,FACTORY),"miner","machinist",
                    IndustryLife.QUARRY_IRON,IndustryLife.QUARRY_COPPER,"Iron ore","Copper ore",3,3),
            new Route(1,"Industrial materials",List.of(FACTORY,WORKSHOP),"machinist","city-maintenance",
                    IndustryLife.FACTORY_PARTS,IndustryLife.FACTORY_STEEL,"Spare parts","Steel",5,4),
            new Route(2,"Farm produce",List.of(FARM,BAKERY,CAFE,SOUTH_RESIDENCES,MARKET),FarmLife.ACCOUNT,"shop-assistant",
                    FarmLife.barn("vegetables"),FarmLife.barn("rice"),"Vegetables","Rice",2,1));
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{STAGE,KIND,IRON,COPPER,WAGES,PAYMENT,COMPANY_RUN})t.industryStocks.putIfAbsent(key,0);
        LogisticsLife.ensure(t);
    }
    static int stock(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int amount){t.industryStocks.put(key,amount);}
    static boolean active(TownLife.Town t){return stock(t,STAGE)>0;}
    static Route route(TownLife.Town t){return ROUTES.get(stock(t,KIND));}
    static TownLife.Place truckPlace(TownLife.Town t) {
        return active(t)?route(t).places.get(stock(t,STAGE)-1):null;
    }
    static TownLife.Place next(TownLife.Town t) {
        return active(t)&&stock(t,STAGE)<route(t).places.size()?route(t).places.get(stock(t,STAGE)):null;
    }
    static int reservedCrop(TownLife.Town t,String crop) {
        if(!active(t)||stock(t,KIND)!=2)return 0;
        return switch(crop){case "vegetables"->stock(t,IRON);case "rice"->stock(t,COPPER);default->0;};
    }
    static void requireDriver(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(active(t)&&stock(t,COMPANY_RUN)>0) {
            if(!LogisticsLife.yours(t))throw new IllegalArgumentException("Only the company owner can complete this contract.");
        } else if(!JOB.equals(t.player.jobId))throw new IllegalArgumentException("Apply for the freight driver job first.");
    }
    static String load(TownLife.Town t,int day,int minute) {return load(t,day,minute,false);}
    static String load(TownLife.Town t,int day,int minute,boolean company) {
        if(company)LogisticsLife.requireOwner(t);else requireDriver(t);
        var p=t.player;
        Route r=ROUTES.stream().filter(x->x.places.getFirst()==p.place).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Load at Quarry, Factory or Farm."));
        if(minute<480 || minute>=960)throw new IllegalArgumentException("Loading hours 08:00–16:00.");
        if(active(t) || p.lastWorkDay==day)throw new IllegalArgumentException("Finish your existing job or return tomorrow.");
        if(p.ownsBicycle && p.bicycleParked==null)throw new IllegalArgumentException("Park your bicycle first.");
        int first=Math.min(2,stock(t,r.first)),second=Math.min(1,stock(t,r.second));
        if(r.kind==2) {
            first=Math.min(first,RetailLife.product("vegetables").capacity()-t.retailStock.getOrDefault("vegetables",0));
            second=Math.min(second,RetailLife.product("rice").capacity()-t.retailStock.getOrDefault("rice",0));
        }
        first=Math.max(0,first);second=Math.max(0,second);
        int units=first+second,cost=first*r.firstPrice+second*r.secondPrice;
        int relocation=company && LogisticsLife.truckPlace(t)!=p.place?LogisticsLife.RELOCATION:0;
        int fuel=r.places.size()-1+(relocation>0?1:0), fuelCost=fuel*FuelLife.PRICE;
        if(units==0)throw new IllegalArgumentException("No cargo or free destination storage.");
        int fee=company?CONTRACT_FEE:WAGE;
        if(FuelLife.stock(t)<fuel || t.employerFunds.getOrDefault(r.buyer,0)<cost+(company?fee:0))
            throw new IllegalArgumentException("Insufficient buyer funds or truck fuel.");
        if(company) {
            if(!LogisticsLife.hasTruck(t))throw new IllegalArgumentException("Buy a company truck first.");
            LogisticsLife.spend(t,fuelCost+relocation);
            if(relocation>0)t.employerFunds.merge(JOB,relocation,Math::addExact);
            LogisticsLife.park(t,p.place);
        } else {
            if(t.employerFunds.getOrDefault(JOB,0)<fee+fuelCost)
                throw new IllegalArgumentException("Insufficient employer payroll.");
            t.employerFunds.merge(JOB,-fee-fuelCost,Integer::sum);
        }
        t.employerFunds.merge(FuelLife.ACCOUNT,fuelCost,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-fuel,Integer::sum);
        t.employerFunds.merge(r.buyer,-cost-(company?fee:0),Integer::sum);
        t.industryStocks.merge(r.first,-first,Integer::sum);
        t.industryStocks.merge(r.second,-second,Integer::sum);
        set(t,KIND,r.kind);set(t,IRON,first);set(t,COPPER,second);set(t,WAGES,fee);set(t,PAYMENT,cost);set(t,STAGE,1);set(t,COMPANY_RUN,company?1:0);
        p.lastWorkDay=day;
        return "Loaded "+units+" units of "+r.name+". Follow the assigned route, then unload.";
    }
    static int drive(TownLife.Town t,TownLife.Place destination) {
        requireDriver(t);
        var p=t.player;
        if(!active(t) || p.place!=truckPlace(t) || destination!=next(t) || destination==null)
            throw new IllegalArgumentException("Return to the assigned truck and follow its next delivery leg.");
        if(p.ownsBicycle && p.bicycleParked==null)throw new IllegalArgumentException("Park your bicycle first.");
        set(t,STAGE,stock(t,STAGE)+1);
        if(stock(t,COMPANY_RUN)>0)LogisticsLife.park(t,destination);
        p.cycling=false;p.destination=destination;p.travelRemaining=3;
        return 3;
    }
    static String unload(TownLife.Town t,int day) {
        requireDriver(t);
        var p=t.player;
        if(!active(t)||next(t)!=null||p.place!=truckPlace(t))throw new IllegalArgumentException("Deliver the truck to its destination first.");
        Route r=route(t);
        int first=stock(t,IRON),second=stock(t,COPPER),wage=stock(t,WAGES);
        switch(r.kind) {
            case 0 -> {
                t.industryStocks.merge(IndustryLife.FACTORY_IRON,first,Math::addExact);
                t.industryStocks.merge(IndustryLife.FACTORY_COPPER_ORE,second,Math::addExact);
            }
            case 1 -> {
                t.technicalMaterials.merge("spare-parts",first,Math::addExact);
                t.technicalMaterials.merge("steel",second,Math::addExact);
            }
            case 2 -> {
                t.retailStock.merge("vegetables",first,Math::addExact);
                t.retailStock.merge("rice",second,Math::addExact);
            }
            default -> throw new IllegalStateException("Unknown freight route.");
        }
        t.employerFunds.merge(r.source,stock(t,PAYMENT),Math::addExact);
        boolean company=stock(t,COMPANY_RUN)>0;
        if(company)LogisticsLife.receive(t,wage);else p.money=Math.addExact(p.money,wage);
        p.lastWorkDay=day;
        p.fatigue=Math.min(100,p.fatigue+8);
        clear(t);
        return "Delivered "+(first+second)+" units of "+r.name+". "+(company?"Company received ":"Earned ")+wage+" EUR.";
    }
    static void clear(TownLife.Town t) {
        for(String key:new String[]{STAGE,KIND,IRON,COPPER,WAGES,PAYMENT,COMPANY_RUN})set(t,key,0);
    }
    static void recover(TownLife.Town t) {
        if(!active(t))return;
        Route r=route(t);
        t.industryStocks.merge(r.first,stock(t,IRON),Math::addExact);
        t.industryStocks.merge(r.second,stock(t,COPPER),Math::addExact);
        t.employerFunds.merge(stock(t,COMPANY_RUN)>0?r.buyer:JOB,stock(t,WAGES),Math::addExact);
        t.employerFunds.merge(r.buyer,stock(t,PAYMENT),Math::addExact);
        clear(t);
    }
    record Report(boolean active,String truckPlace,String nextDestination,int iron,int copper,int wage,int quarryOre,
                  String cargoLabel,int firstUnits,int secondUnits,String firstLabel,String secondLabel,String destination,boolean companyRun) {}
    static Report report(TownLife.Town t) {
        var place=truckPlace(t);var next=next(t);Route r=route(t);
        int first=stock(t,IRON),second=stock(t,COPPER);
        return new Report(active(t),place==null?null:place.name(),next==null?null:next.name(),
                r.kind==0?first:0,r.kind==0?second:0,stock(t,COMPANY_RUN)>0?CONTRACT_FEE:WAGE,IndustryLife.stock(t,IndustryLife.QUARRY_IRON)+IndustryLife.stock(t,IndustryLife.QUARRY_COPPER),
                r.name,first,second,r.firstLabel,r.secondLabel,active(t)?r.places.getLast().name():null,stock(t,COMPANY_RUN)>0);
    }
}
