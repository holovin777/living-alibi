package com.holovin.npclife;

import java.util.*;

/** Temporary accommodation for displaced autonomous households, with finite daily beds. */
final class FireHousingLife {
    private FireHousingLife(){}
    static final TownLife.Place PLACE=CaritasLife.PLACE;
    static final int CAPACITY=24, BED_COST=2;
    static final String ACCOUNT="fire-shelter", DAY="fire-housing:day", RESERVED="fire-housing:reserved";
    private static String bed(TownLife.Citizen c){return "fire-housing:bed:"+c.generationIndex;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static boolean displaced(TownLife.Town t,TownLife.Citizen c){
        return c.alive&&c.generationIndex>0&&(c.home==TownLife.Place.NORTH_RESIDENCES||c.home==TownLife.Place.SOUTH_RESIDENCES)
                &&FireRecoveryLife.closed(t,c.home)&&!RebirthLife.linkedAdult(t,c)&&!RebirthLife.linkedChild(t,c);
    }
    static boolean accommodated(TownLife.Town t,TownLife.Citizen c){
        int day=get(t,DAY);
        return day>0&&get(t,bed(c))==day&&displaced(t,c);
    }
    static void prepare(TownLife.Town t,WorldState s){
        if(get(t,DAY)!=s.day){t.industryStocks.put(DAY,s.day);t.industryStocks.put(RESERVED,0);}
        if(s.minute%15!=0)return;
        var groups=new TreeMap<String,List<TownLife.Citizen>>();
        for(var c:t.people.values()){
            if(!displaced(t,c)||NpcJusticeLife.confined(t,c,s))continue;
            String unit=c.homeUnit==null?c.id:c.homeUnit;
            groups.computeIfAbsent(c.home.name()+":"+unit,k->new ArrayList<>()).add(c);
        }
        // Prioritize households with children; allocate to the whole eligible group.
        var households=new ArrayList<>(groups.values());
        households.sort(Comparator.comparing((List<TownLife.Citizen> g)->g.stream().noneMatch(DemographyLife::child))
            .thenComparing(g->g.get(0).id));
        for(var group:households){
            var missing=group.stream().filter(c->!accommodated(t,c)).toList();
            int n=missing.size(), cost=n*BED_COST;
            if(n==0||n>CAPACITY-get(t,RESERVED)||t.employerFunds.getOrDefault("city-maintenance",0)<cost)continue;
            if(!SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,cost))continue;
            for(var c:missing)t.industryStocks.put(bed(c),s.day);
            t.industryStocks.merge(RESERVED,n,Math::addExact);
        }
    }
    record View(int capacity,int reserved,int accommodated,int present,int waiting,int dailyCost){}
    static View view(TownLife.Town t){
        int housed=0,present=0,waiting=0;
        for(var c:t.people.values())if(displaced(t,c)){
            if(accommodated(t,c)){housed++;if(c.presentAt(PLACE))present++;}
            else waiting++;
        }
        return new View(CAPACITY,get(t,RESERVED),housed,present,waiting,BED_COST);
    }
}
