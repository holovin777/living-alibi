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
    static void expire(PlayerLife.Player p,int day,int minute) {
        if(p.roomUntil>0 && !active(p,day,minute)) {
            // Guesthouse checkout clears perishables from the rented cupboard.
            p.storedBread=0;p.storedWater=0;
        }
    }
    static void requireStay(PlayerLife.Player p,int day,int minute,int duration) {
        present(p);
        if(!active(p,day,minute) || p.roomUntil<now(day,minute)+duration)
            throw new IllegalArgumentException("A valid room lease for the entire action is required.");
    }
    static String transfer(PlayerLife.Player p,String item,boolean deposit,int day,int minute) {
        requireStay(p,day,minute,1);
        if(!"bread".equals(item) && !"water".equals(item))
            throw new IllegalArgumentException("Unknown cupboard item.");
        boolean bread="bread".equals(item);
        int carried=bread?p.bread:p.bottledWater;
        int stored=bread?p.storedBread:p.storedWater;
        if(deposit) {
            if(carried<1)throw new IllegalArgumentException("No item in your bag.");
            if(p.storedBread+p.storedWater>=24)throw new IllegalArgumentException("Cupboard is full.");
        } else {
            if(stored<1)throw new IllegalArgumentException("No item in your cupboard.");
            if(p.bread+p.bottledWater>=12)throw new IllegalArgumentException("Your bag is full.");
        }
        int change=deposit?1:-1;
        if(bread){p.bread-=change;p.storedBread+=change;}
        else {p.bottledWater-=change;p.storedWater+=change;}
        return (deposit?"Stored ":"Retrieved ")+item+".";
    }
    static String cook(PlayerLife.Player p,int day,int minute) {
        requireStay(p,day,minute,15);
        if(p.hunger==0)throw new IllegalArgumentException("You are not hungry.");
        if(p.storedBread<1 || p.storedWater<1)
            throw new IllegalArgumentException("Store one bread and one bottle of water in your cupboard first.");
        p.storedBread--;p.storedWater--;
        p.hunger=Math.max(0,p.hunger-45);
        p.thirst=Math.max(0,p.thirst-20);
        return "Prepared and ate a simple bread soup.";
    }
    static String wash(PlayerLife.Player p,int day,int minute) {
        requireStay(p,day,minute,10);
        if(p.hygiene>=95)throw new IllegalArgumentException("You are already clean.");
        p.hygiene=100;
        return "Washed and refreshed.";
    }
    static String rent(TownLife.Town town,int day,int minute) {
        var p=town.player;
        present(p);
        long start=now(day,minute);
        if(p.roomUntil-start>1440)
            throw new IllegalArgumentException("Renew during the last 24 hours of your stay.");
        if(p.money<RENT)throw new IllegalArgumentException("Not enough money for rent.");
        expire(p,day,minute);
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
