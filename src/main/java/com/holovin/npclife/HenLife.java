package com.holovin.npclife;

/** A small home flock with finite daily feed, drinking water and nest storage. */
final class HenLife {
    private HenLife() {}
    static final String COOP="hens:coop", COUNT="hens:count", HEALTH="hens:health", EGGS="hens:eggs",
            FED="hens:fed-day", WATER="hens:water-day", LAST="hens:last-tick";
    static final int COOP_PRICE=20, HEN_PRICE=12, MAX_HENS=3, NEST_CAPACITY=6;
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{COOP,COUNT,HEALTH,EGGS,FED,WATER,LAST})t.industryStocks.putIfAbsent(key,0);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(key,value);}
    static void clear(TownLife.Town t){for(String key:new String[]{COOP,COUNT,HEALTH,EGGS,FED,WATER,LAST})set(t,key,0);}
    static String act(TownLife.Town t,String action,int day,int minute) {
        if("sell-eggs".equals(action))return RetailLife.buyEggs(t,minute);
        GardenLife.present(t);
        var p=t.player;
        int count=get(t,COUNT);
        if("build-coop".equals(action)) {
            if(get(t,COOP)>0)throw new IllegalArgumentException("You already have a coop.");
            if(p.money<COOP_PRICE)throw new IllegalArgumentException("Need 20 EUR to build a coop.");
            p.money-=COOP_PRICE;t.employerFunds.merge("technician",COOP_PRICE,Math::addExact);
            set(t,COOP,1);
            return "Built a coop for three hens.";
        }
        if(get(t,COOP)==0)throw new IllegalArgumentException("Build a coop first.");
        return switch(action) {
            case "buy-hen" -> {
                if(count>=MAX_HENS)throw new IllegalArgumentException("The coop is full.");
                if(get(t,FED)==day||get(t,WATER)==day)
                    throw new IllegalArgumentException("Buy additional hens before today's care or return tomorrow.");
                if(p.money<HEN_PRICE)throw new IllegalArgumentException("Need 12 EUR for a hen.");
                p.money-=HEN_PRICE;t.employerFunds.merge(FarmLife.ACCOUNT,HEN_PRICE,Math::addExact);
                set(t,COUNT,count+1);
                if(count==0)set(t,HEALTH,100);
                yield "Bought one hen from the farm.";
            }
            case "feed-hens","water-hens" -> {
                if(count==0||minute>=1080)throw new IllegalArgumentException("Care for hens before 18:00.");
                boolean feeding=action.equals("feed-hens");
                String marker=feeding?FED:WATER, item=feeding?"rice":"water";
                if(get(t,marker)==day)throw new IllegalArgumentException("Already done today.");
                if(FoodLife.quantity(p,item,false)<1)throw new IllegalArgumentException("Bring one "+item+" unit in your bag.");
                FoodLife.change(p,item,false,-1);set(t,marker,day);
                yield feeding?"Fed the flock one rice unit.":"Provided one unit of drinking water.";
            }
            case "collect-eggs" -> {
                if(get(t,EGGS)<1)throw new IllegalArgumentException("No eggs in the nest.");
                if(FoodLife.total(p,true)>=24)throw new IllegalArgumentException("Your cupboard is full.");
                set(t,EGGS,get(t,EGGS)-1);FoodLife.change(p,"eggs",true,1);
                yield "Collected one egg unit into your cupboard.";
            }
            default -> throw new IllegalArgumentException("Unknown chicken coop action.");
        };
    }
    static void tick(TownLife.Town t,int day,int minute) {
        if(minute!=1080||get(t,LAST)>=day)return;
        set(t,LAST,day);
        if(!t.player.ownsHouse||get(t,COUNT)==0)return;
        boolean cared=get(t,FED)==day&&get(t,WATER)==day;
        int health=Math.max(0,Math.min(100,get(t,HEALTH)+(cared?10:-15)));
        set(t,HEALTH,health);
        if(cared&&health>=50) {
            int production=GardenLife.winter(day)?Math.min(1,get(t,COUNT)):get(t,COUNT);
            set(t,EGGS,Math.min(NEST_CAPACITY,get(t,EGGS)+production));
        }
    }
    record Report(boolean coop,int hens,int health,int eggs,int nestCapacity,boolean fedToday,boolean wateredToday,
                  boolean winter,int coopPrice,int henPrice,int maxHens,int eggPrice,int marketFree,int marketBudget) {}
    static Report report(TownLife.Town t,int day) {
        return new Report(get(t,COOP)>0,get(t,COUNT),get(t,HEALTH),get(t,EGGS),NEST_CAPACITY,
                get(t,FED)==day,get(t,WATER)==day,GardenLife.winter(day),COOP_PRICE,HEN_PRICE,MAX_HENS,
                RetailLife.EGG_PRICE,RetailLife.eggSpace(t),t.employerFunds.getOrDefault("shop-assistant",0));
    }
}
