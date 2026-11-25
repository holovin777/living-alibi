package com.holovin.npclife;

/** A single private vegetable bed; deterministic growth and finite inputs. */
final class GardenLife {
    private GardenLife() {}
    static final String PLANTED="garden:planted", GROWTH="garden:growth", WATERED="garden:watered-day", LAST="garden:last-tick";
    static final int PRICE=3, REQUIRED=3, YIELD=4;
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{PLANTED,GROWTH,WATERED,LAST})t.industryStocks.putIfAbsent(key,0);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(key,value);}
    static void clear(TownLife.Town t){for(String key:new String[]{PLANTED,GROWTH,WATERED,LAST})set(t,key,0);}
    static void present(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(!t.player.ownsHouse||t.player.place!=HousingLife.HOUSE_DISTRICT)
            throw new IllegalArgumentException("Buy Casa del Glicine and visit its garden.");
    }
    static boolean winter(int day){return CalendarLife.season(day)==CalendarLife.Season.WINTER;}
    static String act(TownLife.Town t,String action,int day,int minute) {
        present(t);var p=t.player;
        return switch(action==null?"":action) {
            case "plant" -> {
                if(get(t,PLANTED)>0||winter(day))throw new IllegalArgumentException("Use an empty bed outside winter.");
                if(p.money<PRICE)throw new IllegalArgumentException("Need 3 EUR for seeds.");
                p.money-=PRICE;t.employerFunds.merge(FarmLife.ACCOUNT,PRICE,Math::addExact);
                set(t,PLANTED,1);set(t,GROWTH,0);set(t,WATERED,0);
                yield "Planted vegetables. Water on dry days.";
            }
            case "water" -> {
                if(get(t,PLANTED)==0||get(t,GROWTH)>=REQUIRED||winter(day))
                    throw new IllegalArgumentException("No growing crop to water.");
                if(minute>=1080||get(t,WATERED)==day||WeatherLife.at(day,minute).raining())
                    throw new IllegalArgumentException("Water once on a dry day before 18:00.");
                if(p.bottledWater<1)throw new IllegalArgumentException("Bring one water unit in your bag.");
                FoodLife.change(p,"water",false,-1);set(t,WATERED,day);
                yield "Watered the garden with one unit from your bag.";
            }
            case "harvest" -> {
                if(get(t,PLANTED)==0||get(t,GROWTH)<REQUIRED)
                    throw new IllegalArgumentException("Vegetables are not ripe yet.");
                if(FoodLife.total(p,true)+YIELD>24)
                    throw new IllegalArgumentException("Make room for four vegetables in your cupboard.");
                FoodLife.change(p,"vegetables",true,YIELD);
                set(t,PLANTED,0);set(t,GROWTH,0);set(t,WATERED,0);
                yield "Harvested four vegetable units into your home cupboard.";
            }
            default -> throw new IllegalArgumentException("Unknown garden action.");
        };
    }
    static void tick(TownLife.Town t,int day,int minute) {
        if(minute!=1080||get(t,LAST)>=day)return;
        set(t,LAST,day);
        if(!t.player.ownsHouse||get(t,PLANTED)==0||get(t,GROWTH)>=REQUIRED||winter(day))return;
        if(get(t,WATERED)==day||WeatherLife.at(day,minute).raining())
            set(t,GROWTH,get(t,GROWTH)+1);
    }
    record Report(boolean planted,int growth,int required,boolean wateredToday,boolean rainy,boolean winter,int seedPrice,int harvestUnits) {}
    static Report report(TownLife.Town t,int day,int minute) {
        return new Report(get(t,PLANTED)>0,get(t,GROWTH),REQUIRED,get(t,WATERED)==day,
                WeatherLife.at(day,minute).raining(),winter(day),PRICE,YIELD);
    }
}
