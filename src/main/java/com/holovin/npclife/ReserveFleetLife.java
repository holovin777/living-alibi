package com.holovin.npclife;

/** Municipal reserve vehicles use existing public maintenance funds and finite supplies. */
final class ReserveFleetLife {
    private ReserveFleetLife(){}
    static final String ACCOUNT="city-maintenance";
    static final int REPAIR_COST=15, REPAIR_POINTS=30;
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=480)return;
        for(String kind:new String[]{"bus","truck"}){
            String key="reserve:"+kind+":";
            // Only service a reserve that has actually been used and recovered.
            if(!t.industryStocks.containsKey(key+"fuel"))continue;
            if(kind.equals("bus")?VehicleTheftLife.bus(t):VehicleTheftLife.truck(t))continue;
            if(t.industryStocks.getOrDefault(key+"service-day",0)>=s.day)continue;
            t.industryStocks.put(key+"service-day",s.day);
            service(t,kind);
        }
    }
    static void service(TownLife.Town t,String kind){
        String key="reserve:"+kind+":";
        if(kind.equals("bus")?VehicleTheftLife.bus(t):VehicleTheftLife.truck(t))return;
        int condition=VehicleTheftLife.reserveCondition(t,kind);
        if(condition<70 && t.technicalMaterials.getOrDefault("spare-parts",0)>0
                && SupplyChainLife.pay(t,ACCOUNT,"technician",REPAIR_COST)){
            t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
            t.industryStocks.put(key+"condition",Math.min(100,condition+REPAIR_POINTS));
            t.industryStocks.merge(key+"repair-expense",REPAIR_COST,Math::addExact);
        }
        int fuel=VehicleTheftLife.reserveFuel(t,kind);
        int capacity=kind.equals("truck")?16:12;
        int units=Math.max(0,Math.min(capacity-fuel,Math.min(6,Math.min(FuelLife.stock(t),t.employerFunds.getOrDefault(ACCOUNT,0)/FuelLife.PRICE))));
        if(units>0&&SupplyChainLife.pay(t,ACCOUNT,FuelLife.ACCOUNT,units*FuelLife.PRICE)){
            t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);
            t.industryStocks.put(key+"fuel",fuel+units);
            t.industryStocks.merge(key+"fuel-expense",units*FuelLife.PRICE,Math::addExact);
        }
    }
}
