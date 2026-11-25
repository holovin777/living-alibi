package com.holovin.npclife;
import java.util.List;

/** Individual opinions change from first-hand experiences or editions actually read. */
final class OpinionLife {
    private OpinionLife(){}
    static final String PRESS_DAY="opinion:press-day", PRESS_CITY="opinion:press-city", PRESS_STALL="opinion:press-stall", PRESS_OWNER="opinion:press-owner";
    static final List<String> FIELDS=List.of("city","stall","last-fountain","last-purchase","last-read","read-count");
    static String key(TownLife.Citizen c,String f){return "opinion:"+c.id+":"+f;}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static int score(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),50);}
    static boolean eligible(TownLife.Citizen c){return c.alive&&c.generationIndex>0&&!DemographyLife.child(c);}
    static void ensureResident(TownLife.Town t,TownLife.Citizen c){
        for(String f:FIELDS)t.industryStocks.putIfAbsent(key(c,f),f.equals("city")||f.equals("stall")?50:0);
    }
    static void ensure(TownLife.Town t){
        for(String k:List.of(PRESS_DAY,PRESS_OWNER))t.industryStocks.putIfAbsent(k,0);
        for(String k:List.of(PRESS_CITY,PRESS_STALL))t.industryStocks.putIfAbsent(k,1);
        for(var c:t.people.values())if(c.generationIndex>0)ensureResident(t,c);
    }
    static void change(TownLife.Town t,TownLife.Citizen c,String f,int delta){
        if(!eligible(c))return;
        ensureResident(t,c);
        t.industryStocks.put(key(c,f),Math.max(0,Math.min(100,score(t,c,f)+delta)));
    }
    static void daily(TownLife.Town t,TownLife.Citizen c,String marker,String field,int day,int delta){
        if(!eligible(c))return;
        ensureResident(t,c);
        if(get(t,key(c,marker))>=day)return;
        t.industryStocks.put(key(c,marker),day);change(t,c,field,delta);
    }
    static void fountain(TownLife.Town t,TownLife.Citizen c,int day,boolean success){
        daily(t,c,"last-fountain","city",day,success?3:-2);
    }
    static void purchase(TownLife.Town t,TownLife.Citizen c,int day,int amount){
        daily(t,c,"last-purchase","stall",day,amount<=2?4:amount==3?2:-1);
    }
    static void clearStall(TownLife.Town t){
        for(var c:t.people.values())if(c.generationIndex>0){
            t.industryStocks.put(key(c,"stall"),50);t.industryStocks.put(key(c,"last-purchase"),0);
        }
        t.industryStocks.put(PRESS_STALL,1);t.industryStocks.put(PRESS_OWNER,0);
    }
    /** Called only when a real edition is published; encode signed signals as 0/1/2 for SQLite. */
    static void edition(TownLife.Town t,int day,KnowledgeLife.Belief claim){
        int city=0;
        for(var e:t.archive.values())if(e.day()==day&&e.category().equals("Municipality")){
            if(e.id().startsWith("budget-low:"))city=-1;
            else if(e.id().startsWith("budget-recovery:"))city=1;
        }
        int stall=0;
        if(claim!=null&&claim.statement().contains(" recommended the private market stall to ")&&
                day==claim.eventDay()&&claim.confidence()>=60)
            stall=claim.statement().startsWith("It is not true that ")?-1:1;
        t.industryStocks.put(PRESS_DAY,day);t.industryStocks.put(PRESS_CITY,city+1);
        t.industryStocks.put(PRESS_STALL,stall+1);t.industryStocks.put(PRESS_OWNER,t.player.incarnation);
    }
    static void read(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        int edition=get(t,PRESS_DAY);
        if(!eligible(c)||edition<=0||edition>day||day-edition>7||minute%60!=0||
                !c.presentAt(TownLife.Place.LIBRARY)||"SLEEPING".equals(c.activity)||"WORKING".equals(c.activity))return;
        ensureResident(t,c);
        if(get(t,key(c,"last-read"))>=edition)return;
        t.industryStocks.put(key(c,"last-read"),edition);
        t.industryStocks.merge(key(c,"read-count"),1,Math::addExact);
        change(t,c,"city",2*(get(t,PRESS_CITY)-1));
        if(get(t,PRESS_OWNER)==t.player.incarnation&&MarketStallLife.get(t,MarketStallLife.LICENSE)>0)
            change(t,c,"stall",get(t,PRESS_STALL)-1);
    }
    record Report(int respondents,int cityTrust,int stallTrust,int readers,int lastEdition){}
    static Report report(TownLife.Town t){
        var adults=t.people.values().stream().filter(OpinionLife::eligible).toList();
        int city=(int)Math.round(adults.stream().mapToInt(c->score(t,c,"city")).average().orElse(50));
        int stall=(int)Math.round(adults.stream().mapToInt(c->score(t,c,"stall")).average().orElse(50));
        int readers=(int)adults.stream().filter(c->get(t,key(c,"read-count"))>0).count();
        return new Report(adults.size(),city,stall,readers,get(t,PRESS_DAY));
    }
}
