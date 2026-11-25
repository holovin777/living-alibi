package com.holovin.npclife;

import java.util.List;

/** Perennial home orchard, with separate establishment and annual fruiting. */
final class OrchardLife {
    private OrchardLife() {}
    record Crop(String id,String label,int price,CalendarLife.Season season) {}
    static final List<Crop> CROPS=List.of(
            new Crop("apples","Apple tree",12,CalendarLife.Season.AUTUMN),
            new Crop("pears","Pear tree",12,CalendarLife.Season.AUTUMN),
            new Crop("plums","Plum tree",10,CalendarLife.Season.SUMMER),
            new Crop("grapes","Grapevine",10,CalendarLife.Season.AUTUMN));
    static final int MATURE=5, RIPE=3, YIELD=3, SELL_PRICE=2;
    static final String WATER="orchard:watered", LAST="orchard:last";
    static final String HIVE="orchard:hive", HONEY="orchard:honey", BEE_WATER="orchard:bee-water", BEE_PROGRESS="orchard:bee-progress";
    static final int HIVE_PRICE=30, HONEY_CAPACITY=4, HONEY_PRICE=3;
    static final String CELLAR="orchard:cellar", WINE_DUE="orchard:wine-due", WINE_READY="orchard:wine-ready";
    static final int CELLAR_PRICE=35, JUICE_PRICE=5, WINE_PRICE=8, WINE_DAYS=7;
    static String key(Crop c,String field){return "orchard:"+c.id()+":"+field;}
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int amount){t.industryStocks.put(key,amount);}
    static List<String> keys(){
        var keys=new java.util.ArrayList<String>(List.of(WATER,LAST,HIVE,HONEY,BEE_WATER,BEE_PROGRESS,CELLAR,WINE_DUE,WINE_READY));
        for(var c:CROPS)for(String f:List.of("planted","age","fruit","year","pollinated"))keys.add(key(c,f));
        return keys;
    }
    static void ensure(TownLife.Town t){for(String k:keys())t.industryStocks.putIfAbsent(k,0);}
    static void clear(TownLife.Town t){for(String k:keys())set(t,k,0);}
    static boolean isFruit(String id){return CROPS.stream().anyMatch(c->c.id().equals(id));}
    static boolean handles(String action){return action!=null&&(action.startsWith("orchard-")||action.startsWith("sell-fruit-")||action.equals("sell-honey")||action.equals("sell-grape-juice")||action.equals("sell-wine"));}
    static String act(TownLife.Town t,String action,int day,int minute) {
        if(action.equals("sell-honey"))return RetailLife.buyHoney(t,minute);
        if(action.equals("sell-grape-juice"))return RetailLife.buyCellarProduct(t,"grape-juice",minute);
        if(action.equals("sell-wine"))return RetailLife.buyCellarProduct(t,"wine",minute);
        if(action.startsWith("sell-fruit-"))return RetailLife.buyFruit(t,action.substring("sell-fruit-".length()),minute);
        GardenLife.present(t);
        var p=t.player;
        if(action.equals("orchard-build-cellar")) {
            if(get(t,CELLAR)>0||p.money<CELLAR_PRICE)throw new IllegalArgumentException("Need an empty cellar site and 35 EUR.");
            int balance=Math.addExact(t.employerFunds.getOrDefault("technician",0),CELLAR_PRICE);
            p.money-=CELLAR_PRICE;t.employerFunds.put("technician",balance);set(t,CELLAR,1);
            return "Installed a small cellar and grape press.";
        }
        if(action.equals("orchard-press-juice")) {
            if(get(t,CELLAR)==0)throw new IllegalArgumentException("Build the cellar first.");
            FoodLife.change(p,"grapes",true,-2);FoodLife.change(p,"grape-juice",true,1);
            return "Pressed one grape juice unit into the cupboard.";
        }
        if(action.equals("orchard-start-wine")) {
            if(get(t,CELLAR)==0||get(t,WINE_DUE)>0||get(t,WINE_READY)>0)
                throw new IllegalArgumentException("Need an installed cellar and an empty barrel.");
            int due=Math.addExact(day,WINE_DAYS);
            FoodLife.change(p,"grapes",true,-3);set(t,WINE_DUE,due);
            return "Started a wine batch. Ready on day "+due+" at 18:00.";
        }
        if(action.equals("orchard-collect-wine")) {
            if(get(t,CELLAR)==0||get(t,WINE_READY)==0||FoodLife.total(p,true)>=24)
                throw new IllegalArgumentException("Need mature wine and cupboard space.");
            FoodLife.change(p,"wine",true,1);set(t,WINE_READY,0);
            return "Moved one mature wine unit into the cupboard.";
        }
        if(action.equals("orchard-build-hive")) {
            if(get(t,HIVE)>0||p.money<HIVE_PRICE)throw new IllegalArgumentException("Need an empty apiary and 30 EUR.");
            int balance=Math.addExact(t.employerFunds.getOrDefault(FarmLife.ACCOUNT,0),HIVE_PRICE);
            p.money-=HIVE_PRICE;t.employerFunds.put(FarmLife.ACCOUNT,balance);set(t,HIVE,1);
            return "Installed a populated beehive.";
        }
        if(action.equals("orchard-water-bees")) {
            if(get(t,HIVE)==0||GardenLife.winter(day)||minute>=1080||get(t,BEE_WATER)==day)
                throw new IllegalArgumentException("Provide bee drinking water once before 18:00 outside winter.");
            FoodLife.change(p,"water",false,-1);set(t,BEE_WATER,day);
            return "Provided one drinking water unit for the bees.";
        }
        if(action.equals("orchard-collect-honey")) {
            if(get(t,HONEY)<1||FoodLife.total(p,true)>=24)
                throw new IllegalArgumentException("Need stored honey and cupboard space.");
            FoodLife.change(p,"honey",true,1);set(t,HONEY,get(t,HONEY)-1);
            return "Collected one honey unit into the cupboard.";
        }
        if(action.equals("orchard-water")||action.equals("orchard-water-rain")){
            boolean needsWater=CROPS.stream().anyMatch(c->get(t,key(c,"planted"))>0 &&
                    (get(t,key(c,"age"))<MATURE || (c.season()==CalendarLife.season(day)
                    && get(t,key(c,"year"))<CalendarLife.year(day) && get(t,key(c,"fruit"))<RIPE)));
            if(!needsWater||GardenLife.winter(day)||minute>=1080||get(t,WATER)==day||WeatherLife.at(day,minute).raining())
                throw new IllegalArgumentException("Water growing trees once on a dry day before 18:00.");
            if(action.equals("orchard-water-rain")){
                if(GardenLife.get(t,GardenLife.RAIN)<1)throw new IllegalArgumentException("Rain barrel empty.");
                GardenLife.set(t,GardenLife.RAIN,GardenLife.get(t,GardenLife.RAIN)-1);
            }else FoodLife.change(p,"water",false,-1);
            set(t,WATER,day);
            return "Watered the orchard.";
        }
        for(var c:CROPS){
            if(action.equals("orchard-plant-"+c.id())){
                if(GardenLife.winter(day)||get(t,key(c,"planted"))>0||get(t,WATER)==day)
                    throw new IllegalArgumentException("Plant an empty plot outside winter before today's watering.");
                if(p.money<c.price())throw new IllegalArgumentException("Not enough money for a sapling.");
                p.money-=c.price();t.employerFunds.merge(FarmLife.ACCOUNT,c.price(),Math::addExact);
                set(t,key(c,"planted"),1);set(t,key(c,"age"),0);set(t,key(c,"fruit"),0);set(t,key(c,"year"),0);
                return "Planted "+c.label()+".";
            }
            if(action.equals("orchard-harvest-"+c.id())){
                if(get(t,key(c,"planted"))==0||get(t,key(c,"fruit"))<RIPE||c.season()!=CalendarLife.season(day)
                        ||get(t,key(c,"year"))==CalendarLife.year(day))
                    throw new IllegalArgumentException("Fruit is not ripe in this season.");
                int amount=YIELD+(get(t,key(c,"pollinated"))==CalendarLife.year(day)?1:0);
                if(FoodLife.total(p,true)+amount>24)throw new IllegalArgumentException("Make room for "+amount+" fruit units.");
                FoodLife.change(p,c.id(),true,amount);
                set(t,key(c,"fruit"),0);set(t,key(c,"year"),CalendarLife.year(day));set(t,key(c,"pollinated"),0);
                return "Harvested "+amount+" units of "+c.id()+" into the cupboard.";
            }
        }
        throw new IllegalArgumentException("Unknown orchard action.");
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=1080||get(t,LAST)>=day)return;
        set(t,LAST,day);
        if(!t.player.ownsHouse)return;
        if(get(t,CELLAR)>0&&get(t,WINE_DUE)>0&&day>=get(t,WINE_DUE)) {
            set(t,WINE_DUE,0);set(t,WINE_READY,1);
        }
        var season=CalendarLife.season(day);
        boolean rain=WeatherLife.at(day,minute).raining();
        boolean wet=get(t,WATER)==day||rain;
        boolean bees=get(t,HIVE)>0&&get(t,BEE_WATER)==day&&season!=CalendarLife.Season.WINTER&&!rain;
        if(bees&&get(t,HONEY)<HONEY_CAPACITY) {
            int progress=get(t,BEE_PROGRESS)+1;
            if(progress>=2){set(t,HONEY,get(t,HONEY)+1);progress=0;}
            set(t,BEE_PROGRESS,progress);
        }
        for(var c:CROPS){
            if(get(t,key(c,"planted"))==0)continue;
            if(season!=c.season()){set(t,key(c,"fruit"),0);set(t,key(c,"pollinated"),0);}
            if(season==CalendarLife.Season.WINTER||!wet)continue;
            int age=get(t,key(c,"age"));
            if(age<MATURE)set(t,key(c,"age"),age+1);
            else if(season==c.season()&&get(t,key(c,"year"))<CalendarLife.year(day)&&get(t,key(c,"fruit"))<RIPE){
                set(t,key(c,"fruit"),get(t,key(c,"fruit"))+1);
                if(bees&&!c.id().equals("grapes"))set(t,key(c,"pollinated"),CalendarLife.year(day));
            }
        }
    }
    record Plant(String id,String label,int price,String season,boolean planted,int age,int fruit,boolean ready,boolean harvestedThisYear,int marketFree,boolean pollinated) {}
    record Report(List<Plant> plants,boolean wateredToday,boolean rainy,boolean winter,int marketBudget,int sellPrice,boolean hive,int hivePrice,int honey,int honeyCapacity,boolean beesWateredToday,int honeyMarketFree,int honeyPrice,boolean cellar,int cellarPrice,int wineDueDay,boolean wineReady,int juiceMarketFree,int wineMarketFree,int juicePrice,int winePrice) {}
    static Report report(TownLife.Town t,int day,int minute){
        return new Report(CROPS.stream().map(c->new Plant(c.id(),c.label(),c.price(),c.season().name(),
                get(t,key(c,"planted"))>0,get(t,key(c,"age")),get(t,key(c,"fruit")),
                get(t,key(c,"planted"))>0&&get(t,key(c,"fruit"))>=RIPE&&c.season()==CalendarLife.season(day)&&get(t,key(c,"year"))<CalendarLife.year(day),
                get(t,key(c,"year"))==CalendarLife.year(day),RetailLife.fruitSpace(t,c.id()),get(t,key(c,"pollinated"))==CalendarLife.year(day))).toList(),
                get(t,WATER)==day,WeatherLife.at(day,minute).raining(),GardenLife.winter(day),t.employerFunds.getOrDefault("shop-assistant",0),SELL_PRICE,get(t,HIVE)>0,HIVE_PRICE,get(t,HONEY),HONEY_CAPACITY,get(t,BEE_WATER)==day,RetailLife.honeySpace(t),HONEY_PRICE,get(t,CELLAR)>0,CELLAR_PRICE,get(t,WINE_DUE),get(t,WINE_READY)>0,RetailLife.cellarSpace(t,"grape-juice"),RetailLife.cellarSpace(t,"wine"),JUICE_PRICE,WINE_PRICE);
    }
}
