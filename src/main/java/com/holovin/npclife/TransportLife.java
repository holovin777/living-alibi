package com.holovin.npclife;

/** First transport tier: a portable used bicycle, no fuel or parking state. */
final class TransportLife {
    private TransportLife() { }
    static final int BICYCLE_PRICE=45;
    static void ready(PlayerLife.Player p) {
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0)throw new IllegalArgumentException("Finish traveling first.");
    }
    static String buyBicycle(TownLife.Town town,int minute) {
        var p=town.player;
        ready(p);
        if(p.place!=TownLife.Place.WORKSHOP)
            throw new IllegalArgumentException("Visit the mechanical workshop.");
        if(minute<480 || minute>=1080)throw new IllegalArgumentException("Workshop opens 08:00–18:00.");
        if(p.ownsBicycle)throw new IllegalArgumentException("You already own a bicycle.");
        if(p.money<BICYCLE_PRICE)throw new IllegalArgumentException("Need 45 EUR.");
        p.money-=BICYCLE_PRICE;
        town.employerFunds.merge("technician",BICYCLE_PRICE,Math::addExact);
        p.ownsBicycle=true;
        return "Bought a used bicycle for 45 EUR. Select cycling to ride.";
    }
    static String mode(PlayerLife.Player p,String mode) {
        ready(p);
        if(!"walk".equals(mode)&&!"cycle".equals(mode))
            throw new IllegalArgumentException("Choose walk or cycle.");
        if("cycle".equals(mode)&&!p.ownsBicycle)
            throw new IllegalArgumentException("Buy a bicycle first.");
        p.cycling="cycle".equals(mode);
        return p.cycling?"Cycling selected.":"Walking selected.";
    }
}
