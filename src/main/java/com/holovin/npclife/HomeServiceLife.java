package com.holovin.npclife;

import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Abstract home heating, appliance wear, and a scheduled visit by an existing mechanic. */
final class HomeServiceLife {
    private HomeServiceLife(){}
    static final List<String> DEVICES=List.of("fridge","television","washer","boiler");
    static final int GAS_LIMIT=12, REPAIR_PRICE=15, WARRANTY_DAYS=60, PART_PRICE=5;
    static int get(TownLife.Town t,String k){return HomeGoodsLife.get(t,"service:"+k);}
    static void set(TownLife.Town t,String k,int v){HomeGoodsLife.set(t,"service:"+k,v);}
    static int condition(TownLife.Town t,String id){return Math.max(0,100-get(t,"wear:"+id));}
    static boolean working(TownLife.Town t,String id){return HomeGoodsLife.installed(t,id)&&condition(t,id)>0;}
    static void wear(TownLife.Town t,String id,int n){if(HomeGoodsLife.installed(t,id))set(t,"wear:"+id,Math.min(100,get(t,"wear:"+id)+n));}
    static int target(TownLife.Town t){return get(t,"target");}
    static boolean gas(TownLife.Town t){return t.player.ownsHouse&&get(t,"gas-bill")<GAS_LIMIT;}
    static boolean heating(TownLife.Town t){return working(t,"boiler")&&gas(t)&&target(t)>0;}
    static int indoor(TownLife.Town t,WorldState s){
        return get(t,"temp-set")==1?get(t,"temperature"):Math.max(5,Math.min(30,WeatherLife.at(s.day,s.minute).temperatureC()+4));
    }
    static String thermostat(TownLife.Town t,WorldState s,int target){
        HomeApplianceLife.home(t,s);
        if(!HomeGoodsLife.installed(t,"boiler")||!List.of(0,16,20,22).contains(target))
            throw new IllegalArgumentException("Choose an installed boiler and a supported thermostat setting.");
        set(t,"target",target);return "Thermostat updated.";
    }
    static String payGas(TownLife.Town t,WorldState s){
        HomeApplianceLife.home(t,s);int amount=Math.min(t.player.money,get(t,"gas-bill"));
        if(amount<1)throw new IllegalArgumentException("No gas bill to pay or insufficient cash.");
        t.player.money-=amount;t.employerFunds.merge("city-maintenance",amount,Math::addExact);
        set(t,"gas-bill",get(t,"gas-bill")-amount);return "Gas bill payment recorded.";
    }
    static boolean mechanic(TownLife.Town t,TownLife.Citizen c){
        return c.alive&&c.employed&&"Mechanic".equals(c.occupation)&&c.generationIndex>0&&c.age>=18
            &&!RebirthLife.linkedAdult(t,c)&&!BusinessLife.isEmployee(t,c);
    }
    static void purchased(TownLife.Town t,String id,int day){
        if(DEVICES.contains(id)){
            set(t,"purchase:"+id,day);set(t,"warranty-used:"+id,0);
        }
    }
    static boolean warranty(TownLife.Town t,String id,int day){
        int purchase=get(t,"purchase:"+id);
        return DEVICES.contains(id)&&HomeGoodsLife.installed(t,id)&&purchase>0&&day>=purchase
            &&day-purchase<WARRANTY_DAYS&&get(t,"warranty-used:"+id)==0&&condition(t,id)<=40;
    }
    static int quote(TownLife.Town t,String id,int day){return warranty(t,id,day)?0:REPAIR_PRICE;}
    static String merchant(String id){return HomeGoodsLife.product(id).shop()==WORKSHOP?"technician":"shop-assistant";}
    static boolean funded(TownLife.Town t,String id,boolean covered){
        return covered?t.employerFunds.getOrDefault(merchant(id),0)>=REPAIR_PRICE:t.player.money>=REPAIR_PRICE;
    }
    static boolean pending(TownLife.Town t){return get(t,"appointment-day")>0;}
    static String device(TownLife.Town t){int n=get(t,"device");return n>0&&n<=DEVICES.size()?DEVICES.get(n-1):"";}
    static boolean assigned(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return pending(t)&&get(t,"appointment-day")==s.day&&s.minute>=540&&s.minute<900
            &&get(t,"mechanic")==c.generationIndex&&mechanic(t,c)&&!EmploymentLife.sick(c,s.day)
            &&!NpcJusticeLife.confined(t,c,s);
    }
    static String request(TownLife.Town t,WorldState s,String id){
        HomeApplianceLife.home(t,s);
        if(!DEVICES.contains(id)||!HomeGoodsLife.installed(t,id)||condition(t,id)>=100)
            throw new IllegalArgumentException("Choose an installed appliance that needs service.");
        boolean covered=warranty(t,id,s.day);
        if(pending(t)||!funded(t,id,covered))throw new IllegalArgumentException("An appointment already exists or funds are insufficient.");
        var c=t.people.values().stream().filter(x->mechanic(t,x)).min(Comparator.comparingInt(x->x.generationIndex))
            .orElseThrow(()->new IllegalArgumentException("No local mechanic is available."));
        int day=s.day+1;while((day-1)%7>=5)day++;
        set(t,"device",DEVICES.indexOf(id)+1);set(t,"mechanic",c.generationIndex);
        set(t,"appointment-day",day);set(t,"minutes",0);set(t,"covered",covered?1:0);
        return covered?"Warranty service scheduled at no charge.":"Home service appointment scheduled.";
    }
    static void clear(TownLife.Town t){for(String k:List.of("appointment-day","mechanic","device","minutes","covered"))set(t,k,0);}
    static String cancel(TownLife.Town t,WorldState s){
        HomeApplianceLife.home(t,s);if(!pending(t))throw new IllegalArgumentException("No appointment to cancel.");
        ServiceCompanyLife.count(t,"cancelled",1);clear(t);return "Home service appointment cancelled.";
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!t.player.alive||RebirthLife.active(t)||!t.player.ownsHouse)return;
        if(s.minute%60==0){
            int outside=WeatherLife.at(s.day,s.minute).temperatureC();
            int baseline=Math.max(5,Math.min(32,outside+4));
            int current=indoor(t,s);
            int desired=heating(t)?Math.max(baseline,target(t)):baseline;
            int step=heating(t)?3:1;
            set(t,"temperature",current+Math.max(-step,Math.min(step,desired-current)));set(t,"temp-set",1);
        }
        if(s.minute==0&&get(t,"daily")<s.day){
            set(t,"daily",s.day);
            if(heating(t)){
                int tariff=target(t)>20?3:2;
                set(t,"gas-bill",Math.min(GAS_LIMIT,get(t,"gas-bill")+tariff));wear(t,"boiler",1);
            }
        }
        if(!pending(t))return;
        int day=get(t,"appointment-day");
        if(s.day>day||s.day==day&&s.minute>=900){ServiceCompanyLife.count(t,"missed",1);clear(t);set(t,"last-result",2);return;}
        if(s.day!=day)return;
        var c=NpcVehicleCrimeLife.person(t,get(t,"mechanic"));
        String id=device(t);boolean covered=get(t,"covered")==1;
        boolean attending=c!=null&&assigned(t,c,s)&&c.presentAt(SOUTH_RESIDENCES)
            &&t.player.place==SOUTH_RESIDENCES&&t.player.travelRemaining==0&&DEVICES.contains(id)
            &&HomeGoodsLife.installed(t,id)&&funded(t,id,covered)&&ServiceCompanyLife.available(t);
        if(!attending){set(t,"minutes",0);return;}
        int elapsed=get(t,"minutes")+1;set(t,"minutes",elapsed);
        if(elapsed<30)return;
        if(covered){
            t.employerFunds.merge(merchant(id),-REPAIR_PRICE,Math::addExact);
            set(t,"warranty-used:"+id,1);
        }else t.player.money-=REPAIR_PRICE;
        ServiceCompanyLife.complete(t,c);
        set(t,"wear:"+id,0);set(t,"last-result",1);clear(t);
    }
    record Device(String id,int condition,boolean canRequest,int repairPrice,boolean warrantyCovered){}
    record View(int indoorTemperature,int thermostat,boolean heating,boolean gasConnected,int gasBill,int repairPrice,
                int appointmentDay,String appointmentDevice,int lastResult,List<Device> devices,ServiceCompanyLife.View company){}
    static View view(TownLife.Town t,WorldState s){
        boolean atHome=t.player.ownsHouse&&t.player.place==SOUTH_RESIDENCES&&t.player.travelRemaining==0&&t.player.alive&&!RebirthLife.active(t);
        return new View(indoor(t,s),target(t),heating(t),gas(t),get(t,"gas-bill"),REPAIR_PRICE,get(t,"appointment-day"),device(t),
            get(t,"last-result"),DEVICES.stream().filter(id->HomeGoodsLife.installed(t,id))
                .map(id->new Device(id,condition(t,id),atHome&&!pending(t)&&condition(t,id)<100&&funded(t,id,warranty(t,id,s.day)),quote(t,id,s.day),warranty(t,id,s.day))).toList(),ServiceCompanyLife.view(t));
    }
}
