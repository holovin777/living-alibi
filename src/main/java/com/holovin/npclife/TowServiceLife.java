package com.holovin.npclife;

import java.util.List;

/** Paid asynchronous-in-world recovery of an owned disabled vehicle. */
final class TowServiceLife {
    private TowServiceLife(){}
    private static String key(TownLife.Town t,String field){return "tow-service:"+t.player.incarnation+":"+field;}
    private static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    private static void set(TownLife.Town t,String field,int n){t.industryStocks.put(key(t,field),n);}
    private static int time(WorldState s){return Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1;}
    static String kind(TownLife.Town t){
        int k=get(t,"kind");return k>0&&k<=VehicleDocumentsLife.KINDS.size()?VehicleDocumentsLife.KINDS.get(k-1):"";
    }
    static boolean pending(TownLife.Town t,String kind){return !kind.isEmpty()&&kind.equals(kind(t));}
    static void requireAvailable(TownLife.Town t,String kind){
        if(pending(t,kind))throw new IllegalArgumentException("Vehicle is awaiting recovery.");
    }
    private static int fuel(TownLife.Town t,String kind){return kind.equals("car")?t.player.carFuel:LightMotorLife.get(t,kind,"fuel");}
    static int distance(TownLife.Place origin){return Math.max(1,CityExpansionLife.distance(origin,TownLife.Place.WORKSHOP));}
    static int units(TownLife.Place origin){return Math.min(6,Math.max(1,(distance(origin)+3)/4));}
    static int duration(TownLife.Place origin){return Math.min(90,15+distance(origin)*2);}
    static int fee(TownLife.Place origin){return 12+2*units(origin);}
    static boolean canRequest(TownLife.Town t,WorldState s,String kind){
        var origin=VehicleDocumentsLife.parked(t,kind);
        return origin!=null&&origin!=TownLife.Place.WORKSHOP&&origin==t.player.place&&kind(t).isEmpty()
                &&t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0&&!t.player.cycling
                &&!IntercomLife.insideVisit(t,s.day,s.minute)&&s.minute>=360&&s.minute<1320
                &&(fuel(t,kind)<1||VehicleDocumentsLife.condition(t,kind)<3)
                &&t.player.money>=fee(origin)&&FuelLife.stock(t)>=units(origin);
    }
    static String request(TownLife.Town t,WorldState s,String kind){
        if(!canRequest(t,s,kind))throw new IllegalArgumentException("Recovery unavailable: check vehicle location, condition, hours, funds and fuel stock.");
        var origin=VehicleDocumentsLife.parked(t,kind);
        int cost=fee(origin),fuel=units(origin);
        t.player.money-=cost;
        t.employerFunds.merge(FuelLife.ACCOUNT,fuel*FuelLife.PRICE,Math::addExact);
        t.employerFunds.merge("technician",cost-fuel*FuelLife.PRICE,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-fuel,Integer::sum);
        set(t,"kind",VehicleDocumentsLife.KINDS.indexOf(kind)+1);
        set(t,"origin",origin.ordinal()+1);set(t,"due",Math.addExact(time(s),duration(origin)));
        return "Recovery ordered for EUR "+cost+". Workshop delivery in "+duration(origin)+" game minutes.";
    }
    static void tick(TownLife.Town t,WorldState s){
        String kind=kind(t);
        if(kind.isEmpty()||time(s)<get(t,"due"))return;
        var parked=VehicleDocumentsLife.parked(t,kind);
        if(parked!=null&&parked.ordinal()+1==get(t,"origin")){
            if(kind.equals("car"))t.player.carPlace=TownLife.Place.WORKSHOP;
            else LightMotorLife.park(t,kind,TownLife.Place.WORKSHOP);
            set(t,"completed",Math.addExact(get(t,"completed"),1));
        }
        set(t,"kind",0);set(t,"origin",0);set(t,"due",0);
    }
    record View(String kind,int minutesRemaining,int completed){}
    static View view(TownLife.Town t,WorldState s){return new View(kind(t),kind(t).isEmpty()?0:Math.max(0,get(t,"due")-time(s)),get(t,"completed"));}
}
