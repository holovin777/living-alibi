package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Finite appliance assembly and paid distribution, in abstract physical units. */
final class SupplyChainLife {
    private SupplyChainLife(){}
    record Recipe(String id,int price,String buyer,int steel,int copper){}
    static final List<Recipe> RECIPES=List.of(
        new Recipe("television",65,"shop-assistant",1,1),
        new Recipe("fridge",80,"shop-assistant",2,1),
        new Recipe("washer",95,"shop-assistant",2,1),
        new Recipe("boiler",110,"technician",2,1));
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int v){t.industryStocks.put(k,v);}
    static String finished(String id){return "supply:factory:"+id;}
    static String retail(String id){return "supply:retail:"+id;}
    static int stock(TownLife.Town t,String id){return t.industryStocks.getOrDefault(retail(id),2);}
    static void take(TownLife.Town t,String id){
        int n=stock(t,id);if(n<1)throw new IllegalArgumentException("This item is sold out.");
        set(t,retail(id),n-1);
    }
    static boolean pay(TownLife.Town t,String from,String to,int amount){
        int cash=t.employerFunds.getOrDefault(from,0);if(cash<amount)return false;
        t.employerFunds.put(from,cash-amount);t.employerFunds.merge(to,amount,Math::addExact);return true;
    }
    static boolean assemble(TownLife.Town t,Recipe r){
        int steel=get(t,IndustryLife.FACTORY_STEEL),copper=get(t,IndustryLife.FACTORY_COPPER);
        if(steel<r.steel()||copper<r.copper()||get(t,finished(r.id()))>=3)return false;
        set(t,IndustryLife.FACTORY_STEEL,steel-r.steel());set(t,IndustryLife.FACTORY_COPPER,copper-r.copper());
        set(t,finished(r.id()),get(t,finished(r.id()))+1);return true;
    }
    static boolean deliver(TownLife.Town t,Recipe r){
        int cost=r.price()/2;
        if(stock(t,r.id())>=4||get(t,finished(r.id()))<1||FuelLife.stock(t)<1
            ||t.employerFunds.getOrDefault(r.buyer(),0)<cost+FuelLife.PRICE)return false;
        pay(t,r.buyer(),"machinist",cost);pay(t,r.buyer(),FuelLife.ACCOUNT,FuelLife.PRICE);
        set(t,FuelLife.STOCK,FuelLife.stock(t)-1);set(t,finished(r.id()),get(t,finished(r.id()))-1);
        set(t,retail(r.id()),stock(t,r.id())+1);return true;
    }
    static boolean attending(TownLife.Town t,TownLife.Place place,String role){
        return t.people.values().stream().anyMatch(c->c.alive&&c.employed&&role.equals(c.occupation)&&c.presentAt(place)&&"WORKING".equals(c.activity));
    }
    static boolean once(TownLife.Town t,String stage,int day){
        String key="supply:day:"+stage;if(get(t,key)>=day)return false;set(t,key,day);return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==835&&once(t,"assembly",s.day)&&attending(t,FACTORY,"Factory machinist")){
            for(int i=0;i<RECIPES.size();i++){
                var r=RECIPES.get(Math.floorMod(s.day+i,RECIPES.size()));
                if(stock(t,r.id())<4&&assemble(t,r))break;
            }
        }
        if(s.minute==900&&once(t,"delivery",s.day)&&attending(t,LOGISTICS_DEPOT,"Warehouse worker")&&attending(t,QUARRY,"Transport driver")){
            for(int i=0;i<RECIPES.size();i++)if(deliver(t,RECIPES.get(Math.floorMod(s.day+i,RECIPES.size()))))break;
        }
    }
}
