package com.holovin.npclife;

import java.util.Comparator;

/** Finite donated supplies, real voluntary work and one parcel per day.
 * The library district is the first location; it is not an institution API. */
final class CaritasLife {
    private CaritasLife(){}
    static final TownLife.Place PLACE=TownLife.Place.LIBRARY;
    static final int START=720, END=840, CAPACITY=40;
    private static final String P="caritas:", DAY=P+"day", WORKER=P+"worker",
            BREAD=P+"bread", WATER=P+"water", SERVED=P+"served", DONATED=P+"donated";
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    private static String last(TownLife.Citizen c){return P+"recipient:"+c.id;}
    private static String visitor(TownLife.Town t){return P+"visitor:"+t.player.incarnation;}
    private static boolean available(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.generationIndex>0&&c.age>=18&&!c.employed&&c.money>=40&&
                c.sociability>=55&&c.home!=PLACE&&!CityExpansionLife.outer(c.home)&&
                !RebirthLife.linkedAdult(t,c)&&!EmploymentLife.sick(c,s.day)&&
                !NpcJusticeLife.confined(t,c,s)&&!PublicSafetyLife.alarmed(t,c,s.day,s.minute)&&
                !CondominiumLife.cleaningAssigned(t,c,s)&&
                c.health>=50&&c.hunger<80&&c.thirst<80&&c.fatigue<80;
    }
    static void prepare(TownLife.Town t,WorldState s){
        if(s.minute!=630||get(t,DAY)==s.day)return;
        set(t,DAY,s.day);set(t,WORKER,0);
        // A small modeled merchant donation from existing market inventory.
        for(String item:new String[]{"bread","water"}){
            String k=P+item;
            int stock=t.retailStock.getOrDefault(item,0);
            int amount=Math.min(4,Math.min(Math.max(0,stock-10),CAPACITY-get(t,k)));
            if(amount>0){
                t.retailStock.put(item,stock-amount);
                set(t,k,get(t,k)+amount);
                set(t,DONATED,Math.addExact(get(t,DONATED),amount));
            }
        }
        var worker=t.people.values().stream().filter(c->available(t,c,s)&&c.travelRemaining==0)
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->-c.sociability).thenComparing(c->c.id))
                .findFirst().orElse(null);
        if(worker!=null)set(t,WORKER,worker.generationIndex);
    }
    static boolean assigned(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return get(t,DAY)==s.day&&get(t,WORKER)>0&&get(t,WORKER)==c.generationIndex&&
                s.minute>=630&&s.minute<END&&available(t,c,s);
    }
    private static boolean open(TownLife.Town t,WorldState s){
        if(s.minute<START||s.minute>=END||get(t,DAY)!=s.day||get(t,WORKER)==0)return false;
        var c=DemographyLife.byIndex(t,get(t,WORKER));
        return c!=null&&assigned(t,c,s)&&c.place==PLACE&&c.travelRemaining==0&&
                "VOLUNTEERING".equals(c.activity)&&c.currentSpace().equals(TownLife.publicSpace(PLACE));
    }
    private static boolean stocked(TownLife.Town t){return get(t,BREAD)>0&&get(t,WATER)>0;}
    static boolean seekingMeal(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return s.minute>=660&&s.minute<END&&get(t,DAY)==s.day&&get(t,WORKER)>0&&stocked(t)&&
                c.alive&&c.generationIndex>0&&c.age>=18&&c.money<25&&!c.employed&&c.home!=PLACE&&
                (c.food<2||c.water<2)&&!RebirthLife.linkedAdult(t,c)&&!CityExpansionLife.outer(c.home)&&
                !EmploymentLife.sick(c,s.day)&&get(t,last(c))!=s.day;
    }
    private static void parcel(TownLife.Town t){
        set(t,BREAD,get(t,BREAD)-1);set(t,WATER,get(t,WATER)-1);
        set(t,SERVED,Math.addExact(get(t,SERVED),1));
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute%5!=0||!stocked(t)||!open(t,s))return;
        var recipient=t.people.values().stream()
                .filter(c->seekingMeal(t,c,s)&&c.place==PLACE&&c.travelRemaining==0&&
                        "SEEKING_MEAL".equals(c.activity)&&c.currentSpace().equals(TownLife.publicSpace(PLACE)))
                .min(Comparator.comparingInt((TownLife.Citizen c)->c.money).thenComparing(c->c.id)).orElse(null);
        if(recipient!=null){
            parcel(t);recipient.food++;recipient.water++;set(t,last(recipient),s.day);
        }
    }
    private static boolean present(TownLife.Town t){
        return !RebirthLife.active(t)&&t.player.alive&&t.player.travelRemaining==0&&t.player.place==PLACE;
    }
    private static boolean receive(TownLife.Town t,WorldState s){
        return present(t)&&open(t,s)&&stocked(t)&&t.player.money<25&&
                (t.player.bread==0||t.player.bottledWater==0)&&FoodLife.total(t.player,false)<=10&&
                get(t,visitor(t))!=s.day;
    }
    private static boolean donate(TownLife.Town t,WorldState s,String item){
        return present(t)&&open(t,s)&&get(t,P+item)<CAPACITY&&FoodLife.quantity(t.player,item,false)>0;
    }
    static String act(TownLife.Town t,WorldState s,String action){
        if("caritas-receive".equals(action)){
            if(!receive(t,s))throw new IllegalArgumentException("A free parcel is not available to you right now.");
            parcel(t);FoodLife.change(t.player,"bread",false,1);FoodLife.change(t.player,"water",false,1);
            set(t,visitor(t),s.day);
            return "Received a free parcel of bread and water.";
        }
        String item=switch(action){case "caritas-donate-bread"->"bread";case "caritas-donate-water"->"water";default->throw new IllegalArgumentException("Unknown charity action.");};
        if(!donate(t,s,item))throw new IllegalArgumentException("Visit the open charity with a spare item and available storage.");
        FoodLife.change(t.player,item,false,-1);set(t,P+item,get(t,P+item)+1);
        set(t,DONATED,Math.addExact(get(t,DONATED),1));
        return "Donated "+item+" to the community kitchen.";
    }
    record Report(int bread,int water,int served,int donated,boolean open,boolean canReceive,boolean canDonateBread,boolean canDonateWater){}
    static Report report(TownLife.Town t,WorldState s){
        return new Report(get(t,BREAD),get(t,WATER),get(t,SERVED),get(t,DONATED),open(t,s),
                receive(t,s),donate(t,s,"bread"),donate(t,s,"water"));
    }
}
