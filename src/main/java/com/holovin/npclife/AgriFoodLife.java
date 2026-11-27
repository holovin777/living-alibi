package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** Local agricultural cooperative with finite crops, feed and food stocks. */
final class AgriFoodLife {
    private AgriFoodLife(){}
    static final String ACCOUNT="agri-food-cooperative", FIELD="Grain farmer", HERD="Dairy farmer",
        MILL="Mill operator", DAIRY="Dairy processor", FOOD="Food processing worker";
    static final int CAP=24,WAGE=4;
    static final List<String> PRODUCTS=List.of("flour","milk","cheese","pasta","oil","meat");
    static String key(String f){return "agri-food:"+f;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(f),0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(f),n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static boolean active(TownLife.Town t){return get(t,"enabled")>0;}
    static boolean role(String job){return List.of(FIELD,HERD,MILL,DAIRY,FOOD).contains(job);}
    static void ensure(TownLife.Town t){
        if(active(t)||!StaffingLife.enabled(t))return;
        set(t,"enabled",1);set(t,"cattle",4);set(t,"field:wheat",8);set(t,"field:oilseed",4);
        SupplyChainLife.pay(t,"farm-worker",ACCOUNT,Math.min(80,t.employerFunds.getOrDefault("farm-worker",0)));
    }
    static TownLife.Citizen worker(TownLife.Town t,WorldState s,String role,TownLife.Place site){
        return t.people.values().stream().filter(c->role.equals(c.occupation)&&c.workplace==site
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c))
            .min(Comparator.comparing(c->c.id)).orElse(null);
    }
    static boolean paid(TownLife.Town t,TownLife.Citizen c,boolean power){
        if(c==null||cash(t)<WAGE+(power&&EnergyLife.enabled(t)?EnergyLife.PRICE:0))return false;
        if(power&&(!GridDistributionLife.power(t,TownLife.Place.FACTORY)||!EnergyLife.buy(t,ACCOUNT)))return false;
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);return true;
    }
    static void grow(TownLife.Town t,WorldState s){
        int growth=switch(WeatherLife.at(s.day,s.minute).season()){case SPRING,SUMMER -> 3;case AUTUMN -> 1;case WINTER -> 0;};
        growth=FarmModernizationLife.cropGrowth(t,growth,false);
        for(String crop:List.of("wheat","oilseed"))set(t,"field:"+crop,Math.min(CAP,get(t,"field:"+crop)+growth));
    }
    static void harvest(TownLife.Town t,WorldState s){
        boolean machine=FarmModernizationLife.mechanized(t);
        boolean manual=FarmModernizationLife.active(t)&&!machine;
        int wheat=Math.min(manual?2:6,Math.min(get(t,"field:wheat"),CAP-get(t,"wheat")));
        int oil=Math.min(manual?1:3,Math.min(get(t,"field:oilseed"),CAP-get(t,"oilseed")));
        if(wheat+oil==0||!paid(t,worker(t,s,FIELD,TownLife.Place.FARM),false))return;
        if(machine)FarmModernizationLife.machineHarvest(t);
        add(t,"field:wheat",-wheat);add(t,"wheat",wheat);add(t,"field:oilseed",-oil);add(t,"oilseed",oil);
    }
    static void mill(TownLife.Town t,WorldState s){
        if(get(t,"wheat")<2||get(t,"flour")>CAP-2||get(t,"feed")>=CAP||!paid(t,worker(t,s,MILL,TownLife.Place.FACTORY),true))return;
        add(t,"wheat",-2);add(t,"flour",2);add(t,"feed",1);
    }
    static void herd(TownLife.Town t,WorldState s){
        int cattle=get(t,"cattle");if(cattle<=0)return;
        int need=(cattle+3)/4,milk=Math.min(FarmModernizationLife.milk(t,cattle),CAP-get(t,"milk"));
        if(get(t,"feed")<need||!paid(t,worker(t,s,HERD,TownLife.Place.FARM),false)){add(t,"unfed-days",1);FarmModernizationLife.herdDay(t,s,false);return;}
        add(t,"feed",-need);add(t,"milk",milk);FarmModernizationLife.herdDay(t,s,true);
        if(s.day%28==0&&cattle<8&&(!FarmModernizationLife.active(t)||FarmModernizationLife.get(t,"herd-health")>=70))add(t,"cattle",1);
    }
    static void dairy(TownLife.Town t,WorldState s){
        if(get(t,"milk")<2||get(t,"cheese")>=CAP||!paid(t,worker(t,s,DAIRY,TownLife.Place.FACTORY),true))return;
        add(t,"milk",-2);add(t,"cheese",1);
    }
    static void food(TownLife.Town t,WorldState s){
        boolean pasta=get(t,"flour")>=2&&get(t,"pasta")<=CAP-2;
        boolean oil=get(t,"oilseed")>=2&&get(t,"oil")<CAP;
        boolean meat=get(t,"cattle")>4&&get(t,"meat")<=CAP-3&&s.day%7==0;
        if(!pasta&&!oil&&!meat||!paid(t,worker(t,s,FOOD,TownLife.Place.FACTORY),true))return;
        if(pasta){add(t,"flour",-2);add(t,"pasta",2);}
        if(oil){add(t,"oilseed",-2);add(t,"oil",1);if(get(t,"feed")<CAP)add(t,"feed",1);}
        if(meat){add(t,"cattle",-1);add(t,"meat",3);}
    }
    static void deliver(TownLife.Town t,WorldState s){
        if(FuelLife.stock(t)<1||!SupplyChainLife.attending(t,TownLife.Place.LOGISTICS_DEPOT,"Warehouse worker"))return;
        int possible=0;
        for(String id:PRODUCTS)possible+=Math.min(get(t,id),Math.max(0,FoodSupplyLife.capacity(t,id)-FoodSupplyLife.stock(t,id)));
        if(possible==0||cash(t)<FuelLife.PRICE||t.employerFunds.getOrDefault(FoodSupplyLife.SHOP,0)<1)return;
        SupplyChainLife.pay(t,ACCOUNT,FuelLife.ACCOUNT,FuelLife.PRICE);t.industryStocks.merge(FuelLife.STOCK,-1,Math::addExact);
        int remaining=12;
        for(int i=0;i<PRODUCTS.size()&&remaining>0;i++){
            String id=PRODUCTS.get(Math.floorMod(s.day+i,PRODUCTS.size()));
            int price=Math.max(1,RetailLife.product(id).price()-1);
            int n=Math.min(remaining,Math.min(get(t,id),Math.max(0,FoodSupplyLife.capacity(t,id)-FoodSupplyLife.stock(t,id))));
            n=Math.min(n,t.employerFunds.getOrDefault(FoodSupplyLife.SHOP,0)/price);
            if(n<=0||!SupplyChainLife.pay(t,FoodSupplyLife.SHOP,ACCOUNT,n*price))continue;
            add(t,id,-n);t.retailStock.merge(id,n,Math::addExact);add(t,"sold",n);remaining-=n;
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(!active(t))ensure(t);
        if(!FarmModernizationLife.active(t))FarmModernizationLife.ensure(t);
        if(s.minute==360&&get(t,"growth-day")<s.day){set(t,"growth-day",s.day);FarmModernizationLife.dawn(t,s);grow(t,s);}
        if(s.minute==725&&get(t,"production-day")<s.day){
            set(t,"production-day",s.day);FarmModernizationLife.prepare(t,s);harvest(t,s);mill(t,s);herd(t,s);dairy(t,s);food(t,s);
        }
        if(s.minute==910&&get(t,"delivery-day")<s.day){
            set(t,"delivery-day",s.day);deliver(t,s);
            if(!GridDistributionLife.power(t,TownLife.Place.FACTORY)&&get(t,"milk")>0){
                add(t,"milk",-1);WasteLife.discard(t,"organic",1);add(t,"spoiled",1);
            }
        }
    }
    record Product(String id,int stock){}
    record View(boolean active,int wheatField,int oilseedField,int wheat,int oilseed,int cattle,int feed,int unfedDays,int sold,int spoiled,int funds,List<Product> products,FarmModernizationLife.View modernization){}
    static View view(TownLife.Town t){
        return new View(active(t),get(t,"field:wheat"),get(t,"field:oilseed"),get(t,"wheat"),get(t,"oilseed"),get(t,"cattle"),get(t,"feed"),get(t,"unfed-days"),get(t,"sold"),get(t,"spoiled"),cash(t),
            PRODUCTS.stream().map(id->new Product(id,get(t,id))).toList(),FarmModernizationLife.view(t));
    }
}
