package com.holovin.npclife;

/** An owner-operated carrier. Its ledger persists with the freight manifest. */
final class LogisticsLife {
    private LogisticsLife() {}
    static final int REGISTRATION=20, TRUCK_PRICE=220, RELOCATION=8;
    static final String OWNER="logistics:owner", CASH="logistics:cash", TRUCK="logistics:truck-place";
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{OWNER,CASH,TRUCK})t.industryStocks.putIfAbsent(key,0);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(key,value);}
    static boolean yours(TownLife.Town t){return get(t,OWNER)>0 && get(t,OWNER)==t.player.incarnation;}
    static boolean hasTruck(TownLife.Town t){return get(t,TRUCK)>0;}
    static TownLife.Place truckPlace(TownLife.Town t){return hasTruck(t)?TownLife.Place.values()[get(t,TRUCK)-1]:null;}
    static void park(TownLife.Town t,TownLife.Place place){set(t,TRUCK,place.ordinal()+1);}
    static void requireOwner(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(!yours(t))throw new IllegalArgumentException("Register or acquire the logistics company first.");
    }
    static void office(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(t.player.place!=TownLife.Place.WORKSHOP)throw new IllegalArgumentException("Visit the workshop office.");
        if(FreightLife.active(t))throw new IllegalArgumentException("Complete the active freight delivery first.");
    }
    static void spend(TownLife.Town t,int amount) {
        if(amount<0 || get(t,CASH)<amount)throw new IllegalArgumentException("Insufficient company funds.");
        set(t,CASH,get(t,CASH)-amount);
    }
    static void receive(TownLife.Town t,int amount){set(t,CASH,Math.addExact(get(t,CASH),amount));}
    static String manage(TownLife.Town t,String action) {
        office(t);
        var p=t.player;
        if("register".equals(action)) {
            if(yours(t))throw new IllegalArgumentException("You already own this company.");
            int previous=get(t,OWNER);
            if(previous>0&&!t.pastLives.containsKey(previous))throw new IllegalArgumentException("The previous owner is still alive.");
            if(p.money<REGISTRATION)throw new IllegalArgumentException("Need 20 EUR to register.");
            p.money-=REGISTRATION;
            t.employerFunds.merge("city-maintenance",REGISTRATION,Math::addExact);
            set(t,OWNER,p.incarnation);
            return "Registered or acquired the carrier. Invest in its separate account.";
        }
        requireOwner(t);
        return switch(action) {
            case "invest-25","invest-100" -> {
                int amount=action.equals("invest-25")?25:100;
                if(p.money<amount)throw new IllegalArgumentException("Insufficient personal savings.");
                p.money-=amount;receive(t,amount);
                yield "Invested "+amount+" EUR.";
            }
            case "withdraw-25" -> {
                spend(t,25);p.money=Math.addExact(p.money,25);
                yield "Withdrew 25 EUR from company reserves.";
            }
            case "buy-truck" -> {
                if(hasTruck(t))throw new IllegalArgumentException("The company already has a truck.");
                spend(t,TRUCK_PRICE);
                t.employerFunds.merge("technician",TRUCK_PRICE,Math::addExact);
                park(t,TownLife.Place.WORKSHOP);
                yield "Bought the company truck for 220 EUR.";
            }
            default -> throw new IllegalArgumentException("Unknown company action.");
        };
    }
    record Report(boolean registered,boolean yours,int cash,boolean truck,String truckPlace,int contractFee,int relocationFee) {}
    static Report report(TownLife.Town t) {
        var place=truckPlace(t);
        return new Report(get(t,OWNER)>0,yours(t),get(t,CASH),hasTruck(t),place==null?null:place.name(),FreightLife.CONTRACT_FEE,RELOCATION);
    }
}
