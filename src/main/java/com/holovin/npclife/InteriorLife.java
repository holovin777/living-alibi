package com.holovin.npclife;

import java.util.List;

/** Portable room furnishings bought from the existing shop; no duplicate wallets. */
final class InteriorLife {
    private InteriorLife(){}
    record Package(String id,int price){}
    static final List<Package> PACKAGES=List.of(new Package("kitchen",90),new Package("bedroom",75),new Package("children",65),new Package("laundry",DomesticLife.LAUNDRY_PRICE));
    static boolean installed(TownLife.Town t,TownLife.Citizen c,String id){return EducationLife.get(t,c,"interior-"+id)>0;}
    static boolean hasChild(TownLife.Town t,TownLife.Citizen c){
        var partner=RebirthLife.partner(t,c);
        return t.people.values().stream().anyMatch(x->x.alive&&DemographyLife.child(x)&&RebirthLife.together(c,x)&&
            (DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex||
             (partner!=null&&(DemographyLife.get(t,x,"parent-a")==partner.generationIndex||DemographyLife.get(t,x,"parent-b")==partner.generationIndex))));
    }
    record Offer(String id,int price,boolean installed,boolean canBuy){}
    static List<Offer> offers(TownLife.Town t,TownLife.Citizen c){
        return PACKAGES.stream().map(p->new Offer(p.id(),p.price(),installed(t,c,p.id()),
            RebirthLife.atHome(c)&&!installed(t,c,p.id())&&c.money>=p.price()+50&&(!p.id().equals("children")||hasChild(t,c)))).toList();
    }
    static String style(TownLife.Citizen c){
        return HomeMarketLife.LISTINGS.stream().filter(l->HomeMarketLife.unit(l).equals(c.homeUnit)).map(HomeMarketLife.Listing::id).findFirst().orElse("standard");
    }
    static RebirthLife.HomeAction purchase(TownLife.Town t,TownLife.Citizen c,String id){
        var offer=offers(t,c).stream().filter(x->x.id().equals(id)&&x.canBuy()).findFirst().orElseThrow(()->new IllegalArgumentException("This furnishing package is unavailable."));
        int shop=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),offer.price());
        c.money-=offer.price();t.employerFunds.put("shop-assistant",shop);
        EducationLife.set(t,c,"interior-"+id,1);
        return new RebirthLife.HomeAction(30,switch(id){
            case "kitchen" -> "Purchased kitchen furnishings.";
            case "bedroom" -> "Purchased bedroom furnishings.";
            case "laundry" -> "Purchased a washing machine.";
            default -> "Purchased children's furnishings.";
        });
    }
}
