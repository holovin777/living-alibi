package com.holovin.npclife;

import java.util.List;

/** Finite municipal inventory, with no per-owner home count limit.
 * The first home can relocate a household; later purchases are investments.
 */
final class HomeMarketLife {
    private HomeMarketLife(){}
    static final int RESERVE=50;
    record Listing(String id,String name,TownLife.Place district,int price,int resale){}
    static final List<Listing> LISTINGS=List.of(
        new Listing("tiglio","Casa del Tiglio",TownLife.Place.NORTH_RESIDENCES,240,180),
        new Listing("rosa","Casa della Rosa",TownLife.Place.SOUTH_RESIDENCES,360,270),
        new Listing("olmo","Casa dell'Olmo",TownLife.Place.SOUTH_RESIDENCES,520,390));
    static String key(Listing l){return "home-market:"+l.id()+":owner";}
    static String unit(Listing l){return "unit:home-market-"+l.id();}
    static int owner(TownLife.Town t,Listing l){return t.industryStocks.getOrDefault(key(l),0);}
    static Listing owned(TownLife.Town t,TownLife.Citizen c){
        var all=portfolio(t,c);
        return all.stream().filter(l->unit(l).equals(c.homeUnit)).findFirst().orElse(all.isEmpty()?null:all.getFirst());
    }
    static List<Listing> portfolio(TownLife.Town t,TownLife.Citizen c){
        if(c==null||c.generationIndex<=0)return List.of();
        return LISTINGS.stream().filter(l->owner(t,l)==c.generationIndex).toList();
    }
    static String debtKey(Listing l){return "home-market:"+l.id()+":arrears";}
    static int arrears(TownLife.Town t,Listing l){return t.industryStocks.getOrDefault(debtKey(l),0);}
    static int weeklyUpkeep(Listing l){return Math.max(1,l.price()/40);}
    /** Outstanding costs follow the property. No automated sale or seizure exists. */
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=360||t.industryStocks.getOrDefault("home-market:last-day",0)>=day)return;
        t.industryStocks.put("home-market:last-day",day);
        for(var l:LISTINGS){
            var c=DemographyLife.byIndex(t,owner(t,l));
            if(c==null||!c.alive)continue;
            int debt=arrears(t,l);
            // Ordinary housing bills already cover the primary residence.
            if(!unit(l).equals(c.homeUnit)&&Math.floorMod(day-1,7)==0)
                debt=Math.addExact(debt,weeklyUpkeep(l));
            int paid=Math.min(debt,Math.max(0,c.money-RESERVE));
            int city=Math.addExact(MunicipalLife.balance(t,MunicipalLife.CITY),paid);
            c.money-=paid;t.employerFunds.put(MunicipalLife.CITY,city);
            t.industryStocks.put(debtKey(l),debt-paid);
        }
    }
    static boolean atCounter(TownLife.Citizen c,int minute){
        return RebirthLife.awakePresent(c)&&c.place==TownLife.Place.OFFICE&&minute>=540&&minute<1020;
    }
    static boolean householdReady(TownLife.Town t,TownLife.Citizen c){
        var p=RebirthLife.partner(t,c);
        return p==null||!RebirthLife.together(c,p)||(RebirthLife.meeting(c,p)&&RebirthLife.willing(t,c,p));
    }
    record Offer(String id,String name,String district,int price,int resale,boolean available,boolean owned,boolean canBuy,boolean canSell,int arrears,int weeklyUpkeep){}
    static List<Offer> offers(TownLife.Town t,int minute){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c))return List.of();
        boolean ready=atCounter(c,minute);
        var current=owned(t,c);
        return LISTINGS.stream().map(l->new Offer(l.id(),l.name(),l.district().label,l.price(),l.resale(),
                owner(t,l)==0,owner(t,l)==c.generationIndex,
                ready&&(current!=null||householdReady(t,c))&&owner(t,l)==0&&c.money>=l.price()+RESERVE,
                ready&&owner(t,l)==c.generationIndex&&(!unit(l).equals(c.homeUnit)||householdReady(t,c))
                    &&t.employerFunds.getOrDefault("city-maintenance",0)>=Math.max(0,l.resale()-arrears(t,l))
                    &&c.money>=Math.max(0,arrears(t,l)-l.resale()),
                arrears(t,l),weeklyUpkeep(l))).toList();
    }
    private static void relocate(TownLife.Town t,TownLife.Citizen c,TownLife.Place district,String target){
        String old=c.homeUnit;
        var p=RebirthLife.partner(t,c);
        var members=t.people.values().stream().filter(x->x!=c&&x.alive&&x.homeUnit.equals(old)&&x.home==c.home&&
            (x==p||(DemographyLife.child(x)&&(DemographyLife.get(t,x,"parent-a")==c.generationIndex||
            DemographyLife.get(t,x,"parent-b")==c.generationIndex||
            (p!=null&&(DemographyLife.get(t,x,"parent-a")==p.generationIndex||DemographyLife.get(t,x,"parent-b")==p.generationIndex)))))).toList();
        RentalLife.moveHome(t,c,district,target);
        for(var member:members)RentalLife.moveHome(t,member,district,target);
    }
    static String act(TownLife.Town t,String action,int minute){
        var c=RebirthLife.requireAdult(t);
        var offer=offers(t,minute).stream().filter(o->action.equals("buy-"+o.id())||action.equals("sell-"+o.id())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Unknown home listing."));
        var l=LISTINGS.stream().filter(x->x.id().equals(offer.id())).findFirst().orElseThrow();
        int city=t.employerFunds.getOrDefault("city-maintenance",0);
        if(action.startsWith("buy-")){
            if(!offer.canBuy())throw new IllegalArgumentException("Visit the property counter with sufficient funds and household agreement.");
            int next=Math.addExact(city,l.price());
            c.money-=l.price();t.employerFunds.put("city-maintenance",next);
            boolean first=portfolio(t,c).isEmpty();
            t.industryStocks.put(key(l),c.generationIndex);
            if(first){
                relocate(t,c,l.district(),unit(l));
                return "Purchased a new home. Your household can now move there.";
            }
            return "Purchased an additional property. Your household remains at its existing home.";
        }
        if(!offer.canSell())throw new IllegalArgumentException("The home cannot be sold right now.");
        int net=l.resale()-arrears(t,l);
        int next=Math.addExact(c.money,net);
        t.employerFunds.put("city-maintenance",Math.subtractExact(city,net));c.money=next;
        if(unit(l).equals(c.homeUnit))relocate(t,c,c.home,TownLife.privateUnit(c.id));
        t.industryStocks.put(key(l),0);t.industryStocks.put(debtKey(l),0);
        return "Sold the property and settled its outstanding upkeep.";
    }
    /** Return a deceased owner's asset to the city when no surviving household
     * remains; otherwise pass it to the first eligible adult co-resident. */
    static void estate(TownLife.Town t,TownLife.Citizen c){
        for(var l:portfolio(t,c)){
            var heir=t.people.values().stream().filter(x->x!=c&&x.alive&&x.generationIndex>0&&
                (DemographyLife.get(t,c,"partner")==x.generationIndex||
                 DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex))
                .sorted(java.util.Comparator.comparing((TownLife.Citizen x)->!x.homeUnit.equals(unit(l)))
                    .thenComparingInt(x->-x.age).thenComparingInt(x->x.generationIndex)).findFirst().orElse(null);
            if(heir!=null)t.industryStocks.put(key(l),heir.generationIndex);
            else {
                for(var x:t.people.values())if(x.alive&&unit(l).equals(x.homeUnit))
                    RentalLife.moveHome(t,x,x.home,TownLife.privateUnit(x.id));
                t.industryStocks.put(key(l),0);t.industryStocks.put(debtKey(l),0);
            }
        }
    }
}
