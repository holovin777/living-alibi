package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Short public lobby appearances derived from real home arrivals. No private unit data. */
final class ResidentialArrivalLife {
    private ResidentialArrivalLife(){}
    static final int WINDOW=4;
    private static String key(TownLife.Citizen c){return "residential:arrival:"+c.id;}
    private static int tick(WorldState s){return Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1;}

    static void arrived(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c.alive&&c.place==c.home&&AddressLife.building(c)!=0)
            t.industryStocks.put(key(c),tick(s));
        else t.industryStocks.remove(key(c));
    }

    static List<TownLife.Citizen> visible(TownLife.Town t,WorldState s,int building){
        if(building==0||!PhoneLife.available(t)||t.player.travelRemaining>0||t.player.cycling||
                !AddressLife.district(building).equals(t.player.place.name()))return List.of();
        int now=tick(s);
        return t.people.values().stream()
                .filter(c->c.alive&&c.travelRemaining==0&&c.place==c.home&&
                        c.currentSpace().equals(c.homeUnit)&&AddressLife.building(c)==building)
                .filter(c->!java.util.Set.of("SLEEPING","IN_CUSTODY","SICK_LEAVE","RESTING_WHILE_SICK","WORKING").contains(c.activity))
                .filter(c->{int arrival=t.industryStocks.getOrDefault(key(c),0);
                    return arrival>0&&now>=arrival&&now-arrival<WINDOW;})
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->t.industryStocks.get(key(c))).reversed()
                        .thenComparing(c->c.id))
                .limit(2).toList();
    }
}
