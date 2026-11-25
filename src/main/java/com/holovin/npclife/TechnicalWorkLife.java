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
        if(o.id.equals("water-pump"))UtilityLife.pumpRepaired(town);
        o.phase=Phase.COMPLETED;
        o.completedDay=day;
        town.player.lastTechnicalDay=day;
        town.player.fatigue=Math.min(100,town.player.fatigue+7);
        return "Completed "+o.title+": city infrastructure "+previous+" -> "+
                o.condition+"/100. Consumed stocked materials. Today's technical shift is now earned."+
                (o.id.equals("water-pump")?
                    " Park fountain now has "+town.cleanWater+" clean-water unit(s).":
                    " Better street lighting reduces night fatigue for people outdoors.");
    }

    // Linked citizens operate on the very same physical orders as the visitor.
    record LinkedOrder(String id,String title,String district,String phase,int condition,String materials,int wage,boolean canInspect,boolean canComplete){}
    static boolean linkedEngineer(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.linkedAdult(t,c)&&c.employed&&"Engineer".equals(c.occupation)
                &&EducationLife.get(t,c,"diploma-engineering")>0&&EducationLife.get(t,c,"technical")>=45;
    }
    static int linkedWage(Order o){return o.id.equals("water-pump")?38:62;}
    static boolean linkedPresent(TownLife.Town t,TownLife.Citizen c,Order o,int day,int minute,int duration){
        return linkedEngineer(t,c)&&RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==o.place
                &&!EmploymentLife.sick(c,day)&&minute>=480&&minute+duration<=1080&&c.fatigue<85;
    }
    static boolean materialsReady(TownLife.Town t,Order o){
        return t.technicalMaterials.getOrDefault(o.materialA,0)>=o.requiredA
                &&(o.materialB==null||t.technicalMaterials.getOrDefault(o.materialB,0)>=o.requiredB);
    }
    static List<LinkedOrder> linkedOrders(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!linkedEngineer(t,c))return List.of();
        return orders(t).stream().map(o->new LinkedOrder(o.id,o.title,o.place.name(),o.phase.name(),o.condition,
                o.requiredA+" "+o.materialA+" ("+t.technicalMaterials.getOrDefault(o.materialA,0)+" available)"
                +(o.materialB==null?"":" + "+o.requiredB+" "+o.materialB+" ("+t.technicalMaterials.getOrDefault(o.materialB,0)+" available)"),
                linkedWage(o),linkedPresent(t,c,o,day,minute,INSPECTION_MINUTES)&&o.phase==Phase.OPEN,
                linkedPresent(t,c,o,day,minute,COMPLETION_MINUTES)&&o.phase==Phase.INSPECTED&&materialsReady(t,o)
                &&EducationLife.get(t,c,"last-engineering-order")<day&&t.employerFunds.getOrDefault(o.jobId,0)>=linkedWage(o))).toList();
    }
    static String linkedInspect(TownLife.Town t,String id,int day,int minute){
        var c=RebirthLife.requireAdult(t);var o=order(t,id);
        if(linkedOrders(t,day,minute).stream().noneMatch(v->v.id().equals(o.id)&&v.canInspect()))
            throw new IllegalArgumentException("Inspection unavailable: qualification, location, hours or order state.");
        RebirthLife.travel(t,o.place,day);
        o.phase=Phase.INSPECTED;c.fatigue=Math.min(100,c.fatigue+3);
        return "Inspected "+o.title+"; condition "+o.condition+"/100.";
    }
    static String linkedComplete(TownLife.Town t,String id,int day,int minute){
        var c=RebirthLife.requireAdult(t);var o=order(t,id);
        if(linkedOrders(t,day,minute).stream().noneMatch(v->v.id().equals(o.id)&&v.canComplete()))
            throw new IllegalArgumentException("Repair unavailable: inspect first, check stock, hours and payroll.");
        int wage=linkedWage(o),wallet=Math.addExact(c.money,wage);
        RebirthLife.travel(t,o.place,day);
        t.technicalMaterials.merge(o.materialA,-o.requiredA,Integer::sum);
        if(o.materialB!=null)t.technicalMaterials.merge(o.materialB,-o.requiredB,Integer::sum);
        int previous=o.condition;
        o.condition=Math.min(100,o.condition+(o.jobId.equals("engineer")?40:55));
        o.phase=Phase.COMPLETED;o.completedDay=day;
        if(o.id.equals("water-pump"))UtilityLife.pumpRepaired(t);
        t.employerFunds.merge(o.jobId,-wage,Integer::sum);c.money=wallet;
        EducationLife.set(t,c,"last-engineering-order",day);
        EducationLife.learn(t,c,"technical",2);
        c.fatigue=Math.min(100,c.fatigue+7);
        return "Completed "+o.title+": "+previous+" -> "+o.condition+"/100; earned EUR "+wage+".";
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
