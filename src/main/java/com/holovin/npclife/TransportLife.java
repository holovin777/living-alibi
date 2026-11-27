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
    static boolean hasRack(TownLife.Place place) {
        return CityExpansionLife.outer(place)||place==TownLife.Place.PARK || place==TownLife.Place.WORKSHOP ||
                place==TownLife.Place.NORTH_RESIDENCES;
    }
    static String mode(PlayerLife.Player p,String mode) {
        ready(p);
        if("park".equals(mode)) {
            if(!p.ownsBicycle || p.bicycleParked!=null)
                throw new IllegalArgumentException("No bicycle with you to park.");
            if(!hasRack(p.place))throw new IllegalArgumentException("Find a bicycle rack first.");
            p.bicycleParked=p.place;p.cycling=false;
            return "Bicycle parked at "+p.place.label+".";
        }
        if("retrieve".equals(mode)) {
            if(!p.ownsBicycle || p.bicycleParked==null || p.bicycleParked!=p.place)
                throw new IllegalArgumentException("Return to the district where your bicycle is parked.");
            p.bicycleParked=null;p.cycling=false;
            return "Retrieved your bicycle.";
        }
        ready(p);
        if(!"walk".equals(mode)&&!"cycle".equals(mode))
            throw new IllegalArgumentException("Choose walk or cycle.");
        if("cycle".equals(mode)&&p.bicycleParked!=null)
            throw new IllegalArgumentException("Retrieve your parked bicycle first.");
        if("cycle".equals(mode)&&!p.ownsBicycle)
            throw new IllegalArgumentException("Buy a bicycle first.");
        p.cycling="cycle".equals(mode);
        return p.cycling?"Cycling selected.":"Walking selected.";
    }
}
