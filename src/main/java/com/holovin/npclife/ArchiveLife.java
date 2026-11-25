package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Public historical milestones, separate from private memories and investigation truth. */
final class ArchiveLife {
    private ArchiveLife(){}
    static final int PAGE_SIZE=8;
    record Entry(String id,int day,int minute,String category,String text){}
    record Report(int year,int currentYear,int firstYear,int page,int pages,int total,List<Entry> entries){}
    static void record(TownLife.Town t,String id,int day,int minute,String category,String text){
        var entry=new Entry(id,day,minute,category,text);
        if(t.archive.putIfAbsent(id,entry)==null)t.pendingArchive.add(entry);
    }
    static void begin(TownLife.Town t,int day,int minute){
        record(t,"archive-open",day,minute,"Archive","Public historical archive opened. Earlier events are not reconstructed.");
    }
    static void transitions(TownLife.Town before,TownLife.Town after,int day,int minute){
        int incarnation=after.player.incarnation;
        if(after.business.registered&&(!before.business.registered||before.business.ownerIncarnation!=after.business.ownerIncarnation))
            record(after,"workshop:"+incarnation+":"+day+":"+minute,day,minute,"Business",
                    before.business.registered?"The fabrication workshop changed ownership.":"A new fabrication workshop was registered.");
        if(MarketStallLife.get(before,MarketStallLife.LICENSE)==0&&MarketStallLife.get(after,MarketStallLife.LICENSE)>0)
            record(after,"stall:"+incarnation,day,minute,"Business","A private market stall was registered.");
        if(ConstructionLife.get(before,ConstructionLife.OWNER)==0&&ConstructionLife.get(after,ConstructionLife.OWNER)>0)
            record(after,"plot:"+incarnation+":"+day+":"+minute,day,minute,"Construction","The Via Verdi plot was purchased for development.");
        if(ConstructionLife.get(before,ConstructionLife.OWNER)>0&&ConstructionLife.get(after,ConstructionLife.OWNER)==0
                &&ConstructionLife.get(after,ConstructionLife.STAGE)==ConstructionLife.STAGES)
            record(after,"house-sale",day,minute,"Construction","The completed Via Verdi house was sold to the municipality.");
    }
    static void annual(TownLife.Town t,int day,int minute){
        if(CalendarLife.dayOfYear(day)!=365||minute!=1439)return;
        var census=DemographyLife.report(t);
        int year=CalendarLife.year(day);
        record(t,"year:"+year,day,minute,"Yearbook","Year "+year+" closed with "+census.alive()+
                " living procedural residents, "+census.children()+" children, "+census.retired()+
                " retired residents; "+census.births()+" births and "+census.deaths()+
                " deaths recorded cumulatively. Municipal treasury: "+MunicipalLife.balance(t,MunicipalLife.CITY)+" EUR.");
    }
    static Report report(TownLife.Town t,int currentDay,int requestedYear,int requestedPage){
        int current=CalendarLife.year(currentDay);
        int first=t.archive.values().stream().mapToInt(e->CalendarLife.year(e.day())).min().orElse(current);
        int year=Math.max(first,Math.min(current,requestedYear<=0?current:requestedYear));
        var all=t.archive.values().stream().filter(e->CalendarLife.year(e.day())==year)
                .sorted(Comparator.comparingInt(Entry::day).thenComparingInt(Entry::minute).thenComparing(Entry::id).reversed()).toList();
        int pages=Math.max(1,(all.size()+PAGE_SIZE-1)/PAGE_SIZE);
        int page=Math.max(1,Math.min(pages,requestedPage));
        int start=(page-1)*PAGE_SIZE;
        return new Report(year,current,first,page,pages,all.size(),List.copyOf(all.subList(start,Math.min(all.size(),start+PAGE_SIZE))));
    }
}
