package com.holovin.npclife;

import java.util.Comparator;

/** Private medical follow-up; no diagnosis or patient identities in public notices. */
final class EmergencyLife {
    private EmergencyLife(){}
    private static String key(TownLife.Citizen c,String field){return "combat:medical:"+c.id+":"+field;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String field){return t.industryStocks.getOrDefault(key(c,field),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String field,int n){t.industryStocks.put(key(c,field),n);}
    private static int now(WorldState s){return Math.toIntExact(((long)s.day-1)*1440+s.minute+1);}
    static void request(TownLife.Town t,WorldState s,TownLife.Citizen c,int previousHealth){
        if(!c.alive||c.health>=previousHealth)return;
        put(t,c,"goal",Math.max(previousHealth,get(t,c,"goal")));
        if(get(t,c,"due")==0)put(t,c,"due",Math.addExact(now(s),20));
    }
    static boolean pending(TownLife.Town t,TownLife.Citizen c){return get(t,c,"due")>0;}
    static int completed(TownLife.Town t){return t.industryStocks.getOrDefault("combat:medical:completed",0);}
    static void tick(TownLife.Town t,WorldState s){
        int time=now(s);
        if(s.minute%10!=0)return;
        for(var c:t.people.values()){
            if(!pending(t,c))continue;
            if(!c.alive||c.health>=get(t,c,"goal")){put(t,c,"due",0);put(t,c,"goal",0);}
        }
        boolean staffed=t.people.values().stream().anyMatch(c->c.alive&&c.employed
                &&("Doctor".equals(c.occupation)||"Nurse".equals(c.occupation))
                &&c.presentAt(TownLife.Place.CLINIC)&&"WORKING".equals(c.activity));
        if(!staffed)return;
        var patient=t.people.values().stream().filter(c->c.alive&&pending(t,c)&&get(t,c,"due")<=time)
                .min(Comparator.comparingInt((TownLife.Citizen c)->c.health).thenComparing(c->c.id)).orElse(null);
        if(patient==null)return;
        patient.health=Math.min(get(t,patient,"goal"),Math.min(100,patient.health+8));
        if("marco".equals(patient.id))s.health=patient.health;
        if(patient.health>=get(t,patient,"goal")){
            put(t,patient,"due",0);put(t,patient,"goal",0);
            t.industryStocks.merge("combat:medical:completed",1,Math::addExact);
        }else put(t,patient,"due",Math.addExact(time,20));
    }
}
