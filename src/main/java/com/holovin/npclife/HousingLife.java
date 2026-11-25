package com.holovin.npclife;

/** A small prepaid guest room, not ownership of another resident's home. */
final class HousingLife {
    private HousingLife() { }
    static final int RENT=35;
    static final int TERM=7*1440;
    static final TownLife.Place DISTRICT=TownLife.Place.NORTH_RESIDENCES;

    static long now(int day,int minute) {return (day-1L)*1440+minute;}
    static boolean active(PlayerLife.Player p,int day,int minute) {
        return p.roomUntil>now(day,minute);
    }
    static void present(PlayerLife.Player p) {
        PlayerLife.requireAlive(p);
        if(p.place!=DISTRICT || p.travelRemaining>0)
            throw new IllegalArgumentException("Visit the north residences guesthouse.");
    }
    static String rent(TownLife.Town town,int day,int minute) {
        var p=town.player;
        present(p);
        long start=now(day,minute);
        if(p.roomUntil-start>1440)
            throw new IllegalArgumentException("Renew during the last 24 hours of your stay.");
        if(p.money<RENT)throw new IllegalArgumentException("Not enough money for rent.");
        p.money-=RENT;
        town.employerFunds.merge("guesthouse",RENT,Math::addExact);
        p.roomUntil=Math.max(start,p.roomUntil)+TERM;
        return "Guest room paid for seven game days. No automatic renewal.";
    }
    static PlayerLife.SleepResult sleep(PlayerLife.Player p,int hours,int day,int minute) {
        present(p);
        if(hours<1 || hours>12)throw new IllegalArgumentException("Sleep between 1 and 12 hours.");
        if(!active(p,day,minute) || p.roomUntil<now(day,minute)+hours*60L)
            throw new IllegalArgumentException("Rent or renew a room for the entire stay first.");
        return PlayerLife.sleepSheltered(p,hours);
    }
}
