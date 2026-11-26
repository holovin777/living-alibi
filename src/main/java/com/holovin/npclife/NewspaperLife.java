package com.holovin.npclife;
import java.util.Comparator;
import java.util.List;

/** A local newspaper reports public notices and attributed public-space testimony. */
final class NewspaperLife {
    private NewspaperLife(){}
    static final String JOB="journalist", OCCUPATION="Journalist", CATEGORY="Newspaper (reported claims)";
    static final String LAST="press:last-edition", DISPATCH_DAY="press:dispatch-day", DISPATCH_OWNER="press:dispatch-owner";
    static final int REPORTER_PAY=40;
    static void ensure(TownLife.Town t){
        for(String k:List.of(LAST,DISPATCH_DAY,DISPATCH_OWNER))t.industryStocks.putIfAbsent(k,0);
    }
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static boolean reporter(TownLife.Citizen c){return c.alive&&c.employed&&DemographyLife.workingAge(c)&&OCCUPATION.equals(c.occupation);}
    static TownLife.Place assignment(int day,int minute){
        if(minute>=480&&minute<840)return switch(Math.floorMod(day,3)){
            case 0 -> TownLife.Place.MARKET;case 1 -> TownLife.Place.PARK;default -> TownLife.Place.CENTRO;
        };
        return TownLife.Place.LIBRARY;
    }
    static boolean safe(KnowledgeLife.Belief b,int day,int minute){
        if(b.sceneId()==null||!b.sceneId().startsWith("public:")||b.actorId().startsWith("visitor-")
                ||b.eventDay()>day||day-b.eventDay()>2||b.eventDay()==day&&b.eventMinute()>minute)return false;
        String s=b.statement();
        // Deliberately narrow public-interest allowlist: no crimes, allegations, private aliases or medical information.
        return s.contains(" crossed paths at ")||s.contains(" did not cross paths at ")||
                s.contains(" recommended the private market stall to ")||
                s.contains(" dispatched ")&&s.contains(" spare parts");
    }
    static KnowledgeLife.Belief claim(TownLife.Town t,TownLife.Citizen source,int day,int minute){
        var known=t.beliefs.get(source.id);
        if(known==null)return null;
        return known.values().stream().filter(b->safe(b,day,minute))
                .max(Comparator.comparingInt(KnowledgeLife.Belief::eventDay).thenComparingInt(KnowledgeLife.Belief::eventMinute)
                        .thenComparingInt(KnowledgeLife.Belief::confidence).thenComparing(KnowledgeLife.Belief::eventKey)).orElse(null);
    }
    static String attributed(TownLife.Citizen source,KnowledgeLife.Belief b){
        return "According to "+source.name+" ("+("DIRECT".equals(b.origin())?"first-hand account":"heard account; unverified")
                +"): "+b.statement();
    }
    static boolean filed(TownLife.Town t,int day){
        return get(t,DISPATCH_DAY)==day&&get(t,DISPATCH_OWNER)==t.player.incarnation;
    }
    static String interview(TownLife.Town t,TownLife.Citizen source,int day,int minute){
        if(!JOB.equals(t.player.jobId))throw new IllegalArgumentException("Apply as a journalist at the library first.");
        if(filed(t,day))return "You already filed a dispatch today.";
        if(!source.alive||DemographyLife.child(source)||source.activity.equals("SLEEPING")||source.activity.equals("TRAVELING")
                ||!PlayerLife.visible(t).contains(source))throw new IllegalArgumentException("Interview an available adult in your public space.");
        var b=claim(t,source,day,minute);
        if(b==null)return "I have no recent public news to report.";
        String text=attributed(source,b);
        ArchiveLife.record(t,"press-dispatch:"+t.player.incarnation+":"+day,day,minute,CATEGORY,text);
        t.industryStocks.put(DISPATCH_DAY,day);t.industryStocks.put(DISPATCH_OWNER,t.player.incarnation);
        return "Dispatch filed. "+text;
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=960||get(t,LAST)>=day)return;
        t.industryStocks.put(LAST,day);
        int cash=t.employerFunds.getOrDefault(JOB,0);
        var reporter=t.people.values().stream().filter(c->reporter(c)&&c.presentAt(TownLife.Place.LIBRARY)
                &&"WORKING".equals(c.activity)&&!EmploymentLife.sick(c,day)).findFirst().orElse(null);
        if(reporter==null||cash<REPORTER_PAY)return;
        StringBuilder text=new StringBuilder("Il Giornale · Day ").append(day);
        t.archive.values().stream().filter(e->e.day()==day&&List.of("Public safety","Business","Construction","Municipality","Yearbook").contains(e.category()))
                .limit(3).forEach(e->text.append("\nOfficial notice: ").append(e.text()));
        var b=claim(t,reporter,day,minute);
        if(b!=null)text.append("\n").append(attributed(reporter,b));
        t.archive.values().stream().filter(e->e.day()==day&&e.id().startsWith("press-dispatch:")).limit(2)
                .forEach(e->text.append("\nCorrespondent dispatch: ").append(e.text()));
        if(text.indexOf("\n")<0)text.append("\nNo new verified notices or reportable accounts today.");
        int wallet=Math.addExact(reporter.money,REPORTER_PAY);
        ArchiveLife.record(t,"press-edition:"+day,day,minute,CATEGORY,text.toString());
        reporter.money=wallet;t.employerFunds.put(JOB,cash-REPORTER_PAY);
        OpinionLife.edition(t,day,b);
    }
    record Report(int funds,boolean filedToday,List<ArchiveLife.Entry> editions){}
    static Report report(TownLife.Town t,int day){
        return new Report(t.employerFunds.getOrDefault(JOB,0),filed(t,day),
                t.archive.values().stream().filter(e->e.id().startsWith("press-edition:"))
                .sorted(Comparator.comparingInt(ArchiveLife.Entry::day).reversed()).limit(7).toList());
    }
}
