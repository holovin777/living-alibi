package com.holovin.npclife;

import java.util.List;

/** Daily fictional energy units: renewable generation, fuel backup, finite battery,
 * priority public supply and paid industrial consumption. No physical grid solver. */
final class EnergyLife {
    private EnergyLife(){}
    static final String ACCOUNT="energy-utility";
    static final int CAPACITY=32, PRIORITY=4, PRICE=2, WAGE=4;
    record Station(String id,String role,TownLife.Place site,int capacity){}
    static final List<Station> STATIONS=List.of(
        new Station("solar","Solar plant operator",TownLife.Place.FARM,12),
        new Station("hydro","Hydropower operator",TownLife.Place.RIVER,8),
        new Station("thermal","Thermal plant operator",TownLife.Place.FACTORY,10));
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault("energy:"+f,0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put("energy:"+f,n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean enabled(TownLife.Town t){return get(t,"enabled")>0;}
    static boolean publicPower(TownLife.Town t){return !enabled(t)||get(t,"public-power")==1;}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static int condition(TownLife.Town t,Station p){return t.industryStocks.getOrDefault("energy:"+p.id()+":condition",100);}
    static boolean role(String occupation){return STATIONS.stream().anyMatch(p->p.role().equals(occupation));}
    static TownLife.Citizen operator(TownLife.Town t,WorldState s,Station p){
        return t.people.values().stream().filter(c->p.role().equals(c.occupation)&&c.workplace==p.site()
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)).min(java.util.Comparator.comparing(c->c.id)).orElse(null);
    }
    static void ensure(TownLife.Town t){
        if(enabled(t)||!StaffingLife.enabled(t))return;
        set(t,"enabled",1);set(t,"public-power",1);
        int investment=Math.min(60,t.employerFunds.getOrDefault("city-maintenance",0));
        SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,investment);
    }
    static int potential(TownLife.Town t,WorldState s,Station p){
        if(p.id().equals("solar")){
            int day=Math.floorMod(s.day-1,365);
            return day<60||day>=305?6:12;
        }
        if(p.id().equals("hydro")){
            var river=t.naturalResources.get("river-water");
            return river!=null&&river.stock>0?8:0;
        }
        return 10;
    }
    static void generate(TownLife.Town t,WorldState s,Station p){
        set(t,p.id()+":generated",0);
        var c=operator(t,s,p);
        if(c==null)return;
        if(condition(t,p)<=55&&cash(t)>=5+WAGE&&t.technicalMaterials.getOrDefault("spare-parts",0)>0){
            SupplyChainLife.pay(t,ACCOUNT,"city-maintenance",5);
            t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
            set(t,p.id()+":condition",Math.min(100,condition(t,p)+40));
        }
        if(condition(t,p)<=25)return;
        boolean thermal=p.id().equals("thermal");
        // Thermal backup only runs if renewable output and stored energy are insufficient.
        if(thermal&&(get(t,"battery")>=PRIORITY+FactoryNetworkLife.PLANTS.size()||FuelLife.stock(t)<1))return;
        int units=Math.min(CAPACITY-get(t,"battery"),potential(t,s,p));
        if(units<=0||cash(t)<WAGE+(thermal?FuelLife.PRICE:0))return;
        if(thermal){
            SupplyChainLife.pay(t,ACCOUNT,FuelLife.ACCOUNT,FuelLife.PRICE);
            t.industryStocks.merge(FuelLife.STOCK,-1,Math::addExact);
        }
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);
        add(t,"battery",units);add(t,"generated",units);set(t,p.id()+":generated",units);
        set(t,p.id()+":condition",Math.max(0,condition(t,p)-2));
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=690||get(t,"day")>=s.day)return;
        ensure(t);set(t,"day",s.day);set(t,"generated",0);set(t,"industrial-used",0);
        for(var p:STATIONS)generate(t,s,p);
        // Public network has priority over factory batches. Daily payment is a municipal service purchase.
        boolean supply=get(t,"battery")>=PRIORITY&&SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,PRIORITY*PRICE);
        if(supply)add(t,"battery",-PRIORITY);
        set(t,"public-power",supply?1:0);
        if(!supply)add(t,"outage-days",1);
    }
    static boolean canBuy(TownLife.Town t,String account,int materialCost){
        return !enabled(t)||get(t,"battery")>0&&t.employerFunds.getOrDefault(account,0)>=materialCost+PRICE;
    }
    static boolean buy(TownLife.Town t,String account){
        if(!enabled(t))return true;
        if(get(t,"battery")<=0||!SupplyChainLife.pay(t,account,ACCOUNT,PRICE))return false;
        add(t,"battery",-1);add(t,"industrial-used",1);return true;
    }
    record StationView(String id,String district,int capacity,int condition,int generated,int workers){}
    record View(boolean active,int stored,int capacity,int generated,int industrialUsed,boolean publicPower,int outageDays,int funds,List<StationView> stations){}
    static View view(TownLife.Town t){
        return new View(enabled(t),get(t,"battery"),CAPACITY,get(t,"generated"),get(t,"industrial-used"),publicPower(t),get(t,"outage-days"),cash(t),
            STATIONS.stream().map(p->new StationView(p.id(),p.site().name(),p.capacity(),condition(t,p),get(t,p.id()+":generated"),
                (int)t.people.values().stream().filter(c->c.alive&&c.employed&&p.role().equals(c.occupation)&&c.workplace==p.site()).count())).toList());
    }
}
