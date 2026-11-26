package com.holovin.npclife;

/** Short-lived public alarm only for residents present in the affected scene. */
final class PublicSafetyLife {
    private PublicSafetyLife(){}
    private static String key(TownLife.Citizen c){return "combat:alarm:"+c.id;}
    private static long now(int day,int minute){return ((long)day-1)*1440+minute;}
    static boolean alarmed(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        return t.industryStocks.getOrDefault(key(c),0)>now(day,minute);
    }
    static void alarm(TownLife.Town t,WorldState s){
        int until=Math.toIntExact(now(s.day,s.minute)+60);
        for(var c:PlayerLife.visible(t)){
            if(!PoliceLife.staff(c))t.industryStocks.merge(key(c),until,Math::max);
        }
    }
}
