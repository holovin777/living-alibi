package com.holovin.npclife;

/** First emergency bed for the visitor without accommodation. Uses the
 * established sheltered sleep model, with a per-night and hygiene limit. */
final class ShelterLife {
    private ShelterLife(){}
    static final int START=1200,END=1320,MAX_CASH=35;
    private static String key(TownLife.Town t,String f){return "shelter:"+t.player.incarnation+":"+f;}
    private static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(t,f),0);}
    private static void set(TownLife.Town t,String f,int v){t.industryStocks.put(key(t,f),v);}
    private static boolean eligible(TownLife.Town t,WorldState s){
        var p=t.player;
        return !RebirthLife.active(t)&&p.alive&&p.travelRemaining==0&&p.place==CaritasLife.PLACE&&
                !p.ownsHouse&&!HousingLife.active(p,s.day,s.minute)&&p.money<MAX_CASH;
    }
    static boolean canSleep(TownLife.Town t,WorldState s){
        return eligible(t,s)&&s.minute>=START&&s.minute<END&&get(t,"night")!=s.day;
    }
    static boolean canWash(TownLife.Town t,WorldState s){
        return eligible(t,s)&&s.minute>=1080&&s.minute+10<=1320&&t.player.hygiene<95&&get(t,"wash")!=s.day;
    }
    static PlayerLife.SleepResult sleep(TownLife.Town t,WorldState s){
        if(!canSleep(t,s))throw new IllegalArgumentException("The emergency bed is available only 20:00–22:00 for visitors without accommodation and less than 35 EUR.");
        var result=PlayerLife.sleepSheltered(t.player,8);
        set(t,"night",s.day);set(t,"stays",Math.addExact(get(t,"stays"),1));
        return result;
    }
    static String wash(TownLife.Town t,WorldState s){
        if(!canWash(t,s))throw new IllegalArgumentException("A free shower is available 18:00–22:00, once per day.");
        t.player.hygiene=100;set(t,"wash",s.day);
        return "Washed at the shelter.";
    }
    record Report(boolean canSleep,boolean canWash,int stays,int lastNight){}
    static Report report(TownLife.Town t,WorldState s){
        return new Report(canSleep(t,s),canWash(t,s),get(t,"stays"),get(t,"night"));
    }
}
