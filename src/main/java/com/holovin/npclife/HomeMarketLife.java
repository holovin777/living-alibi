package com.holovin.npclife;

import java.util.List;

/** Finite municipal homes for linked adults, separate from visitor-owned rentals.
 * A purchase relocates an eligible household without duplicating its wallets.
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
        return LISTINGS.stream().filter(l->owner(t,l)==c.generationIndex).findFirst().orElse(null);
    }
    static boolean atCounter(TownLife.Citizen c,int minute){
        return RebirthLife.awakePresent(c)&&c.place==TownLife.Place.OFFICE&&minute>=540&&minute<1020;
    }
    static boolean householdReady(TownLife.Town t,TownLife.Citizen c){
        var p=RebirthLife.partner(t,c);
        return p==null||!RebirthLife.together(c,p)||(RebirthLife.meeting(c,p)&&RebirthLife.willing(t,c,p));
    }
    record Offer(String id,String name,String district,int price,int resale,boolean available,boolean owned,boolean canBuy,boolean canSell){}
    static List<Offer> offers(TownLife.Town t,int minute){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c))return List.of();
        boolean ready=atCounter(c,minute)&&householdReady(t,c);
        var current=owned(t,c);
        return LISTINGS.stream().map(l->new Offer(l.id(),l.name(),l.district().label,l.price(),l.resale(),
                owner(t,l)==0,owner(t,l)==c.generationIndex,
                ready&&current==null&&owner(t,l)==0&&c.money>=l.price()+RESERVE,
                ready&&owner(t,l)==c.generationIndex&&t.employerFunds.getOrDefault("city-maintenance",0)>=l.resale())).toList();
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
            t.industryStocks.put(key(l),c.generationIndex);
            relocate(t,c,l.district(),unit(l));
            return "Purchased a new home. Your household can now move there.";
        }
        if(!offer.canSell())throw new IllegalArgumentException("The home cannot be sold right now.");
        int next=Math.addExact(c.money,l.resale());
        t.employerFunds.put("city-maintenance",city-l.resale());c.money=next;
        relocate(t,c,c.home,TownLife.privateUnit(c.id));
        t.industryStocks.put(key(l),0);
        return "Sold the home and returned to a standard private residence.";
    }
    /** Return a deceased owner's asset to the city when no surviving household
     * remains; otherwise pass it to the first eligible adult co-resident. */
    static void estate(TownLife.Town t,TownLife.Citizen c){
        var l=owned(t,c);
        if(l==null)return;
        var heir=t.people.values().stream().filter(x->x!=c&&x.alive&&x.age>=18&&x.generationIndex>0&&
            x.homeUnit.equals(unit(l))&&owned(t,x)==null&&
            (DemographyLife.get(t,c,"partner")==x.generationIndex||
             DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex))
            .sorted(java.util.Comparator.comparingInt(x->x.generationIndex)).findFirst().orElse(null);
        if(heir!=null)t.industryStocks.put(key(l),heir.generationIndex);
        else {
            // Keep any remaining occupant's private shelter; never evict them
            // just because the previous owner died.
            for(var x:t.people.values())if(x.alive&&unit(l).equals(x.homeUnit))
                RentalLife.moveHome(t,x,x.home,TownLife.privateUnit(x.id));
            t.industryStocks.put(key(l),0);
        }
    }
}
