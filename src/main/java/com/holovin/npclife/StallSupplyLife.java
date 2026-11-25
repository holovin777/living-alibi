package com.holovin.npclife;

import java.util.List;

/** Standing farm order, paid in advance, delivered by a contracted carrier. */
final class StallSupplyLife {
    private StallSupplyLife() {}
    static final String ENABLED="stall:supply-enabled", RESERVE="stall:supply-reserve", CARGO="stall:supply-cargo",
            ESCROW="stall:supply-escrow", DISPATCH_DAY="stall:supply-dispatch-day", DELIVERED="stall:supply-delivered",
            SPENT="stall:supply-spent";
    static final String CARRIER="stall-supply-carrier";
    static final int UNITS=4, UNIT_PRICE=2, SERVICE_FEE=1, COST=UNITS*UNIT_PRICE+FuelLife.PRICE+SERVICE_FEE;
    static final List<String> KEYS=List.of(ENABLED,RESERVE,CARGO,ESCROW,DISPATCH_DAY,DELIVERED,SPENT);
    static int get(TownLife.Town t,String key){return MarketStallLife.get(t,key);}
    static void set(TownLife.Town t,String key,int value){MarketStallLife.set(t,key,value);}
    static void ensure(TownLife.Town t){for(String k:KEYS)t.industryStocks.putIfAbsent(k,0);}
    static int reserved(TownLife.Town t){return get(t,CARGO);}
    static void clear(TownLife.Town t){
        // Recover uncompleted cargo and unpaid farm escrow when the owner's life ends.
        t.industryStocks.merge(FarmLife.barn("vegetables"),reserved(t),Math::addExact);
        t.employerFunds.merge("city-maintenance",Math.addExact(get(t,RESERVE),get(t,ESCROW)),Math::addExact);
        for(String k:KEYS)set(t,k,0);
    }
    static String act(TownLife.Town t,String action){
        var p=t.player;
        return switch(action){
            case "stall-supply-enable" -> {
                set(t,ENABLED,1);yield "Signed a standing farm supply order.";
            }
            case "stall-supply-disable" -> {
                set(t,ENABLED,0);yield "Paused future orders; dispatched cargo will still arrive.";
            }
            case "stall-supply-fund" -> {
                if(p.money<10)throw new IllegalArgumentException("Need 10 EUR to fund supply.");
                int n=Math.addExact(get(t,RESERVE),10);
                p.money-=10;set(t,RESERVE,n);yield "Added 10 EUR to supply reserve.";
            }
            case "stall-supply-withdraw" -> {
                if(get(t,RESERVE)<10)throw new IllegalArgumentException("Need 10 EUR of uncommitted supply funds.");
                int n=Math.addExact(p.money,10);
                set(t,RESERVE,get(t,RESERVE)-10);p.money=n;yield "Withdrew 10 EUR from supply reserve.";
            }
            default -> throw new IllegalArgumentException("Unknown supply action.");
        };
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(MarketStallLife.get(t,MarketStallLife.LICENSE)==0||!t.player.alive)return;
        if(minute==600&&reserved(t)>0&&get(t,DISPATCH_DAY)<=day){
            int cargo=reserved(t);
            if(MarketStallLife.total(t)+cargo>MarketStallLife.CAPACITY)return;
            int farmBalance=Math.addExact(t.employerFunds.getOrDefault(FarmLife.ACCOUNT,0),get(t,ESCROW));
            int delivered=Math.addExact(get(t,DELIVERED),cargo);
            t.employerFunds.put(FarmLife.ACCOUNT,farmBalance);
            t.industryStocks.merge(MarketStallLife.stock("vegetables"),cargo,Math::addExact);
            set(t,DELIVERED,delivered);set(t,CARGO,0);set(t,ESCROW,0);
        }
        if(minute!=540||get(t,ENABLED)==0||reserved(t)>0||get(t,DISPATCH_DAY)>=day)return;
        if(get(t,RESERVE)<COST||MarketStallLife.total(t)+UNITS>MarketStallLife.CAPACITY||
                FarmLife.stock(t,FarmLife.barn("vegetables"))<UNITS||FuelLife.stock(t)<1)return;
        int fuelBalance=Math.addExact(t.employerFunds.getOrDefault(FuelLife.ACCOUNT,0),FuelLife.PRICE);
        int carrierBalance=Math.addExact(t.employerFunds.getOrDefault(CARRIER,0),SERVICE_FEE);
        int spent=Math.addExact(get(t,SPENT),COST);
        set(t,RESERVE,get(t,RESERVE)-COST);
        FarmLife.set(t,FarmLife.barn("vegetables"),FarmLife.stock(t,FarmLife.barn("vegetables"))-UNITS);
        t.industryStocks.merge(FuelLife.STOCK,-1,Integer::sum);
        t.employerFunds.put(FuelLife.ACCOUNT,fuelBalance);t.employerFunds.put(CARRIER,carrierBalance);
        set(t,CARGO,UNITS);set(t,ESCROW,UNITS*UNIT_PRICE);set(t,DISPATCH_DAY,day);set(t,SPENT,spent);
    }
    record Report(boolean enabled,int reserve,int cargo,int delivered,int spent,int batchCost,int batchUnits,int farmAvailable) {}
    static Report report(TownLife.Town t){
        return new Report(get(t,ENABLED)>0,get(t,RESERVE),reserved(t),get(t,DELIVERED),get(t,SPENT),COST,UNITS,FarmLife.stock(t,FarmLife.barn("vegetables")));
    }
}
