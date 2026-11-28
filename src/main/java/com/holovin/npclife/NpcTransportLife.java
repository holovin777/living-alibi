package com.holovin.npclife;

import java.util.List;

/** Persisted resident transport; logical trips retain the original travel clock. */
final class NpcTransportLife {
    private NpcTransportLife(){}
    static final String ENABLED="npc-transport:enabled";
    static final int WALK=0,CAR=1,BUS=2;
    static boolean enabled(TownLife.Town t){return t.industryStocks.getOrDefault(ENABLED,0)==1;}
    static String key(TownLife.Citizen c,String field){return "npc-transport:"+c.generationIndex+":"+field;}
    static int get(TownLife.Town t,TownLife.Citizen c,String field){return t.industryStocks.getOrDefault(key(c,field),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String field,int value){t.industryStocks.put(key(c,field),Math.max(0,value));}
    static boolean eligible(TownLife.Citizen c){return c.alive&&c.age>=18&&c.generationIndex>0&&!PoliceLife.staff(c);}
    static boolean owns(TownLife.Town t,TownLife.Citizen c){
        return eligible(c)&&(get(t,c,"owned")==1||get(t,c,"initialized")==0&&VehicleTheftLife.owner(c));
    }
    static TownLife.Place parked(TownLife.Town t,TownLife.Citizen c){
        int code=get(t,c,"parked");
        return code>0&&code<=TownLife.Place.values().length?TownLife.Place.values()[code-1]:get(t,c,"initialized")==0?c.place:null;
    }
    static int mode(TownLife.Town t,TownLife.Citizen c){return c.travelRemaining>0?get(t,c,"mode"):WALK;}
    static void prepare(TownLife.Town t,TownLife.Citizen c,WorldState s,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!enabled(t)||!eligible(c)||c.travelRemaining>0)return;
        set(t,c,"mode",WALK);
        if(get(t,c,"initialized")==0){
            set(t,c,"initialized",1);
            if(VehicleTheftLife.owner(c)){
                set(t,c,"owned",1);set(t,c,"fuel",6);set(t,c,"condition",80);set(t,c,"parked",c.place.ordinal()+1);
            }
        }
        if(!owns(t,c)&&c.place==TownLife.Place.WORKSHOP&&s.minute>=480&&s.minute<1080&&c.money>=MotorLife.PRICE+60){
            c.money-=MotorLife.PRICE;t.employerFunds.merge("technician",MotorLife.PRICE,Math::addExact);
            set(t,c,"owned",1);set(t,c,"fuel",6);set(t,c,"condition",80);set(t,c,"parked",c.place.ordinal()+1);
            TownLife.publish(t,s,c.id,c.place,c.name+" bought a used car.",events,observations);
        }
        if(!owns(t,c)||VehicleTheftLife.missing(t,c)||parked(t,c)!=c.place)return;
        // Simplified local service delivery, with real charges and finite stock.
        if(s.minute>=420&&s.minute<1200&&get(t,c,"fuel")<=2&&c.money>=36){
            int units=Math.min(3,Math.min(FuelLife.stock(t),Math.min(MotorLife.CAPACITY-get(t,c,"fuel"),(c.money-30)/FuelLife.PRICE)));
            if(units>0){
                int cost=units*FuelLife.PRICE;c.money-=cost;t.employerFunds.merge(FuelLife.ACCOUNT,cost,Math::addExact);
                t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);set(t,c,"fuel",get(t,c,"fuel")+units);
            }
        }
        if(s.minute>=480&&s.minute<1080&&get(t,c,"condition")<45&&c.money>=45){
            c.money-=15;t.employerFunds.merge("technician",15,Math::addExact);
            set(t,c,"condition",Math.min(100,get(t,c,"condition")+30));
        }
    }
    static void depart(TownLife.Town t,TownLife.Citizen c,TownLife.Place desired,WorldState s){
        if(!enabled(t)||!eligible(c)||c.travelRemaining>0||c.place==desired)return;
        set(t,c,"mode",WALK);set(t,c,"fleet-bus",0);
        if(ServiceFleetLife.depart(t,c,desired,s))return;
        int distance=CityExpansionLife.distance(c.place,desired);
        int units=Math.max(1,(distance+3)/4);
        if(distance>=2&&NpcVehicleCrimeLife.borrowTrip(t,c,desired,units))return;
        if(owns(t,c)&&!VehicleTheftLife.missing(t,c)&&parked(t,c)==c.place&&distance>=2&&
                get(t,c,"fuel")>=units&&get(t,c,"condition")>=units*3){
            set(t,c,"mode",CAR);set(t,c,"fuel",get(t,c,"fuel")-units);
            set(t,c,"condition",get(t,c,"condition")-units*3);set(t,c,"parked",desired.ordinal()+1);
        }else if(BusLife.route(t,c.place,desired)&&BusLife.open(s.minute)&&c.money>=BusLife.FARE+10){
            int id=MunicipalBusFleetLife.boarding(t,s,c.place,desired);
            int minutes=BusLife.duration(t,s,c.place,desired);
            c.money-=BusLife.FARE;MunicipalBusFleetLife.fare(t,id);set(t,c,"mode",BUS);set(t,c,"fleet-bus",id);
            set(t,c,"bus-departure",Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1);
            set(t,c,"trip-duration",minutes);
        }
    }
    static void returned(TownLife.Town t,int index,int fuel,int condition){
        if(index<=0)return;
        for(var c:t.people.values())if(c.generationIndex==index){
            if(get(t,c,"initialized")==0){
                set(t,c,"initialized",1);set(t,c,"owned",1);
                int origin=VehicleTheftLife.get(t,"origin");
                set(t,c,"parked",origin>0?origin:c.place.ordinal()+1);
            }
            set(t,c,"fuel",fuel);set(t,c,"condition",condition);
            return;
        }
    }
}
