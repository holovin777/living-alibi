package com.holovin.npclife;

import java.util.List;
import java.util.stream.IntStream;

/** Two optional used service vans, purchased with retained service revenue.
 * Trips share the existing NPC travel clock and car presentation. */
final class ServiceFleetLife {
    private ServiceFleetLife(){}
    static final int LIMIT=2, PRICE=60, RESERVE=20, SERVICE=10, WEAR=2;
    static int get(TownLife.Town t,int i,String f){return ServiceCompanyLife.get(t,"van:"+i+":"+f);}
    static void set(TownLife.Town t,int i,String f,int n){ServiceCompanyLife.set(t,"van:"+i+":"+f,n);}
    static TownLife.Place parked(TownLife.Town t,int i){
        int p=get(t,i,"place");
        return p>0&&p<=TownLife.Place.values().length?TownLife.Place.values()[p-1]:null;
    }
    static int assigned(TownLife.Town t,TownLife.Citizen c){
        if(c.generationIndex<=0)return -1;
        for(int i=0;i<LIMIT;i++)if(get(t,i,"driver")==c.generationIndex)return i;
        return -1;
    }
    static boolean buy(TownLife.Town t,int i){
        if(parked(t,i)!=null||ServiceCompanyLife.cash(t)<PRICE+RESERVE)return false;
        if(!SupplyChainLife.pay(t,ServiceCompanyLife.ACCOUNT,"technician",PRICE))return false;
        set(t,i,"place",TownLife.Place.WORKSHOP.ordinal()+1);set(t,i,"condition",80);
        ServiceCompanyLife.count(t,"fleet-capital",PRICE);return true;
    }
    static boolean service(TownLife.Town t,int i){
        if(parked(t,i)!=TownLife.Place.WORKSHOP||get(t,i,"driver")!=0||get(t,i,"condition")>=50
                ||ServiceCompanyLife.cash(t)<SERVICE+RESERVE||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return false;
        if(!SupplyChainLife.pay(t,ServiceCompanyLife.ACCOUNT,"technician",SERVICE))return false;
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        set(t,i,"condition",Math.min(100,get(t,i,"condition")+40));
        ServiceCompanyLife.count(t,"fleet-service",SERVICE);return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        for(int i=0;i<LIMIT;i++){
            int index=get(t,i,"driver");
            if(index==0)continue;
            var c=NpcVehicleCrimeLife.person(t,index);
            if(c!=null&&c.travelRemaining==0&&get(t,i,"destination")>0){
                set(t,i,"place",c.place.ordinal()+1);set(t,i,"destination",0);
            }
            if(c==null||!HomeServiceLife.mechanic(t,c)
                    ||(c.travelRemaining==0&&parked(t,i)!=c.place)){
                set(t,i,"driver",0);set(t,i,"destination",0);set(t,i,"return-fuel",0);continue;
            }
            if(c.travelRemaining==0&&parked(t,i)==TownLife.Place.WORKSHOP&&get(t,i,"returning")==1){
                set(t,i,"driver",0);set(t,i,"returning",0);set(t,i,"return-fuel",0);
            }
        }
        if(!StaffingLife.enabled(t)||!NpcTransportLife.enabled(t)||s.minute!=480
                ||ServiceCompanyLife.get(t,"fleet-day")>=s.day)return;
        ServiceCompanyLife.set(t,"fleet-day",s.day);
        for(int i=0;i<LIMIT;i++)service(t,i);
        if(ServiceCompanyLife.get(t,"resident-completed")>=10)
            for(int i=0;i<LIMIT;i++)if(parked(t,i)==null){buy(t,i);break;}
    }
    static TownLife.Place returnDestination(TownLife.Town t,TownLife.Citizen c,WorldState s){
        int i=assigned(t,c);
        if(i<0||c.travelRemaining>0||EmploymentLife.sick(c,s.day)||NpcJusticeLife.confined(t,c,s))return null;
        if(ResidentServiceLife.destination(t,c,s)!=null)return null;
        if(parked(t,i)!=c.place||c.place==TownLife.Place.WORKSHOP)return null;
        return TownLife.Place.WORKSHOP;
    }
    static int units(TownLife.Place a,TownLife.Place b){return Math.max(1,(CityExpansionLife.distance(a,b)+3)/4);}
    /** Reserve actual round-trip fuel up front; return never depends on station availability. */
    static boolean depart(TownLife.Town t,TownLife.Citizen c,TownLife.Place desired,WorldState s){
        if(c.travelRemaining>0||c.place==desired||!HomeServiceLife.mechanic(t,c))return false;
        int i=assigned(t,c);
        if(i>=0){
            if(parked(t,i)!=c.place||desired!=TownLife.Place.WORKSHOP||get(t,i,"return-fuel")<=0)return false;
            int u=get(t,i,"return-fuel");
            set(t,i,"condition",Math.max(0,get(t,i,"condition")-u*WEAR));set(t,i,"return-fuel",0);
            set(t,i,"returning",1);set(t,i,"destination",desired.ordinal()+1);
            NpcTransportLife.set(t,c,"mode",NpcTransportLife.CAR);return true;
        }
        if(c.place!=TownLife.Place.WORKSHOP||ResidentServiceLife.destination(t,c,s)!=desired)return false;
        int u=units(c.place,desired),fuel=u*2,cost=fuel*FuelLife.PRICE;
        if(FuelLife.stock(t)<fuel||ServiceCompanyLife.cash(t)<cost+RESERVE)return false;
        for(i=0;i<LIMIT;i++){
            if(parked(t,i)!=c.place||get(t,i,"driver")!=0||get(t,i,"condition")<fuel*WEAR+20)continue;
            if(!SupplyChainLife.pay(t,ServiceCompanyLife.ACCOUNT,FuelLife.ACCOUNT,cost))return false;
            t.industryStocks.merge(FuelLife.STOCK,-fuel,Math::addExact);
            ServiceCompanyLife.count(t,"fleet-fuel",cost);
            set(t,i,"driver",c.generationIndex);set(t,i,"condition",get(t,i,"condition")-u*WEAR);
            set(t,i,"return-fuel",u);set(t,i,"returning",0);set(t,i,"destination",desired.ordinal()+1);
            NpcTransportLife.set(t,c,"mode",NpcTransportLife.CAR);return true;
        }
        return false;
    }
    record Van(int id,String parked,int condition,int driver,boolean travelling){}
    record View(List<Van> vans,int capital,int fuel,int service){}
    static View view(TownLife.Town t){
        return new View(IntStream.range(0,LIMIT).filter(i->parked(t,i)!=null)
            .mapToObj(i->new Van(i+1,parked(t,i).name(),get(t,i,"condition"),get(t,i,"driver"),get(t,i,"destination")>0)).toList(),
            ServiceCompanyLife.get(t,"fleet-capital"),ServiceCompanyLife.get(t,"fleet-fuel"),ServiceCompanyLife.get(t,"fleet-service"));
    }
}
