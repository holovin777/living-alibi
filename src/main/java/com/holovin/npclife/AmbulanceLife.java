package com.holovin.npclife;

/** Timed fictional emergency transport, paid from the existing municipal budget. */
final class AmbulanceLife {
    private AmbulanceLife(){}
    static final int CARE_COST=8;
    private static String key(TownLife.Town t,String field){return "ambulance:"+t.player.incarnation+":"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    private static void set(TownLife.Town t,String field,int n){t.industryStocks.put(key(t,field),n);}
    private static int clock(WorldState s){return Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1;}
    static int distance(TownLife.Place p){return Math.max(1,CityExpansionLife.distance(p,TownLife.Place.CLINIC));}
    static int fuelUnits(TownLife.Place p){return Math.min(6,Math.max(1,(distance(p)+3)/4));}
    static int responseMinutes(TownLife.Place p){return Math.min(45,5+distance(p));}
    static int tripMinutes(TownLife.Place p){return Math.min(45,5+distance(p));}
    static boolean ready(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0&&!t.player.cycling
            &&!IntercomLife.insideVisit(t,s.day,s.minute);
    }
    static boolean canCall(TownLife.Town t,WorldState s){
        var p=t.player.place;
        return ready(t,s)&&p!=TownLife.Place.CLINIC&&get(t,"origin")==0&&t.player.health<=80
            &&RoadAccidentLife.get(t,"injury")>RoadAccidentLife.get(t,"treated")
            &&FuelLife.stock(t)>=fuelUnits(p)
            &&t.employerFunds.getOrDefault("city-maintenance",0)>=CARE_COST+fuelUnits(p)*FuelLife.PRICE;
    }
    static String call(TownLife.Town t,WorldState s){
        if(!canCall(t,s))throw new IllegalArgumentException("Ambulance unavailable: check injury, location and municipal resources.");
        var p=t.player.place;
        int units=fuelUnits(p),fuelCost=units*FuelLife.PRICE;
        t.employerFunds.merge("city-maintenance",-fuelCost-CARE_COST,Integer::sum);
        t.employerFunds.merge(FuelLife.ACCOUNT,fuelCost,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);
        // Hold the care payment until the patient is actually transported.
        set(t,"origin",p.ordinal()+1);set(t,"due",Math.addExact(clock(s),responseMinutes(p)));
        return "Ambulance requested. Remain in this district; arrival in "+responseMinutes(p)+" game minutes.";
    }
    static boolean canBoard(TownLife.Town t,WorldState s){
        return ready(t,s)&&get(t,"origin")==t.player.place.ordinal()+1&&get(t,"due")>0&&clock(s)>=get(t,"due");
    }
    static void cancel(TownLife.Town t){
        if(get(t,"origin")==0)return;
        t.employerFunds.merge("city-maintenance",CARE_COST,Math::addExact);
        set(t,"origin",0);set(t,"due",0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,"origin")!=0&&(!t.player.alive||RebirthLife.active(t)||JusticeLife.confined(t)||t.player.place.ordinal()+1!=get(t,"origin")||clock(s)>get(t,"due")+30))
            cancel(t);
    }
    record Arrival(String message,int minutes){}
    static Arrival board(TownLife.Town t,WorldState s){
        if(!canBoard(t,s))throw new IllegalArgumentException("Wait for the ambulance in the pickup district.");
        int travel=tripMinutes(t.player.place);
        int gain=Math.max(0,Math.min(15,Math.min(100-t.player.health,RoadAccidentLife.get(t,"injury")-RoadAccidentLife.get(t,"treated"))));
        t.player.health+=gain;RoadAccidentLife.add(t,"treated",gain);
        t.employerFunds.merge("doctor",CARE_COST,Math::addExact);
        t.player.place=TownLife.Place.CLINIC;t.player.destination=t.player.place;t.player.travelRemaining=0;
        set(t,"origin",0);set(t,"due",0);set(t,"completed",Math.addExact(get(t,"completed"),1));
        return new Arrival("Ambulance delivered you to the clinic. Stabilization: health +"+gain+".",travel);
    }
    record View(String status,int minutesRemaining,int completed){}
    static View view(TownLife.Town t,WorldState s){
        boolean active=get(t,"origin")!=0;
        return new View(!active?"NONE":clock(s)>=get(t,"due")?"ARRIVED":"EN_ROUTE",active?Math.max(0,get(t,"due")-clock(s)):0,get(t,"completed"));
    }
}
