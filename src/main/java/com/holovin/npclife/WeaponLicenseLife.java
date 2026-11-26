package com.holovin.npclife;

/** Fictional game permit, not a representation of actual weapons laws. */
final class WeaponLicenseLife {
    private WeaponLicenseLife(){}
    static final int FEE=10, SPORT_PRICE=80, VALID_DAYS=30;
    private static String key(TownLife.Town t,String name){return "combat:"+t.player.incarnation+":license:"+name;}
    private static int get(TownLife.Town t,String name){return t.industryStocks.getOrDefault(key(t,name),0);}
    private static void put(TownLife.Town t,String name,int value){t.industryStocks.put(key(t,name),value);}
    private static boolean ready(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0
                &&!t.player.cycling&&!IntercomLife.insideVisit(t,s.day,s.minute);
    }
    private static boolean eligible(TownLife.Town t,WorldState s){
        var j=JusticeLife.view(t,s);
        return j.convictions()==0&&j.assaults()==0&&JusticeLife.discharges(t)==0;
    }
    static boolean valid(TownLife.Town t,WorldState s){
        return get(t,"expires")>s.day&&eligible(t,s);
    }
    private static boolean canApply(TownLife.Town t,WorldState s){
        return ready(t,s)&&eligible(t,s)&&!valid(t,s)&&t.player.place==TownLife.Place.OFFICE
                &&s.minute>=480&&s.minute+10<=1020&&t.player.money>=FEE;
    }
    private static boolean canBuySport(TownLife.Town t,WorldState s){
        return ready(t,s)&&valid(t,s)&&get(t,"sport")==0&&t.player.place==TownLife.Place.MARKET
                &&s.minute>=480&&s.minute+2<=1200&&t.player.money>=SPORT_PRICE;
    }
    record View(boolean valid,int daysRemaining,boolean canApply,boolean ownsSportWeapon,boolean canBuySport,int practiceShots){}
    static View view(TownLife.Town t,WorldState s){
        return new View(valid(t,s),valid(t,s)?get(t,"expires")-s.day:0,canApply(t,s),get(t,"sport")>0,canBuySport(t,s),get(t,"practice-shots"));
    }
    static String apply(TownLife.Town t,WorldState s){
        if(!canApply(t,s))throw new IllegalArgumentException("Weapon license application unavailable.");
        int expiry=Math.addExact(s.day,VALID_DAYS);
        int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),FEE);
        t.player.money-=FEE;t.employerFunds.put("shop-assistant",funds);
        put(t,"expires",expiry);
        return "Weapon license issued.";
    }
    static void requireLicense(TownLife.Town t,WorldState s){
        if(!valid(t,s))throw new IllegalArgumentException("A valid weapon license is required.");
    }
    static String buySport(TownLife.Town t,WorldState s){
        requireLicense(t,s);
        if(!canBuySport(t,s))throw new IllegalArgumentException("Registered sporting weapon purchase unavailable.");
        int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),SPORT_PRICE);
        t.player.money-=SPORT_PRICE;t.employerFunds.put("shop-assistant",funds);
        put(t,"sport",1);
        return "Registered sporting weapon purchased.";
    }
    static void requireSport(TownLife.Town t,WorldState s){
        if(!ready(t,s))throw new IllegalArgumentException("Shooting is unavailable right now.");
        requireLicense(t,s);
        if(get(t,"sport")==0)throw new IllegalArgumentException("No registered sporting weapon.");
    }
    static String practice(TownLife.Town t,WorldState s){
        requireSport(t,s);
        if(t.player.place!=TownLife.Place.WORKSHOP||s.minute<480||s.minute+2>1080)
            throw new IllegalArgumentException("Visit the training counter from 08:00 to 18:00.");
        put(t,"practice-shots",Math.addExact(get(t,"practice-shots"),1));
        return "Practice shot recorded.";
    }
    static void revoke(TownLife.Town t){
        if(get(t,"expires")>0)put(t,"expires",0);
        if(get(t,"sport")>0)put(t,"sport",0);
    }
}
