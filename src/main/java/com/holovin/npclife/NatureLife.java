package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small, conserved natural resource stocks. Sources are public world facts,
 * not items secretly created for a player or knowledge granted to NPCs.
 */
final class NatureLife {
    private NatureLife() { }

    enum Kind { WATER, FOOD }

    static final class Source {
        final String id;
        final TownLife.Place place;
        final String label;
        final Kind kind;
        final int capacity;
        final int dailyReplenishment;
        final int healthRiskPercent;
        int stock;

        Source(String id, TownLife.Place place, String label, Kind kind,
               int capacity, int dailyReplenishment, int healthRiskPercent) {
            this.id=id;
            this.place=place;
            this.label=label;
            this.kind=kind;
            this.capacity=capacity;
            this.dailyReplenishment=dailyReplenishment;
            this.healthRiskPercent=healthRiskPercent;
            this.stock=capacity;
        }

        Source copy() {
            Source result=new Source(id,place,label,kind,capacity,
                    dailyReplenishment,healthRiskPercent);
            result.stock=stock;
            return result;
        }
    }

    static void ensureSources(TownLife.Town town) {
        // Quantities represent immediately accessible harvest, not the entire river volume.
        add(town,new Source("river-water",TownLife.Place.RIVER,"flowing river water",
                Kind.WATER,120,40,10));
        // No magical puddle refill: later weather simulation will replenish this source.
        add(town,new Source("puddle-water",TownLife.Place.PARK,"stagnant puddle water",
                Kind.WATER,5,0,50));
        add(town,new Source("wild-berries",TownLife.Place.WOODS,"wild berries",
                Kind.FOOD,16,2,0));
    }

    private static void add(TownLife.Town town,Source source) {
        town.naturalResources.putIfAbsent(source.id,source);
    }

    /** Replenish only at midnight, capped at finite carrying capacity. */
    static void tick(TownLife.Town town,int minute) {
        if(minute!=0)return;
        for(Source source:town.naturalResources.values())
            source.stock=Math.min(source.capacity,source.stock+source.dailyReplenishment);
    }

    static Map<String,Source> available(TownLife.Town town) {
        Map<String,Source> result=new LinkedHashMap<>();
        if(town.player.travelRemaining>0)return result;
        for(var source:town.naturalResources.values())
            if(source.place==town.player.place)result.put(source.id,source);
        return result;
    }

    private static Source accessible(TownLife.Town town,String id,Kind kind) {
        PlayerLife.requireAlive(town.player);
        if(town.player.travelRemaining>0)
            throw new IllegalArgumentException("Finish traveling before gathering resources.");
        Source source=town.naturalResources.get(id);
        if(source==null || source.kind!=kind)
            throw new IllegalArgumentException("Unknown natural resource: "+id+". Use sources.");
        if(source.place!=town.player.place)
            throw new IllegalArgumentException("Travel to "+source.place.label+" to reach "+source.label+".");
        if(source.stock<1)
            throw new IllegalArgumentException(source.label+" is depleted. The world must replenish it.");
        return source;
    }

    /** Consume exactly one shared unit, never from an NPC's private property. */
    static String drink(TownLife.Town town,String id,int day,int minute) {
        Source source=accessible(town,id,Kind.WATER);
        source.stock--;
        town.player.thirst=Math.max(0,town.player.thirst-40);

        // A deterministic exposure event, not a clinical probability model.
        boolean unhealthy=Math.floorMod((long)day*127+minute*17L+id.hashCode(),100)
                <source.healthRiskPercent;
        if(unhealthy)town.player.health=Math.max(0,town.player.health-4);
        return "You drank "+source.label+". "+
                (unhealthy?"It made you feel unwell (-4 health).":
                        "No immediate illness was observed; untreated water is not guaranteed safe.");
    }

    static String forage(TownLife.Town town,String id) {
        Source source=accessible(town,id,Kind.FOOD);
        source.stock--;
        town.player.hunger=Math.max(0,town.player.hunger-25);
        return "You gathered and ate "+source.label+" (-25 hunger).";
    }
}
