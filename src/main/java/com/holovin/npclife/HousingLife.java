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
            p.storedBread=0;p.storedWater=0;p.pantryItems.clear();
        }
    }
    static void requireStay(PlayerLife.Player p,int day,int minute,int duration) {
        present(p);
        if(!active(p,day,minute) || p.roomUntil<now(day,minute)+duration)
            throw new IllegalArgumentException("A valid room lease for the entire action is required.");
    }
    static String transfer(PlayerLife.Player p,String item,boolean deposit,int day,int minute) {
        requireStay(p,day,minute,1);
        RetailLife.product(item);
        if(FoodLife.quantity(p,item,!deposit)<1)
            throw new IllegalArgumentException("No item to transfer.");
        if(FoodLife.total(p,deposit)>=(deposit?24:12))
            throw new IllegalArgumentException(deposit?"Cupboard is full.":"Your bag is full.");
        FoodLife.change(p,item,!deposit,-1);
        FoodLife.change(p,item,deposit,1);
        return (deposit?"Stored ":"Retrieved ")+item+".";
    }
    static String cook(PlayerLife.Player p,int day,int minute) {
        return FoodLife.cook(p,FoodLife.recipe("bread-soup"),day,minute);
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
