package com.holovin.npclife;

import java.util.List;

/** Linked adult household tasks: real food/water consumption and prepaid
 * incremental appliance electricity costs, separate from existing fixed bills. */
final class DomesticLife {
    private DomesticLife(){}
    static final int LAUNDRY_PRICE=60;
    static int get(TownLife.Town t,TownLife.Citizen c,String key){return EducationLife.get(t,c,"domestic-"+key);}
    static void set(TownLife.Town t,TownLife.Citizen c,String key,int n){EducationLife.set(t,c,"domestic-"+key,n);}
    static int cleanliness(TownLife.Town t,TownLife.Citizen c){return 100-Math.min(100,get(t,c,"home-dirt"));}
    static int clothes(TownLife.Town t,TownLife.Citizen c){return 100-Math.min(100,get(t,c,"clothes-dirt"));}
    static boolean washer(TownLife.Town t,TownLife.Citizen c){return InteriorLife.installed(t,c,"laundry");}
    record View(int cleanliness,int clothes,int electricityUses,int waterUsed){}
    static View view(TownLife.Town t,TownLife.Citizen c){
        return new View(cleanliness(t,c),clothes(t,c),get(t,c,"power"),get(t,c,"water"));
    }
    static void tick(TownLife.Town t,WorldState clock){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c)||clock.minute!=360||get(t,c,"last-day")>=clock.day)return;
        set(t,c,"last-day",clock.day);
        set(t,c,"home-dirt",Math.min(100,get(t,c,"home-dirt")+8));
        set(t,c,"clothes-dirt",Math.min(100,get(t,c,"clothes-dirt")+15));
    }
    static void actions(TownLife.Town t,TownLife.Citizen c,List<String> actions){
        if(!RebirthLife.atHome(c))return;
        if(InteriorLife.installed(t,c,"kitchen")&&c.food>0&&c.hunger>0&&c.money>=1)actions.add("microwave");
        if(cleanliness(t,c)<100&&c.water>=2)actions.add("clean");
        if(washer(t,c)&&clothes(t,c)<100&&c.water>=2&&c.money>=1)actions.add("laundry");
    }
    private static void electricity(TownLife.Town t,TownLife.Citizen c){
        int funds=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),1);
        c.money--;t.employerFunds.put("city-maintenance",funds);
        set(t,c,"power",Math.addExact(get(t,c,"power"),1));
    }
    private static void water(TownLife.Town t,TownLife.Citizen c){
        c.water--;set(t,c,"water",Math.addExact(get(t,c,"water"),1));
    }
    static RebirthLife.HomeAction act(TownLife.Town t,TownLife.Citizen c,String action){
        var options=new java.util.ArrayList<String>();actions(t,c,options);
        if(!options.contains(action))throw new IllegalArgumentException("This domestic task is not available.");
        return switch(action){
            case "microwave" -> {
                electricity(t,c);c.food--;c.hunger=Math.max(0,c.hunger-50);
                yield new RebirthLife.HomeAction(5,"Heated a meal using one electricity credit.");
            }
            case "clean" -> {
                water(t,c);set(t,c,"home-dirt",0);
                yield new RebirthLife.HomeAction(30,"Cleaned your home using one water unit.");
            }
            case "laundry" -> {
                electricity(t,c);water(t,c);set(t,c,"clothes-dirt",0);
                yield new RebirthLife.HomeAction(45,"Washed clothes using water and electricity.");
            }
            default -> throw new IllegalArgumentException("Unknown domestic task.");
        };
    }
}
