package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Persistent fleet legs, operating budget and limited boarding capacity. */
final class MunicipalBusFleetLife {
    private MunicipalBusFleetLife(){}
    static final String ACCOUNT="municipal-bus-fleet", DRIVER="Bus fleet driver", DISPATCHER="Bus dispatcher", MECHANIC="Bus mechanic";
    static final int COUNT=2, PURCHASE=60, CAPITAL=30, SEATS=12, DRIVER_PAY=4, DISPATCH_PAY=1;
    static final List<Integer> IDS=List.of(1,2);
    private static String key(int id,String f){return "bus-fleet:"+id+":"+f;}
    static int get(TownLife.Town t,int id,String f){return t.industryStocks.getOrDefault(key(id,f),0);}
    static void set(TownLife.Town t,int id,String f,int n){t.industryStocks.put(key(id,f),n);}
    static boolean role(String r){return Set.of(DRIVER,DISPATCHER,MECHANIC).contains(r);}
    static boolean owned(TownLife.Town t,int id){return get(t,id,"owned")>0;}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static int now(WorldState s){return (s.day-1)*1440+s.minute;}
    static TownLife.Place place(int n){return n>0&&n<=TownLife.Place.values().length?TownLife.Place.values()[n-1]:STAZIONE;}
    static TownLife.Place at(TownLife.Town t,int id){return place(get(t,id,"at"));}
    static TownLife.Place from(TownLife.Town t,int id){return place(get(t,id,"from"));}
    static TownLife.Place to(TownLife.Town t,int id){return place(get(t,id,"to"));}
    static boolean moving(TownLife.Town t,int id,WorldState s){return owned(t,id)&&get(t,id,"end")>now(s)&&get(t,id,"end")>get(t,id,"start");}
    static boolean buy(TownLife.Town t,int id){
        if(!IDS.contains(id)||owned(t,id)||MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+PURCHASE+CAPITAL)return false;
        SupplyChainLife.pay(t,MunicipalLife.CITY,"technician",PURCHASE);
        SupplyChainLife.pay(t,MunicipalLife.CITY,ACCOUNT,CAPITAL);
        set(t,id,"owned",1);set(t,id,"at",STAZIONE.ordinal()+1);set(t,id,"condition",100);
        return true;
    }
    static List<TownLife.Citizen> staff(TownLife.Town t,WorldState s,String role){
        return t.people.values().stream().filter(c->role.equals(c.occupation)&&WorkplaceHierarchy.onDuty(c,s.day)
            &&!RelationshipBoundaryLife.controlled(t,c)&&!NpcJusticeLife.confined(t,c,s)
            &&!TownLife.inPlayerConversation(t,c)&&c.fatigue<80)
            .sorted(Comparator.comparing(c->c.id)).toList();
    }
    static TownLife.Place destination(TownLife.Town t,int id){
        if(at(t,id)!=STAZIONE)return STAZIONE;
        if(id==1)return TransportDevelopmentLife.opened(t,NORTH_RESIDENCES)?NORTH_RESIDENCES:PARK;
        return TransportDevelopmentLife.opened(t,FACTORY)?FACTORY:TransportDevelopmentLife.opened(t,SOUTH_RESIDENCES)?SOUTH_RESIDENCES:WORKSHOP;
    }
    static boolean depart(TownLife.Town t,WorldState s,int id){
        if(!owned(t,id)||moving(t,id,s)||get(t,id,"last-slot")==now(s)+1||get(t,id,"condition")<30)return false;
        var drivers=staff(t,s,DRIVER);var dispatchers=staff(t,s,DISPATCHER);
        if(drivers.size()<id||dispatchers.isEmpty()||FuelLife.stock(t)<1||cash(t)<FuelLife.PRICE+DRIVER_PAY+DISPATCH_PAY)return false;
        var driver=drivers.get(id-1);var dispatcher=dispatchers.getFirst();
        var a=at(t,id);var z=destination(t,id);
        if(!BusLife.route(t,a,z))return false;
        int minutes=Math.max(4,CityExpansionLife.distance(a,z)*2)+TransportDevelopmentLife.delay(t,a,z);
        SupplyChainLife.pay(t,ACCOUNT,FuelLife.ACCOUNT,FuelLife.PRICE);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);
        t.employerFunds.merge(ACCOUNT,-DRIVER_PAY-DISPATCH_PAY,Integer::sum);
        driver.money=Math.addExact(driver.money,DRIVER_PAY);dispatcher.money=Math.addExact(dispatcher.money,DISPATCH_PAY);
        driver.fatigue=Math.min(100,driver.fatigue+2);
        set(t,id,"from",a.ordinal()+1);set(t,id,"to",z.ordinal()+1);
        set(t,id,"start",now(s));set(t,id,"end",now(s)+minutes);
        set(t,id,"last-slot",now(s)+1);set(t,id,"driver",driver.generationIndex);
        set(t,id,"condition",get(t,id,"condition")-3);set(t,id,"passengers",0);
        set(t,id,"trips",Math.addExact(get(t,id,"trips"),1));return true;
    }
    static boolean repair(TownLife.Town t,WorldState s,int id){
        if(!owned(t,id)||moving(t,id,s)||at(t,id)!=STAZIONE||get(t,id,"condition")>=65
            ||get(t,id,"repair-day")>=s.day||cash(t)<8||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return false;
        var mechanics=staff(t,s,MECHANIC);if(mechanics.isEmpty())return false;
        var m=mechanics.getFirst();
        SupplyChainLife.pay(t,ACCOUNT,"technician",4);t.employerFunds.merge(ACCOUNT,-4,Integer::sum);
        m.money=Math.addExact(m.money,4);t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
        set(t,id,"condition",Math.min(100,get(t,id,"condition")+40));set(t,id,"repair-day",s.day);return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        for(int id:IDS)if(owned(t,id)&&get(t,id,"end")>0&&get(t,id,"end")<=now(s)){
            set(t,id,"at",get(t,id,"to"));set(t,id,"passengers",0);
        }
        if(s.minute==660&&get(t,0,"review")<s.day){
            set(t,0,"review",s.day);
            if(IDS.stream().filter(id->owned(t,id)).count()<COUNT&&staff(t,s,DRIVER).size()>IDS.stream().filter(id->owned(t,id)).count())
                for(int id:IDS)if(buy(t,id))break;
            if(cash(t)<20&&MunicipalLife.balance(t,MunicipalLife.CITY)>=MunicipalLife.RESERVE+20)
                SupplyChainLife.pay(t,MunicipalLife.CITY,ACCOUNT,20);
        }
        if(s.minute==720)for(int id:IDS)if(repair(t,s,id))break;
        if(s.minute>=480&&s.minute<=1080&&s.minute%120==0)for(int id:IDS)depart(t,s,id);
    }
    static int boarding(TownLife.Town t,WorldState s,TownLife.Place a,TownLife.Place b){
        for(int id:IDS)if(moving(t,id,s)&&from(t,id)==a&&to(t,id)==b&&now(s)-get(t,id,"start")<=1
            &&get(t,id,"passengers")<SEATS)return id;
        return 0;
    }
    /** Caller debits the fare; this credits the actual service used. */
    static void fare(TownLife.Town t,int id){
        t.employerFunds.merge(id>0?ACCOUNT:MunicipalLife.CITY,BusLife.FARE,Math::addExact);
        if(id>0){set(t,id,"passengers",get(t,id,"passengers")+1);set(t,id,"tickets",Math.addExact(get(t,id,"tickets"),1));}
    }
    static int remaining(TownLife.Town t,WorldState s,int id){return Math.max(1,get(t,id,"end")-now(s));}
    record Bus(int id,String district,String from,String to,int condition,boolean moving,int passengers,int seats,int trips,int tickets,String driver,int departure,int arrival){}
    record View(int cash,List<Bus> buses){}
    static View view(TownLife.Town t,WorldState s){
        return new View(cash(t),IDS.stream().filter(id->owned(t,id)).map(id->{
            var driver=DemographyLife.byIndex(t,get(t,id,"driver"));
            return new Bus(id,at(t,id).name(),from(t,id).name(),to(t,id).name(),get(t,id,"condition"),moving(t,id,s),
                get(t,id,"passengers"),SEATS,get(t,id,"trips"),get(t,id,"tickets"),driver==null?"":driver.name,
                get(t,id,"start"),get(t,id,"end"));
        }).toList());
    }
}
