package com.holovin.npclife;

/** Personal two-wheel vehicles; persisted separately for each player incarnation. */
final class LightMotorLife {
    private LightMotorLife(){}
    static final String[] KINDS={"scooter","motorcycle"};
    static void validate(String kind){
        if(!kind.equals("scooter")&&!kind.equals("motorcycle"))throw new IllegalArgumentException("Unknown two-wheeler.");
    }
    static String key(TownLife.Town t,String kind,String field){validate(kind);return "personal-motor:"+t.player.incarnation+":"+kind+":"+field;}
    static int get(TownLife.Town t,String kind,String field){return t.industryStocks.getOrDefault(key(t,kind,field),0);}
    static void set(TownLife.Town t,String kind,String field,int n){t.industryStocks.put(key(t,kind,field),Math.max(0,n));}
    static int price(String kind){validate(kind);return kind.equals("scooter")?75:140;}
    static int capacity(String kind){validate(kind);return kind.equals("scooter")?6:9;}
    static TownLife.Place parked(TownLife.Town t,String kind){
        int p=get(t,kind,"parked");
        return p>0&&p<=TownLife.Place.values().length?TownLife.Place.values()[p-1]:null;
    }
    static void park(TownLife.Town t,String kind,TownLife.Place p){set(t,kind,"parked",p==null?0:p.ordinal()+1);}
    static void present(TownLife.Town t,String kind){
        TransportLife.ready(t.player);JusticeLife.requireFree(t);
        if(parked(t,kind)!=t.player.place)throw new IllegalArgumentException("Return to your own parked vehicle.");
    }
    static String action(TownLife.Town t,String kind,String action,int minute){
        validate(kind);TransportLife.ready(t.player);JusticeLife.requireFree(t);
        if(RebirthLife.active(t))throw new IllegalArgumentException("This vehicle shop is not available in linked life mode yet.");
        switch(action){
            case "buy" -> {
                MotorLife.workshop(t.player,minute);
                if(parked(t,kind)!=null)throw new IllegalArgumentException("You already own this vehicle type.");
                MotorLife.pay(t,price(kind));VehicleDocumentsLife.clear(t,kind);park(t,kind,t.player.place);
                set(t,kind,"fuel",capacity(kind)/2);set(t,kind,"condition",80);set(t,kind,"remainder",0);
                return "Bought a used "+kind+" for "+price(kind)+" EUR.";
            }
            case "sell" -> {
                MotorLife.workshop(t.player,minute);present(t,kind);
                int value=Math.max(1,price(kind)*get(t,kind,"condition")/200);
                if(t.employerFunds.getOrDefault("technician",0)<value)throw new IllegalArgumentException("Dealer has insufficient funds.");
                t.employerFunds.merge("technician",-value,Integer::sum);t.player.money=Math.addExact(t.player.money,value);
                park(t,kind,null);VehicleDocumentsLife.clear(t,kind);set(t,kind,"fuel",0);set(t,kind,"condition",0);set(t,kind,"remainder",0);
                return "Sold "+kind+" for "+value+" EUR.";
            }
            case "refuel" -> {
                present(t,kind);
                boolean station=t.player.place==TownLife.Place.FUEL_STATION||CityExpansionLife.serviceHub(t.player.place);
                if(!station&&t.player.place!=TownLife.Place.WORKSHOP)throw new IllegalArgumentException("Visit a fuel station or workshop.");
                if(minute<(station?360:480)||minute>=(station?1320:1080))throw new IllegalArgumentException("Fuel counter closed.");
                int units=Math.min(3,Math.min(capacity(kind)-get(t,kind,"fuel"),FuelLife.stock(t)));
                int cost=units*FuelLife.PRICE;
                if(units<=0||t.player.money<cost)throw new IllegalArgumentException("Tank full, stock empty, or insufficient money.");
                t.player.money-=cost;t.employerFunds.merge(FuelLife.ACCOUNT,cost,Math::addExact);t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);
                set(t,kind,"fuel",get(t,kind,"fuel")+units);return "Refueled "+kind+" for "+cost+" EUR.";
            }
            case "repair" -> {
                MotorLife.workshop(t.player,minute);present(t,kind);
                if(get(t,kind,"condition")>=100||t.technicalMaterials.getOrDefault("spare-parts",0)<1)throw new IllegalArgumentException("No repair needed or no spare parts.");
                MotorLife.pay(t,10);t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
                set(t,kind,"condition",Math.min(100,get(t,kind,"condition")+30));return "Serviced "+kind+" for 10 EUR.";
            }
            default -> throw new IllegalArgumentException("Unknown vehicle action.");
        }
    }
}
