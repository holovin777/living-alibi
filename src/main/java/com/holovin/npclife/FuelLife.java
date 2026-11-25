package com.holovin.npclife;

/** Shared finite station/garage fuel stock and paid external wholesale deliveries. */
final class FuelLife {
    private FuelLife() { }
    static final String ACCOUNT="fuel-attendant", SUPPLIER="fuel-supplier";
    static final String STOCK="fuel:stock", DAY="fuel:delivery-day";
    static final int CAPACITY=48, PRICE=2;
    static void ensure(TownLife.Town t) {
        t.industryStocks.putIfAbsent(STOCK,CAPACITY);
        t.industryStocks.putIfAbsent(DAY,0);
    }
    static int stock(TownLife.Town t){return t.industryStocks.getOrDefault(STOCK,0);}
    static void tick(TownLife.Town t,int day,int minute) {
        if(minute!=420 || t.industryStocks.getOrDefault(DAY,0)>=day)return;
        t.industryStocks.put(DAY,day);
        int n=Math.min(12,Math.min(CAPACITY-stock(t),t.employerFunds.getOrDefault(ACCOUNT,0)));
        if(n<=0)return;
        t.employerFunds.merge(ACCOUNT,-n,Integer::sum);
        t.employerFunds.merge(SUPPLIER,n,Math::addExact);
        t.industryStocks.merge(STOCK,n,Integer::sum);
    }
    static String refuel(TownLife.Town t,int minute) {
        var p=t.player;
        MotorLife.present(p);
        boolean station=p.place==TownLife.Place.FUEL_STATION;
        if(!station && p.place!=TownLife.Place.WORKSHOP)
            throw new IllegalArgumentException("Visit the fuel station or workshop.");
        if(minute<(station?360:480) || minute>=(station?1320:1080))
            throw new IllegalArgumentException("Fuel counter is closed.");
        int units=Math.min(3,Math.min(MotorLife.CAPACITY-p.carFuel,stock(t)));
        if(units<=0)throw new IllegalArgumentException("Tank full or fuel sold out.");
        int cost=units*PRICE;
        if(p.money<cost)throw new IllegalArgumentException("Not enough money.");
        p.money-=cost;
        t.employerFunds.merge(ACCOUNT,cost,Math::addExact);
        t.industryStocks.merge(STOCK,-units,Integer::sum);
        p.carFuel+=units;
        return "Bought "+units+" fuel units for "+cost+" EUR.";
    }
    record Report(int stock,int capacity,int price,int payroll) { }
    static Report report(TownLife.Town t) {
        return new Report(stock(t),CAPACITY,PRICE,t.employerFunds.getOrDefault(ACCOUNT,0));
    }
}
