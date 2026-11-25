package com.holovin.npclife;

/** A small prepaid guest room, not ownership of another resident's home. */
final class HousingLife {
    private HousingLife() { }
    static final int RENT=35, HOUSE_PRICE=320;
    static final TownLife.Place HOUSE_DISTRICT=TownLife.Place.SOUTH_RESIDENCES;
    static final int TERM=7*1440;
    static final TownLife.Place DISTRICT=TownLife.Place.NORTH_RESIDENCES;

    static long now(int day,int minute) {return (day-1L)*1440+minute;}
    static boolean active(PlayerLife.Player p,int day,int minute) {
        return p.roomUntil>now(day,minute);
    }
    static void present(PlayerLife.Player p) {
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0 || (p.place!=DISTRICT && !(p.ownsHouse && p.place==HOUSE_DISTRICT)))
            throw new IllegalArgumentException("Visit your guesthouse or owned home.");
    }
    static void expire(PlayerLife.Player p,int day,int minute) {
        if(!p.ownsHouse && p.roomUntil>0 && !active(p,day,minute)) {
            // Guesthouse checkout clears perishables from the rented cupboard.
            p.storedBread=0;p.storedWater=0;p.pantryItems.clear();
        }
    }
    static void requireStay(PlayerLife.Player p,int day,int minute,int duration) {
        present(p);
        if(p.ownsHouse && p.place==HOUSE_DISTRICT)return;
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
        if(p.place!=DISTRICT || p.ownsHouse)throw new IllegalArgumentException("Guest room rental is for visitors without an owned home.");
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
    static String buyHouse(TownLife.Town town,int day,int minute) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.place!=HOUSE_DISTRICT || p.travelRemaining>0)throw new IllegalArgumentException("Visit the south residences property office.");
        if(p.ownsHouse)throw new IllegalArgumentException("You already own the house.");
        if(p.money<HOUSE_PRICE)throw new IllegalArgumentException("Need 320 EUR for the house.");
        expire(p,day,minute);
        p.money-=HOUSE_PRICE;
        town.employerFunds.merge("city-maintenance",HOUSE_PRICE,Math::addExact);
        p.ownsHouse=true;
        // The active rental cupboard moves into the purchased home, without duplication.
        // Prepaid rent is not refunded. Expired rental supplies have already been cleared.
        p.roomUntil=0;
        return "Bought Casa del Glicine for 320 EUR. Your pantry moved in; any guest lease ended without refund.";
    }

    static PlayerLife.SleepResult sleep(PlayerLife.Player p,int hours,int day,int minute) {
        present(p);
        if(hours<1 || hours>12)throw new IllegalArgumentException("Sleep between 1 and 12 hours.");
        requireStay(p,day,minute,hours*60);
        return PlayerLife.sleepSheltered(p,hours);
    }
}
