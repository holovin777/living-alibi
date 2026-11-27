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
    // One transitional lease per character, paid directly to the guesthouse.
    // The visitor contributes at most 10 EUR; the existing welfare fund pays the rest.
    static int housingContribution(TownLife.Town t){
        return Math.max(0,Math.min(10,t.player.money));
    }
    static int housingSubsidy(TownLife.Town t){
        return HousingLife.RENT-housingContribution(t);
    }
    static boolean canHousing(TownLife.Town t,WorldState s){
        return eligible(t,s)&&s.minute>=540&&s.minute+15<=720&&get(t,"stays")>0&&
                get(t,"housing")==0&&t.employerFunds.getOrDefault(SocialProtectionLife.ACCOUNT,0)>=housingSubsidy(t);
    }
    static String housing(TownLife.Town t,WorldState s){
        if(!canHousing(t,s))throw new IllegalArgumentException("Housing assistance is available 09:00–12:00 after a shelter stay, once per character, subject to welfare funds.");
        int contribution=housingContribution(t),subsidy=housingSubsidy(t);
        int balance=t.employerFunds.getOrDefault(SocialProtectionLife.ACCOUNT,0);
        int guesthouse=Math.addExact(t.employerFunds.getOrDefault("guesthouse",0),HousingLife.RENT);
        HousingLife.expire(t.player,s.day,s.minute);
        t.player.money-=contribution;
        t.employerFunds.put(SocialProtectionLife.ACCOUNT,balance-subsidy);
        t.employerFunds.put("guesthouse",guesthouse);
        t.player.roomUntil=HousingLife.now(s.day,s.minute)+15+HousingLife.TERM;
        set(t,"housing",1);
        return "Received a seven-day guest room lease. Visit the north residences.";
    }
    record Report(boolean canSleep,boolean canWash,int stays,int lastNight,boolean canHousing,
                  int housingContribution,int housingSubsidy,boolean housingUsed){}
    static Report report(TownLife.Town t,WorldState s){
        return new Report(canSleep(t,s),canWash(t,s),get(t,"stays"),get(t,"night"),canHousing(t,s),
                housingContribution(t),housingSubsidy(t),get(t,"housing")!=0);
    }
}
