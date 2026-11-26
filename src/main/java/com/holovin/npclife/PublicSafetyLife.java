package com.holovin.npclife;

/** Short-lived public alarm only for residents present in the affected scene. */
final class PublicSafetyLife {
    private PublicSafetyLife(){}
    private static String key(TownLife.Citizen c){return "combat:alarm:"+c.id;}
    private static long now(int day,int minute){return ((long)day-1)*1440+minute;}
    static boolean alarmed(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        return t.industryStocks.getOrDefault(key(c),0)>now(day,minute);
    }
    record View(boolean localAlert){}
    static View view(TownLife.Town t,WorldState s){
        return new View(t.industryStocks.getOrDefault("combat:public-alert:"+t.player.place.name(),0)>now(s.day,s.minute));
    }
    static void alarm(TownLife.Town t,WorldState s){
        int until=Math.toIntExact(now(s.day,s.minute)+60);
        t.industryStocks.merge("combat:public-alert:"+t.player.place.name(),until,Math::max);
        ArchiveLife.record(t,"public-alert:"+s.day+":"+s.minute,s.day,s.minute,"Public safety",
                "Police received a report of a dangerous incident in "+t.player.place.label+".");
        for(var c:PlayerLife.visible(t)){
            if(!PoliceLife.staff(c))t.industryStocks.merge(key(c),until,Math::max);
        }
    }
}
