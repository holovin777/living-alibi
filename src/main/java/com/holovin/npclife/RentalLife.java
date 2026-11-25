package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** One municipal apartment that can be bought and leased without replacing the player's home. */
final class RentalLife {
    private RentalLife() {}
    static final String OWNED="rental:owned", LISTED="rental:listed", TENANT="rental:tenant",
            FORMER_HOME="rental:former-home", UNTIL="rental:until", LAST="rental:last-day",
            REVENUE="rental:revenue";
    static final String UNIT="unit:appartamento-verdi";
    static final TownLife.Place DISTRICT=TownLife.Place.NORTH_RESIDENCES;
    static final int PRICE=180, RESALE=140, RENT=55, TERM=7, LIVING_RESERVE=50;
    static final List<String> KEYS=List.of(OWNED,LISTED,TENANT,FORMER_HOME,UNTIL,LAST,REVENUE);
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static void ensure(TownLife.Town t){for(String k:KEYS)t.industryStocks.putIfAbsent(k,0);}
    static TownLife.Citizen tenant(TownLife.Town t){
        int index=get(t,TENANT);
        if(index==0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==index).findFirst().orElse(null);
    }
    static boolean housed(TownLife.Town t,TownLife.Citizen c,int day){
        return c.generationIndex>0&&c.generationIndex==get(t,TENANT)&&day<get(t,UNTIL)&&UNIT.equals(c.homeUnit);
    }
    static void moveHome(TownLife.Town t,TownLife.Citizen c,TownLife.Place district,String unit){
        String old=c.homeUnit;
        for(var item:t.items.values())if(c.id.equals(item.ownerId)&&item.holderId==null&&!item.stolen&&old.equals(item.storedSpace))
            item.storedSpace=unit;
        c.home=district;c.homeUnit=unit;
    }
    static void endLease(TownLife.Town t){
        var c=tenant(t);
        int previous=get(t,FORMER_HOME);
        if(c!=null&&UNIT.equals(c.homeUnit)&&previous>0)
            moveHome(t,c,TownLife.Place.values()[previous-1],TownLife.privateUnit(c.id));
        set(t,TENANT,0);set(t,FORMER_HOME,0);set(t,UNTIL,0);
    }
    static void payRent(TownLife.Town t,TownLife.Citizen c){
        boolean owner=get(t,OWNED)>0&&t.player.alive;
        int recipient=Math.addExact(owner?t.player.money:t.employerFunds.getOrDefault("city-maintenance",0),RENT);
        int revenue=owner?Math.addExact(get(t,REVENUE),RENT):get(t,REVENUE);
        c.money-=RENT;
        if(owner)t.player.money=recipient;
        else t.employerFunds.put("city-maintenance",recipient);
        set(t,REVENUE,revenue);
    }
    static boolean affordable(TownLife.Citizen c){
        return c.alive&&c.generationIndex>0&&c.money>=RENT+LIVING_RESERVE&&c.unpaidBills<100;
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=360||get(t,LAST)>=day)return;
        set(t,LAST,day);
        var current=tenant(t);
        if(get(t,TENANT)>0){
            if(current==null||!current.alive){endLease(t);return;}
            if(day<get(t,UNTIL))return;
            if(get(t,LISTED)>0&&affordable(current)){
                int next=Math.addExact(day,TERM);
                payRent(t,current);set(t,UNTIL,next);
                return;
            }
            endLease(t);return;
        }
        if(get(t,OWNED)==0||get(t,LISTED)==0||!t.player.alive)return;
        // A voluntary, cheaper alternative to the existing housing bill. Prefer
        // a resident whose workplace is in the apartment's neighborhood.
        var candidate=t.people.values().stream()
                .filter(c->affordable(c)&&c.travelRemaining==0&&c.place==c.home&&
                        TownLife.privateUnit(c.id).equals(c.homeUnit))
                .sorted(Comparator.comparing((TownLife.Citizen c)->c.workplace!=DISTRICT)
                        .thenComparingInt(c->c.money).thenComparing(c->c.id))
                .findFirst().orElse(null);
        if(candidate==null)return;
        int until=Math.addExact(day,TERM);
        payRent(t,candidate);
        set(t,TENANT,candidate.generationIndex);set(t,FORMER_HOME,candidate.home.ordinal()+1);set(t,UNTIL,until);
        moveHome(t,candidate,DISTRICT,UNIT);
    }
    static String act(TownLife.Town t,String action,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.OFFICE||minute<540||minute>=1020)
            throw new IllegalArgumentException("Visit the property counter at Offices, 09:00–17:00.");
        if(action.equals("rental-buy")){
            if(get(t,OWNED)>0||p.money<PRICE)throw new IllegalArgumentException("Need 180 EUR and an unowned apartment.");
            int city=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),PRICE);
            p.money-=PRICE;t.employerFunds.put("city-maintenance",city);set(t,OWNED,1);
            return "Bought Appartamento Verdi.";
        }
        if(get(t,OWNED)==0)throw new IllegalArgumentException("Buy the apartment first.");
        return switch(action){
            case "rental-list" -> {set(t,LISTED,1);yield "Listed the apartment for 55 EUR per seven days.";}
            case "rental-unlist" -> {set(t,LISTED,0);yield "Stopped new leases and renewals; the prepaid lease remains valid.";}
            case "rental-sell" -> {
                int city=t.employerFunds.getOrDefault("city-maintenance",0);
                if(get(t,TENANT)>0||city<RESALE)throw new IllegalArgumentException("Sell only a vacant apartment when city funds are available.");
                int money=Math.addExact(p.money,RESALE);
                t.employerFunds.put("city-maintenance",city-RESALE);p.money=money;
                set(t,OWNED,0);set(t,LISTED,0);
                yield "Sold the vacant apartment for 140 EUR.";
            }
            default -> throw new IllegalArgumentException("Unknown property action.");
        };
    }
    /** Municipal succession honors an already paid tenancy, but disables renewal. */
    static void estate(TownLife.Town t){set(t,OWNED,0);set(t,LISTED,0);set(t,REVENUE,0);}
    record Report(boolean owned,boolean listed,boolean occupied,String tenantName,int paidUntilDay,int price,int resale,int weeklyRent,int revenue) {}
    static Report report(TownLife.Town t){
        var c=tenant(t);
        return new Report(get(t,OWNED)>0,get(t,LISTED)>0,c!=null&&c.alive,
                get(t,OWNED)>0&&c!=null&&c.alive?c.name:null,get(t,UNTIL),PRICE,RESALE,RENT,get(t,REVENUE));
    }
}
