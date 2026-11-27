package com.holovin.npclife;

import java.util.Comparator;

/** Finite municipal gas supply, paid regional imports and district service.
 * Abstract gas units and equipment condition, not a physical pipeline model. */
final class GasNetworkLife {
    private GasNetworkLife(){}
    static final String ACCOUNT="gas-utility", SUPPLIER="regional-gas-supplier", ROLE="Gas network operator";
    static final int CAPACITY=32, PRICE=2, WAGE=4, IMPORT_LIMIT=12;
    static String key(String f){return "gas-network:"+f;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(f),0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(f),n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean active(TownLife.Town t){return get(t,"enabled")>0;}
    static int stock(TownLife.Town t){return get(t,"stock");}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static int condition(TownLife.Town t){return t.industryStocks.getOrDefault(key("condition"),100);}
    static boolean available(TownLife.Town t){return !active(t)||condition(t)>25&&stock(t)>0;}
    static void ensure(TownLife.Town t){
        if(active(t)||!StaffingLife.enabled(t))return;
        set(t,"enabled",1);
        SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,Math.min(40,t.employerFunds.getOrDefault("city-maintenance",0)));
    }
    static TownLife.Citizen worker(TownLife.Town t,WorldState s){
        return t.people.values().stream().filter(c->ROLE.equals(c.occupation)&&c.workplace==TownLife.Place.FUEL_STATION
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c))
            .min(Comparator.comparing(c->c.id)).orElse(null);
    }
    static void maintain(TownLife.Town t){
        if(condition(t)>55||cash(t)<5+WAGE||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return;
        if(!SupplyChainLife.pay(t,ACCOUNT,"city-maintenance",5))return;
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        set(t,"condition",Math.min(100,condition(t)+40));add(t,"repairs",1);
    }
    static int deliver(TownLife.Town t,WorldState s){
        var c=worker(t,s);
        if(c==null)return 0;
        maintain(t);
        if(condition(t)<=25)return 0;
        int n=Math.min(IMPORT_LIMIT,Math.min(CAPACITY-stock(t),cash(t)-WAGE));
        if(n<=0||!SupplyChainLife.pay(t,ACCOUNT,SUPPLIER,n))return 0;
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);
        add(t,"stock",n);add(t,"imported",n);
        set(t,"condition",Math.max(0,condition(t)-2));return n;
    }
    static boolean canBuy(TownLife.Town t,String account,int otherCost){
        return !active(t)||available(t)&&t.employerFunds.getOrDefault(account,0)>=PRICE+otherCost;
    }
    static boolean buy(TownLife.Town t,String account){
        if(!active(t))return true;
        if(!available(t)||!SupplyChainLife.pay(t,account,ACCOUNT,PRICE))return false;
        add(t,"stock",-1);add(t,"industrial-used",1);return true;
    }
    static boolean homeUse(TownLife.Town t){
        if(!active(t))return true;
        if(!available(t))return false;
        add(t,"stock",-1);add(t,"home-used",1);return true;
    }
    static boolean heatPlant(FactoryNetworkLife.Plant p){
        return java.util.List.of("paper-mill","textile-mill","glassworks","chemical-works","cement-works").contains(p.id());
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=680||get(t,"day")>=s.day)return;
        ensure(t);set(t,"day",s.day);deliver(t,s);
        // Abstract public residential demand, higher during cold weather.
        int demand=WeatherLife.at(s.day,s.minute).temperatureC()<12?3:1;
        int supplied=condition(t)>25?Math.min(demand,Math.min(stock(t),t.employerFunds.getOrDefault("city-maintenance",0)/PRICE)):0;
        if(supplied>0&&SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,supplied*PRICE)){
            add(t,"stock",-supplied);add(t,"public-used",supplied);
        }
        if(supplied<demand)add(t,"shortage-days",1);
    }
    record View(boolean active,int stock,int capacity,int condition,boolean available,int imported,int homeUsed,int publicUsed,int industrialUsed,int shortageDays,int repairs,int funds){}
    static View view(TownLife.Town t){
        return new View(active(t),stock(t),CAPACITY,condition(t),available(t),get(t,"imported"),get(t,"home-used"),
            get(t,"public-used"),get(t,"industrial-used"),get(t,"shortage-days"),get(t,"repairs"),cash(t));
    }
}
