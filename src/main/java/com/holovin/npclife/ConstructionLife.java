package com.holovin.npclife;

import java.util.List;

/** A single persistent plot with three paid construction stages and finite industrial inputs. */
final class ConstructionLife {
    private ConstructionLife(){}
    static final String OWNER="build:owner", STAGE="build:stage", STEEL="build:steel", PARTS="build:parts",
            RESERVE="build:reserve", LAST="build:last-day", ENABLED="build:enabled";
    static final String CONTRACTOR="construction-contractors";
    static final int LAND=60, SALE=150, LABOR=10, STEEL_PRICE=4, PART_PRICE=5, STAGES=3;
    static final List<String> KEYS=List.of(OWNER,STAGE,STEEL,PARTS,RESERVE,LAST,ENABLED);
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static void ensure(TownLife.Town t){for(String k:KEYS)t.industryStocks.putIfAbsent(k,0);}
    static boolean yours(TownLife.Town t){return get(t,OWNER)==t.player.incarnation;}
    static String act(TownLife.Town t,String action,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.OFFICE||minute<540||minute>=1020)
            throw new IllegalArgumentException("Visit the construction office 09:00–17:00.");
        if(action.equals("construction-buy")){
            if(get(t,OWNER)!=0||get(t,STAGE)>=STAGES||p.money<LAND)
                throw new IllegalArgumentException("Need an available plot and 60 EUR.");
            int city=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),LAND);
            p.money-=LAND;t.employerFunds.put("city-maintenance",city);set(t,OWNER,p.incarnation);
            return "Purchased the Via Verdi construction plot.";
        }
        if(!yours(t))throw new IllegalArgumentException("Own the plot first.");
        int stage=get(t,STAGE);
        if(action.equals("construction-sell")){
            int city=t.employerFunds.getOrDefault("city-maintenance",0);
            if(stage<STAGES||city<SALE)throw new IllegalArgumentException("Need a finished house and municipal funds.");
            int money=Math.addExact(p.money,Math.addExact(SALE,get(t,RESERVE)));
            t.employerFunds.put("city-maintenance",city-SALE);p.money=money;
            set(t,OWNER,0);set(t,RESERVE,0);set(t,ENABLED,0);
            return "Sold the finished house to the city for 150 EUR. The building remains.";
        }
        if(action.equals("construction-withdraw")){
            int n=Math.min(10,get(t,RESERVE));
            if(n==0)throw new IllegalArgumentException("No unused labor funds.");
            int money=Math.addExact(p.money,n);p.money=money;set(t,RESERVE,get(t,RESERVE)-n);
            return "Returned "+n+" EUR from the labor reserve.";
        }
        if(stage>=STAGES)throw new IllegalArgumentException("The house is already finished.");
        if(action.equals("construction-steel")||action.equals("construction-parts")){
            boolean steel=action.equals("construction-steel");
            String key=steel?STEEL:PARTS, source=steel?IndustryLife.FACTORY_STEEL:IndustryLife.FACTORY_PARTS;
            int cost=steel?STEEL_PRICE:PART_PRICE;
            int needed=steel?STAGES-stage:(stage==0?2:STAGES-stage);
            if(get(t,key)>=needed||IndustryLife.stock(t,source)<1||p.money<cost)
                throw new IllegalArgumentException("Need factory stock, funds and remaining material demand.");
            int factory=Math.addExact(t.employerFunds.getOrDefault("machinist",0),cost);
            p.money-=cost;t.employerFunds.put("machinist",factory);
            t.industryStocks.merge(source,-1,Integer::sum);set(t,key,get(t,key)+1);
            return "Purchased and reserved one "+(steel?"steel":"parts")+" unit at the site.";
        }
        return switch(action){
            case "construction-fund" -> {
                if(p.money<10)throw new IllegalArgumentException("Need 10 EUR.");
                int amount=Math.addExact(get(t,RESERVE),10);p.money-=10;set(t,RESERVE,amount);
                yield "Added 10 EUR to the labor reserve.";
            }
            case "construction-start" -> {set(t,ENABLED,1);yield "Authorized the contracted crew to work.";}
            case "construction-pause" -> {set(t,ENABLED,0);yield "Paused construction.";}
            default -> throw new IllegalArgumentException("Unknown construction action.");
        };
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=840||get(t,LAST)>=day||!t.player.alive||!yours(t)||get(t,ENABLED)==0)return;
        set(t,LAST,day);
        int stage=get(t,STAGE),parts=stage==0?0:1;
        if(stage>=STAGES||get(t,STEEL)<1||get(t,PARTS)<parts||get(t,RESERVE)<LABOR)return;
        int paid=Math.addExact(t.employerFunds.getOrDefault(CONTRACTOR,0),LABOR);
        set(t,STEEL,get(t,STEEL)-1);set(t,PARTS,get(t,PARTS)-parts);
        set(t,RESERVE,get(t,RESERVE)-LABOR);t.employerFunds.put(CONTRACTOR,paid);
        set(t,STAGE,stage+1);
        if(stage+1==STAGES)set(t,ENABLED,0);
    }
    static void estate(TownLife.Town t){
        if(!yours(t))return;
        t.employerFunds.merge("city-maintenance",get(t,RESERVE),Math::addExact);
        set(t,RESERVE,0);set(t,OWNER,0);set(t,ENABLED,0);
    }
    record Report(boolean owned,int stage,boolean enabled,int steel,int parts,int reserve,int factorySteel,int factoryParts,int landPrice,int salePrice,int laborPrice) {}
    static Report report(TownLife.Town t){
        return new Report(yours(t),get(t,STAGE),get(t,ENABLED)>0,get(t,STEEL),get(t,PARTS),get(t,RESERVE),
                IndustryLife.stock(t,IndustryLife.FACTORY_STEEL),IndustryLife.stock(t,IndustryLife.FACTORY_PARTS),LAND,SALE,LABOR);
    }
}
