package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Small, authoritative technical work cycle. A job title alone is not a task:
 * a trained employee must inspect a real city fault, consume real materials,
 * and improve infrastructure before collecting the day's technical wage.
 *
 * Units/health values are fictional game abstractions, not safety guidance.
 */
final class TechnicalWorkLife {
    private TechnicalWorkLife() { }

    enum Phase { OPEN, INSPECTED, COMPLETED }

    static final int INSPECTION_MINUTES=30;
    static final int COMPLETION_MINUTES=90;

    static final class Order {
        final String id,title,jobId,materialA,materialB;
        final TownLife.Place place;
        final int requiredA,requiredB;
        int condition;
        int completedDay;
        Phase phase=Phase.OPEN;

        Order(String id,String title,String jobId,TownLife.Place place,
              int condition,String materialA,int requiredA,String materialB,int requiredB) {
            this.id=id;this.title=title;this.jobId=jobId;this.place=place;
            this.condition=condition;
            this.materialA=materialA;this.requiredA=requiredA;
            this.materialB=materialB;this.requiredB=requiredB;
        }
        Order copy() {
            Order o=new Order(id,title,jobId,place,condition,materialA,requiredA,materialB,requiredB);
            o.completedDay=completedDay;o.phase=phase;
            return o;
        }
    }

    static void ensure(TownLife.Town town) {
        town.technicalMaterials.putIfAbsent("spare-parts",6);
        town.technicalMaterials.putIfAbsent("steel",4);
        town.technicalMaterials.putIfAbsent("copper",3);
        town.technicalOrders.putIfAbsent("water-pump",
                new Order("water-pump","Park water-pump maintenance",
                        "technician",TownLife.Place.WORKSHOP,45,
                        "spare-parts",2,null,0));
        town.technicalOrders.putIfAbsent("street-lights",
                new Order("street-lights","Public lighting efficiency upgrade",
                        "engineer",TownLife.Place.OFFICE,50,
                        "steel",1,"copper",1));
    }

    static List<Order> orders(TownLife.Town town) {
        return List.copyOf(town.technicalOrders.values());
    }

    private static Order order(TownLife.Town town,String id) {
        if(id==null)throw new IllegalArgumentException("Select a work order.");
        var found=town.technicalOrders.get(id.strip().toLowerCase(Locale.ROOT));
        if(found==null)throw new IllegalArgumentException("Unknown work order: "+id);
        return found;
    }

    private static void requireAvailable(TownLife.Town town,Order order,int minute,int duration) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0 || p.place!=order.place)
            throw new IllegalArgumentException("Work order requires presence at "+order.place.label+".");
        if(!order.jobId.equals(p.jobId))
            throw new IllegalArgumentException("Get hired as "+order.jobId+" before this work.");
        if(p.engineeringSkill<(order.jobId.equals("engineer")?8:3))
            throw new IllegalArgumentException("Insufficient engineering qualification.");
        if(minute<480 || minute+duration>1080)
            throw new IllegalArgumentException("Technical work is possible 08:00–18:00.");
    }

    static String inspect(TownLife.Town town,String id,int minute) {
        Order o=order(town,id);
        requireAvailable(town,o,minute,INSPECTION_MINUTES);
        if(o.phase!=Phase.OPEN)
            throw new IllegalArgumentException("This task is already inspected or complete.");
        o.phase=Phase.INSPECTED;
        town.player.fatigue=Math.min(100,town.player.fatigue+3);
        return "Inspected "+o.title+". Condition "+o.condition+
                "/100. Requires "+o.requiredA+" "+o.materialA+
                (o.materialB==null?"":" and "+o.requiredB+" "+o.materialB)+".";
    }

    static String complete(TownLife.Town town,String id,int day,int minute) {
        Order o=order(town,id);
        requireAvailable(town,o,minute,COMPLETION_MINUTES);
        if(o.phase!=Phase.INSPECTED)
            throw new IllegalArgumentException("Inspect this task before executing it.");
        if(town.player.lastTechnicalDay==day)
            throw new IllegalArgumentException("A technical work order was already completed today.");
        if(town.technicalMaterials.getOrDefault(o.materialA,0)<o.requiredA ||
                (o.materialB!=null &&
                 town.technicalMaterials.getOrDefault(o.materialB,0)<o.requiredB))
            throw new IllegalArgumentException("Insufficient physical materials in town stock.");
        town.technicalMaterials.put(o.materialA,
                town.technicalMaterials.get(o.materialA)-o.requiredA);
        if(o.materialB!=null)
            town.technicalMaterials.put(o.materialB,
                    town.technicalMaterials.get(o.materialB)-o.requiredB);
        int previous=o.condition;
        o.condition=Math.min(100,o.condition+(o.jobId.equals("engineer")?40:55));
        o.phase=Phase.COMPLETED;
        o.completedDay=day;
        town.player.lastTechnicalDay=day;
        town.player.fatigue=Math.min(100,town.player.fatigue+7);
        return "Completed "+o.title+": city infrastructure "+previous+" -> "+
                o.condition+"/100. Consumed stocked materials. Today's technical shift is now earned.";
    }

    /** Slow wear makes failures reappear; consumable supplies never regenerate for free. */
    static void tick(TownLife.Town town,int day,int minute) {
        if(minute!=0 || day<=1)return;
        for(Order o:town.technicalOrders.values()) {
            o.condition=Math.max(0,o.condition-(o.id.equals("water-pump")?2:1));
            if(o.phase==Phase.COMPLETED && o.condition<=65)
                o.phase=Phase.OPEN;
        }
    }

    static int totalMaterials(TownLife.Town town) {
        return town.technicalMaterials.values().stream().mapToInt(Integer::intValue).sum();
    }
}
