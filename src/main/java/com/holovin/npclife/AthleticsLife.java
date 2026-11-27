package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persistent club membership and abstract local competitions, never fabricated
 * opponents. Youth and adult divisions are separate. Existing districts are
 * temporary venues, not claims that full physical facilities already exist. */
final class AthleticsLife {
    private AthleticsLife(){}
    enum Sport {
        RUN("Running / Corsa",TownLife.Place.PARK), CYCLING("Cycling / Ciclismo",TownLife.Place.RIVER),
        FOOTBALL("Football / Calcio",TownLife.Place.SCHOOL), TENNIS("Tennis",TownLife.Place.SCHOOL),
        KARATE("Karate",TownLife.Place.SCHOOL), BOXING("Boxing / Pugilato",TownLife.Place.SCHOOL),
        GYM("Fitness",TownLife.Place.SCHOOL), YOGA("Yoga",TownLife.Place.PARK),
        SWIMMING("Swimming / Nuoto",TownLife.Place.RIVER);
        final String label;final TownLife.Place venue;
        Sport(String label,TownLife.Place venue){this.label=label;this.venue=venue;}
    }
    static String key(TownLife.Citizen c,String f){return "athletics:"+c.id+":"+f;}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static Sport club(TownLife.Town t,TownLife.Citizen c){
        int i=get(t,c,"club");return i>0&&i<=Sport.values().length?Sport.values()[i-1]:null;
    }
    static int skill(TownLife.Town t,TownLife.Citizen c,Sport sport){
        int prior=switch(sport){
            case RUN,CYCLING->SportLife.get(t,c,"endurance");
            case YOGA->SportLife.get(t,c,"flexibility");
            case GYM->SportLife.get(t,c,"strength");
            case KARATE->Math.max(SportLife.get(t,c,"karate"),DojoLife.get(t,c,"rank")*10);
            default->0;
        };
        return Math.max(prior,get(t,c,"skill:"+sport.name()));
    }
    static boolean healthy(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=6&&c.health>=65&&c.fatigue<75&&c.hunger<80&&c.thirst<80
            &&!EmploymentLife.sick(c,s.day)&&!NpcJusticeLife.confined(t,c,s);
    }
    static boolean present(TownLife.Citizen c,Sport sport){
        return RebirthLife.awakePresent(c)&&c.place==sport.venue&&!RebirthLife.atHome(c);
    }
    static boolean canJoin(TownLife.Town t,TownLife.Citizen c,WorldState s,Sport sport){
        return healthy(t,c,s)&&club(t,c)==null&&present(c,sport)&&s.minute>=480&&s.minute<1200;
    }
    static boolean competitionDay(int day){return Math.floorMod(day-1,7)==5;}
    static boolean canTrain(TownLife.Town t,TownLife.Citizen c,WorldState s){
        var sport=club(t,c);
        return sport!=null&&healthy(t,c,s)&&present(c,sport)&&s.minute>=1020&&s.minute<1200
            &&get(t,c,"trained")<s.day&&SportLife.get(t,c,"last-day")<s.day;
    }
    static boolean canEnter(TownLife.Town t,TownLife.Citizen c,WorldState s){
        var sport=club(t,c);
        return sport!=null&&sport!=Sport.YOGA&&healthy(t,c,s)&&present(c,sport)&&competitionDay(s.day)
            &&s.minute>=480&&s.minute<660&&get(t,c,"entry")!=s.day;
    }
    static boolean canCoach(TownLife.Town t,TownLife.Citizen c,WorldState s){
        var sport=club(t,c);
        return sport!=null&&c.age>=18&&healthy(t,c,s)&&present(c,sport)
            &&skill(t,c,sport)>=70&&get(t,c,"coach:"+sport.name())==0;
    }
    static void train(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canTrain(t,c,s))throw new IllegalArgumentException("Training unavailable.");
        var sport=club(t,c);
        set(t,c,"trained",s.day);SportLife.set(t,c,"last-day",s.day);
        set(t,c,"skill:"+sport.name(),Math.min(100,skill(t,c,sport)+2));
        EducationLife.learn(t,c,"fitness",1);c.fatigue=Math.min(100,c.fatigue+8);
        c.thirst=Math.min(100,c.thirst+4);
        PsychologyLife.adjust(t,c,"stress",20,-2);
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(RelationshipBoundaryLife.controlled(t,c)||!healthy(t,c,s)||CityExpansionLife.outer(c.home))return null;
        var sport=club(t,c);if(sport==null)return null;
        if(competitionDay(s.day)&&sport!=Sport.YOGA&&s.minute>=540&&s.minute<=660)return sport.venue;
        if(s.day%3==0&&s.minute>=990&&s.minute<=1110)return sport.venue;
        return null;
    }
    static int score(TownLife.Town t,TownLife.Citizen c,Sport sport,int day){
        return skill(t,c,sport)*3+EducationLife.get(t,c,"fitness")+c.health/5-c.fatigue/3
            +Math.floorMod(c.personalSeed^(day*0x9E3779B97F4A7C15L),21);
    }
    static void contest(TownLife.Town t,WorldState s,Sport sport,boolean youth){
        String marker="athletics:contest:"+sport.name()+":"+youth;
        if(t.industryStocks.getOrDefault(marker,0)>=s.day)return;
        t.industryStocks.put(marker,s.day);
        var entries=new ArrayList<TownLife.Citizen>();
        for(var c:t.people.values())if(club(t,c)==sport&&(c.age<18)==youth&&healthy(t,c,s)&&present(c,sport)
            &&(get(t,c,"entry")==s.day||!RelationshipBoundaryLife.controlled(t,c)))entries.add(c);
        if(entries.size()<2)return;
        entries.sort(Comparator.comparingInt((TownLife.Citizen c)->score(t,c,sport,s.day)).reversed().thenComparing(c->c.id));
        var winner=entries.getFirst();
        for(int i=0;i<entries.size();i++){
            var c=entries.get(i);
            set(t,c,"last-rank",i+1);set(t,c,"last-contest",s.day);
            set(t,c,"contests",Math.addExact(get(t,c,"contests"),1));
            c.fatigue=Math.min(100,c.fatigue+10);
        }
        set(t,winner,"wins",Math.addExact(get(t,winner,"wins"),1));
        set(t,winner,"wins:"+sport.name(),Math.addExact(get(t,winner,"wins:"+sport.name()),1));
        if(!youth){
            int fund=t.employerFunds.getOrDefault("school",0);
            int prize=Math.min(15,Math.max(0,fund-40));
            winner.money=Math.addExact(winner.money,prize);t.employerFunds.put("school",fund-prize);
        }
        ArchiveLife.record(t,"sports:"+sport.name()+":"+youth+":"+s.day,s.day,s.minute,"Sport",
            winner.name+" won the "+sport.label+" "+(youth?"youth":"adult")+" event with "+entries.size()+" participants.");
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute==360)for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0||c.age<6||RelationshipBoundaryLife.controlled(t,c)||get(t,c,"considered")>0)continue;
            set(t,c,"considered",1);
            if(Math.floorMod(c.personalSeed,4)==0||"STAY_HEALTHY".equals(c.goal))
                set(t,c,"club",1+Math.floorMod(c.personalSeed>>>16,Sport.values().length));
        }
        if(s.minute==1110)for(var c:t.people.values())
            if(!RelationshipBoundaryLife.controlled(t,c)&&s.day%3==0&&canTrain(t,c,s))train(t,c,s);
        if(s.minute==660&&competitionDay(s.day))for(var sport:Sport.values())if(sport!=Sport.YOGA){
            contest(t,s,sport,true);contest(t,s,sport,false);
        }
    }
    static String act(TownLife.Town t,WorldState s,String action){
        var c=RebirthLife.child(t);
        if(c==null||!c.alive||c.age<6||!RebirthLife.awakePresent(c))throw new IllegalArgumentException("Sport action unavailable.");
        if(action.startsWith("join-")){
            var sport=Sport.valueOf(action.substring(5));
            if(!canJoin(t,c,s,sport))throw new IllegalArgumentException("Visit the club venue.");
            set(t,c,"club",sport.ordinal()+1);return "Joined "+sport.label+".";
        }
        return switch(action){
            case "leave"->{set(t,c,"club",0);set(t,c,"entry",0);yield "Left sports club.";}
            case "train"->{train(t,c,s);yield "Training completed.";}
            case "enter"->{if(!canEnter(t,c,s))throw new IllegalArgumentException("Registration unavailable.");
                set(t,c,"entry",s.day);yield "Registered. Be at the venue at 11:00.";}
            case "coach"->{if(!canCoach(t,c,s))throw new IllegalArgumentException("Coach requirements not met.");
                set(t,c,"coach:"+club(t,c).name(),1);yield "Earned the fictional coaching qualification.";}
            default->throw new IllegalArgumentException("Unknown sport action.");
        };
    }
    record View(String club,int skill,int contests,int wins,int lastRank,int nextCompetitionDay,boolean coach,boolean competitiveLevel){}
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null)return null;var sport=club(t,c);
        int next=s.day+Math.floorMod(5-Math.floorMod(s.day-1,7),7);
        if(next==s.day&&s.minute>=660)next+=7;
        return new View(sport==null?"":sport.label,sport==null?0:skill(t,c,sport),get(t,c,"contests"),get(t,c,"wins"),
            get(t,c,"last-rank"),next,sport!=null&&get(t,c,"coach:"+sport.name())>0,
            sport!=null&&skill(t,c,sport)>=60&&get(t,c,"wins:"+sport.name())>=3);
    }
}
