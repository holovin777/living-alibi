package com.holovin.npclife;

import java.util.List;

/**
 * City utilities depend on the same physical assets repaired by skilled workers.
 * A small public water tank transfers units from finite river stock; poor
 * streetlights add an observable night-time fatigue cost in lit public streets.
 * This is a fictional game model, not real potable-water treatment guidance.
 */
final class UtilityLife {
    private UtilityLife() { }

    static final int FOUNTAIN_MIN_CONDITION=60;
    static final int LIGHTS_MIN_CONDITION=70;
    static final int TANK_CAPACITY=24;
    static final int DAILY_PUMP_UNITS=4;
    static final int REPAIR_PRIME_UNITS=6;

    record Services(int pumpCondition,boolean fountainWorking,int cleanWater,
                    int cleanWaterCapacity,int lightingCondition,
                    boolean lightsWorking) { }

    static Services status(TownLife.Town town) {
        int pump=town.technicalOrders.get("water-pump").condition;
        int lights=town.technicalOrders.get("street-lights").condition;
        return new Services(pump,pump>=FOUNTAIN_MIN_CONDITION,
                town.cleanWater,TANK_CAPACITY,lights,lights>=LIGHTS_MIN_CONDITION);
    }

    private static int transferRiverWater(TownLife.Town town,int amount) {
        if(!status(town).fountainWorking() || amount<=0)return 0;
        var source=town.naturalResources.get("river-water");
        if(source==null)return 0;
        int taken=Math.min(amount,Math.min(source.stock,TANK_CAPACITY-town.cleanWater));
        if(taken<1)return 0;
        source.stock-=taken;
        town.cleanWater+=taken;
        return taken;
    }

    /** Commissioning the repaired pump primes the fountain immediately. */
    static void pumpRepaired(TownLife.Town town) {
        transferRiverWater(town,REPAIR_PRIME_UNITS);
    }

    /** No additional water is created on reload or on duplicate midnight calls. */
    static void tick(TownLife.Town town,WorldState clock,
                     List<TownLife.Event> events,
                     List<TownLife.Observation> observations) {
        if(clock.minute==0 && clock.day>town.lastWaterPumpDay) {
            town.lastWaterPumpDay=clock.day;
            transferRiverWater(town,DAILY_PUMP_UNITS);
        }
        if(clock.minute%60==0) {
            for(var c:town.people.values()) {
                if(!c.alive || c.id.equals("marco"))continue;
                if(c.travelRemaining==0 && c.place==c.home)continue; // Private indoor home
                if(nightExposure(town,c.place,clock.minute)) {
                    c.fatigue=Math.min(100,c.fatigue+1);
                }
            }
            if(town.player.alive && town.player.travelRemaining==0 &&
                    nightExposure(town,town.player.place,clock.minute))
                town.player.fatigue=Math.min(100,town.player.fatigue+1);
        }
        for(var c:town.people.values()) {
            if(!c.alive || c.id.equals("marco") || c.water>=2 ||
                    c.travelRemaining>0 || c.place!=TownLife.Place.PARK)
                continue;
            if(takeFountainUnit(town)) {
                c.water++;
                OpinionLife.fountain(town,c,clock.day,true);
                TownLife.publish(town,clock,c.id,c.place,
                        c.name+" refilled a bottle from the working park fountain.",
                        events,observations);
            } else if(clock.minute%60==0)OpinionLife.fountain(town,c,clock.day,false);
        }
    }

    static boolean nightExposure(TownLife.Town town,TownLife.Place place,int minute) {
        if(minute>=360 && minute<1200)return false; // 20:00–06:00
        if(status(town).lightsWorking())return false;
        return switch(place) {
            case PARK, MARKET, OFFICE, WORKSHOP, SCHOOL, LIBRARY,
                 NORTH_RESIDENCES, SOUTH_RESIDENCES -> true;
            default -> false; // indoor/private or remote wilderness
        };
    }

    private static boolean takeFountainUnit(TownLife.Town town) {
        if(!status(town).fountainWorking() || town.cleanWater<=0)return false;
        town.cleanWater--;
        return true;
    }

    /** Public drinking is voluntary, limited by real stored filtered water. */
    static String drinkAtFountain(TownLife.Town town) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0 || p.place!=TownLife.Place.PARK)
            throw new IllegalArgumentException("Visit the park fountain before drinking.");
        if(!status(town).fountainWorking())
            throw new IllegalArgumentException("The park pump is broken; no safe tap water.");
        if(!takeFountainUnit(town))
            throw new IllegalArgumentException("The park fountain has run out of clean water.");
        p.thirst=Math.max(0,p.thirst-35);
        return "Drank one unit of public fountain water; clean-water tank has "+
                town.cleanWater+" unit(s) left.";
    }
}
