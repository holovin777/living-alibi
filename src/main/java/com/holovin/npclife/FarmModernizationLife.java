package com.holovin.npclife;

import java.util.List;

/** Farm equipment, irrigation and herd health. All quantities are fictional
 * gameplay units. The cooperative owns and pays for its assets. */
final class FarmModernizationLife {
    private FarmModernizationLife(){}
    static final String MECHANIC="Farm machinery technician", IRRIGATION="Irrigation operator", VET="Veterinarian";
    static final int WATER_CAP=12, PART_COST=5, VET_COST=4;
    static int get(TownLife.Town t,String f){return AgriFoodLife.get(t,"modern:"+f);}
    static void set(TownLife.Town t,String f,int n){AgriFoodLife.set(t,"modern:"+f,n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean active(TownLife.Town t){return get(t,"enabled")>0;}
    static boolean role(String job){return List.of(MECHANIC,IRRIGATION,VET).contains(job);}
    static void ensure(TownLife.Town t){
        if(active(t)||!AgriFoodLife.active(t))return;
        set(t,"enabled",1);set(t,"tractor",100);set(t,"harvester",100);
        set(t,"pump",100);set(t,"water",6);set(t,"herd-health",100);
    }
    static int water(TownLife.Town t){return get(t,"water");}
    static void dawn(TownLife.Town t,WorldState s){
        if(!active(t)||get(t,"dawn-day")>=s.day)return;
        set(t,"dawn-day",s.day);
        var weather=WeatherLife.at(s.day,s.minute);
        set(t,"water",Math.min(WATER_CAP,Math.max(0,water(t)-(weather.season()==CalendarLife.Season.SUMMER?3:1))+(weather.raining()?4:0)));
    }
    static int cropGrowth(TownLife.Town t,int base,boolean rice){
        if(!active(t)||base<=0)return base;
        if(water(t)==0)return rice?0:Math.max(0,base-1);
        return base;
    }
    static void maintain(TownLife.Town t,WorldState s){
        var c=AgriFoodLife.worker(t,s,MECHANIC,TownLife.Place.FARM);
        if(c==null)return;
        String asset=List.of("tractor","harvester","pump").stream().min(java.util.Comparator.comparingInt(x->get(t,x))).orElseThrow();
        if(get(t,asset)>55||AgriFoodLife.cash(t)<PART_COST+AgriFoodLife.WAGE||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return;
        if(!AgriFoodLife.paid(t,c,false))return;
        SupplyChainLife.pay(t,AgriFoodLife.ACCOUNT,"city-maintenance",PART_COST);
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        set(t,asset,Math.min(100,get(t,asset)+45));add(t,"repairs",1);
    }
    static void irrigate(TownLife.Town t,WorldState s){
        var source=t.naturalResources.get("river-water");
        if(source==null||source.stock<=0||water(t)>=WATER_CAP||get(t,"pump")<=25)return;
        var c=AgriFoodLife.worker(t,s,IRRIGATION,TownLife.Place.FARM);
        if(c==null||!GridDistributionLife.power(t,TownLife.Place.FARM)
            ||AgriFoodLife.cash(t)<AgriFoodLife.WAGE+(EnergyLife.enabled(t)?EnergyLife.PRICE:0))return;
        if(!EnergyLife.buy(t,AgriFoodLife.ACCOUNT)||!AgriFoodLife.paid(t,c,false))return;
        int n=Math.min(6,Math.min(source.stock,WATER_CAP-water(t)));
        source.stock-=n;add(t,"water",n);add(t,"irrigated",n);
        set(t,"pump",Math.max(0,get(t,"pump")-3));
    }
    static boolean mechanized(TownLife.Town t){
        return active(t)&&get(t,"tractor")>25&&get(t,"harvester")>25&&FuelLife.stock(t)>0
            &&AgriFoodLife.cash(t)>=AgriFoodLife.WAGE+FuelLife.PRICE;
    }
    static void machineHarvest(TownLife.Town t){
        if(!mechanized(t)&&!(active(t)&&get(t,"tractor")>25&&get(t,"harvester")>25&&FuelLife.stock(t)>0&&AgriFoodLife.cash(t)>=FuelLife.PRICE))return;
        if(!SupplyChainLife.pay(t,AgriFoodLife.ACCOUNT,FuelLife.ACCOUNT,FuelLife.PRICE))return;
        t.industryStocks.merge(FuelLife.STOCK,-1,Math::addExact);
        for(String asset:List.of("tractor","harvester"))set(t,asset,Math.max(0,get(t,asset)-4));
        add(t,"machine-days",1);
    }
    static int milk(TownLife.Town t,int amount){
        return !active(t)?amount:amount*get(t,"herd-health")/100;
    }
    static void herdDay(TownLife.Town t,WorldState s,boolean fed){
        if(!active(t))return;
        int health=get(t,"herd-health");
        if(!fed)health=Math.max(0,health-8);
        else health=Math.min(100,health+1);
        // A deterministic fictional disease event, not a veterinary risk estimate.
        if(s.day%19==0)health=Math.max(0,health-20);
        set(t,"herd-health",health);
    }
    static void veterinary(TownLife.Town t,WorldState s){
        if(get(t,"herd-health")>=85||AgriFoodLife.get(t,"cattle")<=0)return;
        var c=AgriFoodLife.worker(t,s,VET,TownLife.Place.FARM);
        if(c==null||AgriFoodLife.cash(t)<VET_COST+AgriFoodLife.WAGE)return;
        if(!AgriFoodLife.paid(t,c,false))return;
        SupplyChainLife.pay(t,AgriFoodLife.ACCOUNT,"city-maintenance",VET_COST);
        set(t,"herd-health",Math.min(100,get(t,"herd-health")+25));add(t,"vet-visits",1);
    }
    static void prepare(TownLife.Town t,WorldState s){
        if(!active(t)||get(t,"service-day")>=s.day)return;
        set(t,"service-day",s.day);maintain(t,s);irrigate(t,s);veterinary(t,s);
    }
    record View(boolean active,int tractor,int harvester,int pump,int water,int waterCapacity,
        int herdHealth,int repairs,int vetVisits,int machineDays,int irrigated){}
    static View view(TownLife.Town t){
        return new View(active(t),get(t,"tractor"),get(t,"harvester"),get(t,"pump"),water(t),WATER_CAP,
            get(t,"herd-health"),get(t,"repairs"),get(t,"vet-visits"),get(t,"machine-days"),get(t,"irrigated"));
    }
}
