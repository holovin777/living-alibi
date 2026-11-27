package com.holovin.npclife;

import java.util.*;

/** Small, fictional developmental model. Existing citizen traits and family links
 * remain authoritative. Financial circumstances alone never determine parenting.
 * Counters use the existing persisted industry stock map and survive adulthood.
 */
final class ChildhoodLife {
    private ChildhoodLife(){}
    private static String key(TownLife.Citizen c,String f){return "childhood:"+c.id+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    private static int score(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),50);}
    private static int clamp(int n){return Math.max(0,Math.min(100,n));}
    private static void increment(TownLife.Town t,TownLife.Citizen c,String f){put(t,c,f,Math.addExact(get(t,c,f),1));}
    private static boolean available(TownLife.Citizen c){
        return c!=null&&HomeRoutineLife.atHome(c)&&!Set.of("WORKING","SLEEPING","SICK_LEAVE","RESTING_WHILE_SICK","IN_CUSTODY").contains(c.activity);
    }
    private static boolean with(TownLife.Citizen child,TownLife.Citizen adult){
        return available(adult)&&child.homeUnit.equals(adult.homeUnit)&&child.home==adult.home;
    }
    record View(int observedDays,int supportedDays,int conflictDays,int security,int confidence,int independence){}
    static View view(TownLife.Town t,TownLife.Citizen c){
        if(c==null||c.generationIndex<=0)return null;
        return new View(get(t,c,"days"),get(t,c,"supported"),get(t,c,"conflict"),
                score(t,c,"security"),score(t,c,"confidence"),score(t,c,"independence"));
    }
    static void tick(TownLife.Town t,WorldState clock){
        int minute=clock.minute,day=clock.day;
        if(minute!=420&&minute!=1080&&minute!=1200&&minute!=1260)return;
        for(var c:t.people.values())if(c.alive&&DemographyLife.child(c)){
            if(minute==1260){finishDay(t,c,day);continue;}
            if(get(t,c,"sample-day")!=day){
                put(t,c,"sample-day",day);put(t,c,"contact",0);put(t,c,"tension",0);
                put(t,c,"last-sample",-1);
            }
            if(get(t,c,"last-sample")>=minute)continue;
            put(t,c,"last-sample",minute);
            if(!available(c))continue;
            var a=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
            if(with(c,a)||with(c,b))put(t,c,"contact",1);
            if(a!=b&&with(c,a)&&with(c,b)){
                var relation=t.peerRelationships.get(SocialNetwork.key(a.id,b.id));
                if(relation!=null&&relation.tension>=65)put(t,c,"tension",1);
            }
        }
    }
    private static void finishDay(TownLife.Town t,TownLife.Citizen c,int day){
        if(get(t,c,"sample-day")!=day||get(t,c,"last-day")>=day)return;
        put(t,c,"last-day",day);
        increment(t,c,"days");
        if(get(t,c,"contact")>0){increment(t,c,"supported");increment(t,c,"week-support");}
        if(get(t,c,"tension")>0){increment(t,c,"conflict");increment(t,c,"week-conflict");}
        // Only evaluate complete seven-day observation periods; a missed
        // evening or a working parent is not treated as a harmful event.
        if(get(t,c,"days")%7!=0)return;
        int care=get(t,c,"week-support"),conflict=get(t,c,"week-conflict");
        int security=clamp(score(t,c,"security")+(care>=4?1:0)-(conflict>=3?1:0));
        int confidence=clamp(score(t,c,"confidence")+(care>=4&&conflict<3?1:0));
        int independence=clamp(score(t,c,"independence")+(c.age>=6&&c.health>=60?1:0));
        put(t,c,"security",security);put(t,c,"confidence",confidence);put(t,c,"independence",independence);
        put(t,c,"week-support",0);put(t,c,"week-conflict",0);
        // Small changes only, and no forced career, disorder or moral judgment.
        if(get(t,c,"days")%28==0){
            if(confidence>=65)c.sociability=Math.min(100,c.sociability+1);
            if(independence>=65)c.carefulness=Math.min(100,c.carefulness+1);
        }
    }
}
