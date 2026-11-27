package com.holovin.npclife;

import java.util.Comparator;

/** Municipal water and wastewater treatment in finite fictional game units. */
final class WaterNetworkLife {
    private WaterNetworkLife(){}
    static final String ACCOUNT="water-utility", WATER_ROLE="Water treatment operator", SEWER_ROLE="Wastewater operator";
    static final int CAPACITY=48, SEWER_CAPACITY=32, WAGE=4;
    static String key(String f){return "water-network:"+f;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(f),0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(f),n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean active(TownLife.Town t){return get(t,"enabled")>0;}
    static boolean role(String job){return WATER_ROLE.equals(job)||SEWER_ROLE.equals(job);}
    static int condition(TownLife.Town t,String plant){return t.industryStocks.getOrDefault(key(plant+":condition"),100);}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static void ensure(TownLife.Town t){
        if(active(t)||!StaffingLife.enabled(t))return;
        set(t,"enabled",1);
        SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,Math.min(40,t.employerFunds.getOrDefault("city-maintenance",0)));
    }
    static TownLife.Citizen worker(TownLife.Town t,WorldState s,String role,TownLife.Place site){
        return t.people.values().stream().filter(c->role.equals(c.occupation)&&c.workplace==site
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c))
            .min(Comparator.comparing(c->c.id)).orElse(null);
    }
    static boolean operate(TownLife.Town t,TownLife.Citizen c,String plant,TownLife.Place site){
        if(c==null||!GridDistributionLife.power(t,site))return false;
        if(condition(t,plant)<=55&&cash(t)>=5+WAGE+EnergyLife.PRICE&&t.technicalMaterials.getOrDefault("spare-parts",0)>0){
            SupplyChainLife.pay(t,ACCOUNT,"city-maintenance",5);
            t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
            set(t,plant+":condition",Math.min(100,condition(t,plant)+40));
        }
        if(condition(t,plant)<=25||cash(t)<WAGE+(EnergyLife.enabled(t)?EnergyLife.PRICE:0)||!EnergyLife.buy(t,ACCOUNT))return false;
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);
        set(t,plant+":condition",Math.max(0,condition(t,plant)-2));return true;
    }
    static void generate(TownLife.Town t,WorldState s){
        var river=t.naturalResources.get("river-water");
        if(river==null)return;
        int n=Math.min(16,Math.min(river.stock,CAPACITY-get(t,"treated")));
        if(n<=0||!operate(t,worker(t,s,WATER_ROLE,TownLife.Place.RIVER),"water",TownLife.Place.RIVER))return;
        river.stock-=n;add(t,"treated",n);add(t,"produced",n);
    }
    static void process(TownLife.Town t,WorldState s){
        int n=Math.min(12,get(t,"sewage"));
        if(n<=0||!operate(t,worker(t,s,SEWER_ROLE,TownLife.Place.FACTORY),"sewer",TownLife.Place.FACTORY))return;
        add(t,"sewage",-n);add(t,"processed",n);
        var river=t.naturalResources.get("river-water");
        if(river!=null)river.stock+=Math.min(Math.max(0,river.capacity-river.stock),n);
    }
    static void discharge(TownLife.Town t){
        if(!active(t))return;
        if(get(t,"sewage")<SEWER_CAPACITY)add(t,"sewage",1);else add(t,"overflow",1);
    }
    static int fountain(TownLife.Town t,int requested){
        int n=Math.min(requested,Math.min(get(t,"treated"),UtilityLife.TANK_CAPACITY-t.cleanWater));
        n=Math.min(n,t.employerFunds.getOrDefault("city-maintenance",0));
        if(n<=0||!SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,n))return 0;
        add(t,"treated",-n);t.cleanWater+=n;return n;
    }
    static int sell(TownLife.Town t){
        int n=Math.min(8,Math.min(get(t,"treated"),FoodSupplyLife.capacity(t,"water")-FoodSupplyLife.stock(t,"water")));
        n=Math.min(n,t.employerFunds.getOrDefault(FoodSupplyLife.SHOP,0));
        if(n<=0)return 0;
        if(FactoryNetworkLife.stock(t,"packaging")>0){
            int packed=Math.min(n,FactoryNetworkLife.stock(t,"packaging")*4);
            int packs=(packed+3)/4;
            if(t.employerFunds.getOrDefault(ACCOUNT,0)>=packs&&SupplyChainLife.pay(t,ACCOUNT,FactoryNetworkLife.supplier("packaging"),packs)){
                FactoryNetworkLife.change(t,"packaging",-packs);
            }
        }
        if(!SupplyChainLife.pay(t,FoodSupplyLife.SHOP,ACCOUNT,n))return 0;
        add(t,"treated",-n);t.retailStock.merge("water",n,Math::addExact);add(t,"sold",n);return n;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=700||get(t,"day")>=s.day)return;
        ensure(t);set(t,"day",s.day);
        process(t,s);generate(t,s);
        int n=Math.min(4,Math.min(get(t,"treated"),SEWER_CAPACITY-get(t,"sewage")));
        n=Math.min(n,t.employerFunds.getOrDefault("city-maintenance",0));
        if(n>0&&GridDistributionLife.power(t,TownLife.Place.PARK)&&SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,n)){
            add(t,"treated",-n);add(t,"sewage",n);add(t,"domestic-used",n);
        }
        if(UtilityLife.status(t).fountainWorking())fountain(t,UtilityLife.DAILY_PUMP_UNITS+(SoftwareLife.deployed(t,"water-control")?2:0));
        sell(t);
        if(get(t,"treated")==0)add(t,"empty-reservoir-days",1);
    }
    record View(boolean active,int treated,int capacity,int sewage,int sewerCapacity,int waterCondition,int sewerCondition,
        int produced,int processed,int sold,int domesticUsed,int emptyReservoirDays,int overflow,int funds){}
    static View view(TownLife.Town t){
        return new View(active(t),get(t,"treated"),CAPACITY,get(t,"sewage"),SEWER_CAPACITY,condition(t,"water"),condition(t,"sewer"),
            get(t,"produced"),get(t,"processed"),get(t,"sold"),get(t,"domestic-used"),get(t,"empty-reservoir-days"),get(t,"overflow"),cash(t));
    }
}
