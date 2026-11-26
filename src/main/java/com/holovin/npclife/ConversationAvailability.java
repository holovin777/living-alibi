package com.holovin.npclife;

import java.util.Set;

/** NPCs may spend a little time speaking, but the player cannot reserve their day. */
final class ConversationAvailability {
    private ConversationAvailability(){}
    private static String key(TownLife.Town t,TownLife.Citizen c,String field){
        return "dialogue:patience:"+t.player.incarnation+":"+c.id+":"+field;
    }
    private static int get(TownLife.Town t,TownLife.Citizen c,String field){
        return t.industryStocks.getOrDefault(key(t,c,field),0);
    }
    private static void put(TownLife.Town t,TownLife.Citizen c,String field,int n){
        t.industryStocks.put(key(t,c,field),n);
    }
    private static int tick(int day,int minute){return Math.addExact(Math.multiplyExact(day-1,1440),minute)+1;}
    static boolean duty(TownLife.Citizen c,int day,int minute){
        return PoliceLife.scheduled(c,day,minute)||Set.of("WORKING","STUDYING","JOB_SEARCH","SHOPPING").contains(c.activity);
    }
    static boolean urgent(TownLife.Citizen c){
        return c.health<45||c.hunger>=85||c.thirst>=85||c.fatigue>=85||
                Set.of("SICK_LEAVE","RESTING_WHILE_SICK","SLEEPING","TRAVELING").contains(c.activity);
    }
    static int minutes(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(urgent(c))return 2;
        if(duty(c,day,minute))return PoliceLife.staff(c)?4:5;
        var contact=t.player.contacts.get(c.id);
        return contact!=null&&contact.affinity>=30&&contact.trust>=70?18:
                8+Math.floorMod(c.sociability,5);
    }
    static int turns(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(urgent(c))return 1;
        if(duty(c,day,minute))return 2;
        return minutes(t,c,day,minute)>=18?8:5;
    }
    private static boolean fresh(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        int last=get(t,c,"last");
        return last==0||tick(day,minute)-last>=15;
    }
    static boolean allowed(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(fresh(t,c,day,minute))return true;
        return get(t,c,"turns")<turns(t,c,day,minute)&&
                tick(day,minute)-get(t,c,"start")<minutes(t,c,day,minute);
    }
    static boolean record(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        int now=tick(day,minute);
        if(fresh(t,c,day,minute)){
            put(t,c,"start",now);put(t,c,"turns",0);
        }
        put(t,c,"last",now);
        put(t,c,"turns",get(t,c,"turns")+1);
        return get(t,c,"turns")>=turns(t,c,day,minute)||
                now-get(t,c,"start")+1>=minutes(t,c,day,minute);
    }
    static String farewell(TownLife.Citizen c,int day,int minute,String language){
        boolean it="it".equals(language);
        if(urgent(c))return it?"Scusami, devo occuparmi di me. Ci sentiamo più tardi.":"Sorry, I need to take care of myself. Talk later.";
        if(PoliceLife.scheduled(c,day,minute))return it?"Scusami, devo continuare il mio turno. Buona giornata.":"Sorry, I have to get back to my shift. Have a good day.";
        if(duty(c,day,minute))return it?"Scusami, ho delle cose da fare. A dopo!":"Sorry, I have things to do. See you later!";
        return it?"Ora devo andare. Ci vediamo un'altra volta!":"I have to go now. See you another time!";
    }
}
