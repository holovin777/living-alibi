package com.holovin.npclife;

import java.util.Set;

/** Private home activity, separate from travel, employment and existing physiology. */
final class HomeRoutineLife {
    private HomeRoutineLife(){}
    private static String key(TownLife.Citizen c,String f){return "home-routine:"+c.homeUnit+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static int cleanliness(TownLife.Town t,TownLife.Citizen c){return 100-get(t,c,"dirt");}
    static boolean atHome(TownLife.Citizen c){
        return c.alive&&c.travelRemaining==0&&c.place==c.home&&c.homeUnit.equals(c.currentSpace());
    }
    static String activity(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!atHome(c))return "AWAY";
        if(Set.of("SLEEPING","SICK_LEAVE","RESTING_WHILE_SICK","WORKING").contains(c.activity))return c.activity;
        if(HouseholdLife.dinnerNow(t,c,day,minute))return "FAMILY_DINNER";
        if(c.age<18){
            if(c.age<6)return "PLAYING";
            int childSlot=Math.floorMod(minute/30+(int)(c.personalSeed%5)+day,4);
            if(minute>=900&&minute<1140&&childSlot<2)return "HOMEWORK";
            return childSlot==3?"READING":"PLAYING";
        }
        if(c.hunger>=45&&c.food>0)return "COOKING";
        if(c.thirst>=45&&c.water>0)return "DRINKING";
        if(c.fatigue>=75)return "RESTING";
        if(get(t,c,"dirt")>=Math.max(20,65-c.carefulness/2))return "CLEANING";
        int slot=Math.floorMod(minute/20+(int)(c.personalSeed%7)+day,6);
        return switch(slot){
            case 0 -> c.food>0?"COOKING":"READING";
            case 1 -> "READING";
            case 2 -> "WATCHING_TV";
            case 3 -> c.carefulness>=55?"CLEANING":"RESTING";
            case 4 -> c.curiosity>=55?"READING":"WATCHING_TV";
            default -> "RESTING";
        };
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(clock.minute%30!=0)return;
        var updated=new java.util.HashSet<String>();
        for(var c:t.people.values()){
            if(!atHome(c)||c.age<18||RebirthLife.linkedAdult(t,c)||!updated.add(c.homeUnit))continue;
            int dirt=Math.min(100,get(t,c,"dirt")+1);
            if(activity(t,c,clock.day,clock.minute).equals("CLEANING"))dirt=Math.max(0,dirt-12);
            put(t,c,"dirt",dirt);
        }
    }
}
