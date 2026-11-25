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
    static String key(Crop c,String field){return "orchard:"+c.id()+":"+field;}
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int amount){t.industryStocks.put(key,amount);}
    static List<String> keys(){
        var keys=new java.util.ArrayList<String>(List.of(WATER,LAST));
        for(var c:CROPS)for(String f:List.of("planted","age","fruit","year"))keys.add(key(c,f));
        return keys;
    }
    static void ensure(TownLife.Town t){for(String k:keys())t.industryStocks.putIfAbsent(k,0);}
    static void clear(TownLife.Town t){for(String k:keys())set(t,k,0);}
    static boolean isFruit(String id){return CROPS.stream().anyMatch(c->c.id().equals(id));}
    static boolean handles(String action){return action!=null&&(action.startsWith("orchard-")||action.startsWith("sell-fruit-"));}
    static String act(TownLife.Town t,String action,int day,int minute) {
        if(action.startsWith("sell-fruit-"))return RetailLife.buyFruit(t,action.substring("sell-fruit-".length()),minute);
        GardenLife.present(t);
        var p=t.player;
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
                if(FoodLife.total(p,true)+YIELD>24)throw new IllegalArgumentException("Make room for three fruit units.");
                FoodLife.change(p,c.id(),true,YIELD);
                set(t,key(c,"fruit"),0);set(t,key(c,"year"),CalendarLife.year(day));
                return "Harvested three units of "+c.id()+" into the cupboard.";
            }
        }
        throw new IllegalArgumentException("Unknown orchard action.");
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=1080||get(t,LAST)>=day)return;
        set(t,LAST,day);
        if(!t.player.ownsHouse)return;
        var season=CalendarLife.season(day);
        boolean wet=get(t,WATER)==day||WeatherLife.at(day,minute).raining();
        for(var c:CROPS){
            if(get(t,key(c,"planted"))==0)continue;
            if(season!=c.season())set(t,key(c,"fruit"),0);
            if(season==CalendarLife.Season.WINTER||!wet)continue;
            int age=get(t,key(c,"age"));
            if(age<MATURE)set(t,key(c,"age"),age+1);
            else if(season==c.season()&&get(t,key(c,"year"))<CalendarLife.year(day))
                set(t,key(c,"fruit"),Math.min(RIPE,get(t,key(c,"fruit"))+1));
        }
    }
    record Plant(String id,String label,int price,String season,boolean planted,int age,int fruit,boolean ready,boolean harvestedThisYear,int marketFree) {}
    record Report(List<Plant> plants,boolean wateredToday,boolean rainy,boolean winter,int marketBudget,int sellPrice) {}
    static Report report(TownLife.Town t,int day,int minute){
        return new Report(CROPS.stream().map(c->new Plant(c.id(),c.label(),c.price(),c.season().name(),
                get(t,key(c,"planted"))>0,get(t,key(c,"age")),get(t,key(c,"fruit")),
                get(t,key(c,"planted"))>0&&get(t,key(c,"fruit"))>=RIPE&&c.season()==CalendarLife.season(day)&&get(t,key(c,"year"))<CalendarLife.year(day),
                get(t,key(c,"year"))==CalendarLife.year(day),RetailLife.fruitSpace(t,c.id()))).toList(),
                get(t,WATER)==day,WeatherLife.at(day,minute).raining(),GardenLife.winter(day),t.employerFunds.getOrDefault("shop-assistant",0),SELL_PRICE);
    }
}
