package com.holovin.npclife;

/** Fictional game permit, not a representation of actual weapons laws. */
final class WeaponLicenseLife {
    private WeaponLicenseLife(){}
    static final int FEE=10, SPORT_PRICE=80, VALID_DAYS=30, CAPACITY=5, REFILL_PRICE=3;
    private static String key(TownLife.Town t,String name){return "combat:"+t.player.incarnation+":license:"+name;}
    private static int get(TownLife.Town t,String name){return t.industryStocks.getOrDefault(key(t,name),0);}
    private static void put(TownLife.Town t,String name,int value){t.industryStocks.put(key(t,name),value);}
    private static boolean ready(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0
                &&!t.player.cycling&&!IntercomLife.insideVisit(t,s.day,s.minute);
    }
    private static boolean eligible(TownLife.Town t,WorldState s){
        var j=JusticeLife.view(t,s);
        return j.convictions()==0&&j.assaults()==0&&JusticeLife.discharges(t)==0&&JusticeLife.thefts(t)==0;
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
    record View(boolean valid,int daysRemaining,boolean canApply,boolean ownsSportWeapon,boolean canBuySport,int practiceShots,int rounds,int capacity,boolean canRefill,int practiceSessions){}
    static View view(TownLife.Town t,WorldState s){
        return new View(valid(t,s),valid(t,s)?get(t,"expires")-s.day:0,canApply(t,s),get(t,"sport")>0,canBuySport(t,s),get(t,"practice-shots"),rounds(t),CAPACITY,canRefill(t,s),get(t,"practice-shots")/CAPACITY);
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
        put(t,"sport",1);put(t,"rounds",CAPACITY);
        return "Registered sporting weapon purchased.";
    }
    static void requireSport(TownLife.Town t,WorldState s){
        if(!ready(t,s))throw new IllegalArgumentException("Shooting is unavailable right now.");
        requireLicense(t,s);
        if(get(t,"sport")==0)throw new IllegalArgumentException("No registered sporting weapon.");
    }
    // Older registered items had no round count: initialize their stock logically once.
    static int rounds(TownLife.Town t){
        if(get(t,"sport")==0)return 0;
        return t.industryStocks.getOrDefault(key(t,"rounds"),CAPACITY);
    }
    static void spendRound(TownLife.Town t){
        int count=rounds(t);
        if(count<=0)throw new IllegalArgumentException("No game rounds remaining.");
        put(t,"rounds",count-1);
    }
    private static boolean canRefill(TownLife.Town t,WorldState s){
        return ready(t,s)&&valid(t,s)&&get(t,"sport")>0&&rounds(t)==0&&t.player.money>=REFILL_PRICE
                &&t.player.place==TownLife.Place.WORKSHOP&&s.minute>=480&&s.minute+2<=1080;
    }
    static String refill(TownLife.Town t,WorldState s){
        if(!canRefill(t,s))throw new IllegalArgumentException("Training refill unavailable.");
        int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),REFILL_PRICE);
        t.player.money-=REFILL_PRICE;t.employerFunds.put("shop-assistant",funds);
        put(t,"rounds",CAPACITY);
        return "Training supply replenished.";
    }
    static String practice(TownLife.Town t,WorldState s){
        requireSport(t,s);
        if(t.player.place!=TownLife.Place.WORKSHOP||s.minute<480||s.minute+2>1080)
            throw new IllegalArgumentException("Visit the training counter from 08:00 to 18:00.");
        int shots=Math.addExact(get(t,"practice-shots"),1);
        spendRound(t);
        put(t,"practice-shots",shots);
        return "Practice shot recorded.";
    }
    static void revoke(TownLife.Town t){
        if(get(t,"expires")>0)put(t,"expires",0);
        if(get(t,"sport")>0)put(t,"sport",0);
        put(t,"rounds",0);
    }
}
