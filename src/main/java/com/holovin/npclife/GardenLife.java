package com.holovin.npclife;

/** A single private vegetable bed; deterministic growth and finite inputs. */
final class GardenLife {
    private GardenLife() {}
    static final String PLANTED="garden:planted", GROWTH="garden:growth", WATERED="garden:watered-day", LAST="garden:last-tick";
    static final int PRICE=3, REQUIRED=3, YIELD=4;
    static final String GREENHOUSE="garden:greenhouse";
    static final int GREENHOUSE_PRICE=25;
    static final String COMPOSTER="garden:composter", SCRAPS="garden:scraps", COMPOST_DUE="garden:compost-due",
            COMPOST="garden:compost", FERTILIZED="garden:fertilized";
    static final int COMPOSTER_PRICE=10, SCRAP_CAPACITY=6, COMPOST_INPUT=3, COMPOST_DAYS=3, BONUS=2;
    static final String BARREL="garden:barrel", RAIN="garden:rain";
    static final int BARREL_PRICE=8, RAIN_CAPACITY=8, RAIN_DAILY=3;
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{PLANTED,GROWTH,WATERED,LAST,BARREL,RAIN,GREENHOUSE,COMPOSTER,SCRAPS,COMPOST_DUE,COMPOST,FERTILIZED})t.industryStocks.putIfAbsent(key,0);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(key,value);}
    static void clear(TownLife.Town t){for(String key:new String[]{PLANTED,GROWTH,WATERED,LAST,BARREL,RAIN,GREENHOUSE,COMPOSTER,SCRAPS,COMPOST_DUE,COMPOST,FERTILIZED})set(t,key,0);}
    static void present(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(!t.player.ownsHouse||t.player.place!=HousingLife.HOUSE_DISTRICT)
            throw new IllegalArgumentException("Buy Casa del Glicine and visit its garden.");
    }
    static boolean winter(int day){return CalendarLife.season(day)==CalendarLife.Season.WINTER;}
    static boolean covered(TownLife.Town t){return get(t,GREENHOUSE)>0;}
    static int harvestUnits(TownLife.Town t){return YIELD+(get(t,FERTILIZED)>0?BONUS:0);}
    static String cookWithCompost(TownLife.Town t,FoodLife.Recipe recipe,int day,int minute) {
        String result=FoodLife.cook(t.player,recipe,day,minute);
        if(t.player.ownsHouse && t.player.place==HousingLife.HOUSE_DISTRICT && get(t,COMPOSTER)>0
                && recipe.ingredients().keySet().stream().anyMatch(id->id.equals("vegetables")||OrchardLife.isFruit(id))
                && get(t,SCRAPS)<SCRAP_CAPACITY) {
            set(t,SCRAPS,get(t,SCRAPS)+1);
            result+=" Collected one unit of plant scraps.";
        }
        return result;
    }
    static String act(TownLife.Town t,String action,int day,int minute) {
        if("sell-vegetables".equals(action))return RetailLife.buyProduce(t,minute);
        if(action!=null&&action.startsWith("rental-"))return RentalLife.act(t,action,minute);
        if(action!=null&&action.startsWith("bank-"))return BankLife.act(t,action,day,minute);
        if(action!=null&&action.startsWith("stall-"))return MarketStallLife.act(t,action,day,minute);
        if(OrchardLife.handles(action))return OrchardLife.act(t,action,day,minute);
        if(java.util.Set.of("build-coop","buy-hen","feed-hens","water-hens","collect-eggs","sell-eggs").contains(action==null?"":action))
            return HenLife.act(t,action,day,minute);
        present(t);var p=t.player;
        return switch(action==null?"":action) {
            case "plant" -> {
                if(get(t,PLANTED)>0||(winter(day)&&!covered(t)))throw new IllegalArgumentException("Use an empty bed; winter planting requires a greenhouse.");
                if(p.money<PRICE)throw new IllegalArgumentException("Need 3 EUR for seeds.");
                p.money-=PRICE;t.employerFunds.merge(FarmLife.ACCOUNT,PRICE,Math::addExact);
                set(t,PLANTED,1);set(t,GROWTH,0);set(t,WATERED,0);
                yield "Planted vegetables. Water on dry days.";
            }
            case "build-composter" -> {
                if(get(t,COMPOSTER)>0)throw new IllegalArgumentException("You already have a composter.");
                if(p.money<COMPOSTER_PRICE)throw new IllegalArgumentException("Need 10 EUR for a composter.");
                int supplierBalance=Math.addExact(t.employerFunds.getOrDefault("technician",0),COMPOSTER_PRICE);
                p.money-=COMPOSTER_PRICE;t.employerFunds.put("technician",supplierBalance);
                set(t,COMPOSTER,1);
                yield "Installed a composter. Plant scraps from home cooking will be collected here.";
            }
            case "start-compost" -> {
                if(get(t,COMPOSTER)==0||get(t,SCRAPS)<COMPOST_INPUT||get(t,COMPOST_DUE)>0||get(t,COMPOST)>0)
                    throw new IllegalArgumentException("Need a composter, three scraps and an empty compost chamber.");
                int due=Math.addExact(day,COMPOST_DAYS);
                set(t,SCRAPS,get(t,SCRAPS)-COMPOST_INPUT);set(t,COMPOST_DUE,due);
                yield "Started composting. Ready at 18:00 on day "+due+".";
            }
            case "fertilize" -> {
                if(get(t,COMPOSTER)==0||get(t,COMPOST)<1||get(t,PLANTED)==0||get(t,GROWTH)>=REQUIRED||get(t,FERTILIZED)>0)
                    throw new IllegalArgumentException("Need mature compost and a growing, unfertilized bed.");
                set(t,COMPOST,0);set(t,FERTILIZED,1);
                yield "Applied one compost unit. This crop will yield two extra vegetables.";
            }
            case "build-greenhouse" -> {
                if(covered(t))throw new IllegalArgumentException("You already have a greenhouse.");
                if(p.money<GREENHOUSE_PRICE)throw new IllegalArgumentException("Need 25 EUR for the greenhouse.");
                int supplierBalance=Math.addExact(t.employerFunds.getOrDefault("technician",0),GREENHOUSE_PRICE);
                p.money-=GREENHOUSE_PRICE;t.employerFunds.put("technician",supplierBalance);
                set(t,GREENHOUSE,1);
                yield "Installed a greenhouse over the existing vegetable bed. Water it manually in every season.";
            }
            case "buy-barrel" -> {
                if(get(t,BARREL)>0)throw new IllegalArgumentException("You already have a rain barrel.");
                if(p.money<BARREL_PRICE)throw new IllegalArgumentException("Need 8 EUR for a rain barrel.");
                p.money-=BARREL_PRICE;t.employerFunds.merge("technician",BARREL_PRICE,Math::addExact);
                set(t,BARREL,1);set(t,RAIN,0);
                yield "Installed an eight-unit rain barrel.";
            }
            case "water","water-rain" -> {
                if(get(t,PLANTED)==0||get(t,GROWTH)>=REQUIRED||(winter(day)&&!covered(t)))
                    throw new IllegalArgumentException("No growing crop to water.");
                if(minute>=1080||get(t,WATERED)==day||(!covered(t)&&WeatherLife.at(day,minute).raining()))
                    throw new IllegalArgumentException("Water once before 18:00; uncovered beds receive rain automatically.");
                if("water-rain".equals(action)) {
                    if(get(t,BARREL)==0||get(t,RAIN)<1)throw new IllegalArgumentException("The rain barrel is empty.");
                    set(t,RAIN,get(t,RAIN)-1);
                } else {
                    if(p.bottledWater<1)throw new IllegalArgumentException("Bring one water unit in your bag.");
                    FoodLife.change(p,"water",false,-1);
                }
                set(t,WATERED,day);
                yield "Watered the garden with one unit of "+("water-rain".equals(action)?"rainwater.":"bottled water.");
            }
            case "harvest" -> {
                if(get(t,PLANTED)==0||get(t,GROWTH)<REQUIRED)
                    throw new IllegalArgumentException("Vegetables are not ripe yet.");
                int amount=harvestUnits(t);
                if(FoodLife.total(p,true)+amount>24)
                    throw new IllegalArgumentException("Make room for "+amount+" vegetables in your cupboard.");
                FoodLife.change(p,"vegetables",true,amount);
                set(t,PLANTED,0);set(t,GROWTH,0);set(t,WATERED,0);set(t,FERTILIZED,0);
                yield "Harvested "+amount+" vegetable units into your home cupboard.";
            }
            default -> throw new IllegalArgumentException("Unknown garden action.");
        };
    }
    static void tick(TownLife.Town t,int day,int minute) {
        if(minute!=1080||get(t,LAST)>=day)return;
        set(t,LAST,day);
        if(!t.player.ownsHouse)return;
        if(get(t,COMPOSTER)>0&&get(t,COMPOST_DUE)>0&&get(t,COMPOST_DUE)<=day) {
            set(t,COMPOST_DUE,0);set(t,COMPOST,1);
        }
        boolean rain=WeatherLife.at(day,minute).raining();
        if(rain&&get(t,BARREL)>0)set(t,RAIN,Math.min(RAIN_CAPACITY,get(t,RAIN)+RAIN_DAILY));
        if(get(t,PLANTED)==0||get(t,GROWTH)>=REQUIRED||(winter(day)&&!covered(t)))return;
        if(get(t,WATERED)==day||(!covered(t)&&rain))
            set(t,GROWTH,get(t,GROWTH)+1);
    }
    record Report(boolean planted,int growth,int required,boolean wateredToday,boolean rainy,boolean winter,int seedPrice,int harvestUnits,boolean barrel,int rainwater,int rainCapacity,int barrelPrice,int sellPrice,int marketFree,int marketBudget,boolean greenhouse,int greenhousePrice,boolean composter,int composterPrice,int scraps,int compostDueDay,int compost,boolean fertilized) {}
    static Report report(TownLife.Town t,int day,int minute) {
        return new Report(get(t,PLANTED)>0,get(t,GROWTH),REQUIRED,get(t,WATERED)==day,
                WeatherLife.at(day,minute).raining(),winter(day),PRICE,harvestUnits(t),get(t,BARREL)>0,get(t,RAIN),RAIN_CAPACITY,BARREL_PRICE,RetailLife.PRODUCE_PRICE,RetailLife.produceSpace(t),t.employerFunds.getOrDefault("shop-assistant",0),covered(t),GREENHOUSE_PRICE,get(t,COMPOSTER)>0,COMPOSTER_PRICE,get(t,SCRAPS),get(t,COMPOST_DUE),get(t,COMPOST),get(t,FERTILIZED)>0);
    }
}
