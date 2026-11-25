package com.holovin.npclife;
import java.util.List;

/** Persistent skills, attendance and aptitude-based employment. */
final class EducationLife {
    private EducationLife(){}
    static final String LESSONS="education:lessons", GRADUATES="education:graduates", EXPERIENCE="education:experience", SCHOOL_SERVICE="school-services";
    static final List<String> SUBJECTS=List.of("technical","social","academic","commerce");
    static final List<String> FIELDS=List.of("technical","social","academic","commerce","school-days","last-school","last-work","graduate","fitness","hobby-days","last-hobby","allowance-week","allowance-total");
    static String key(TownLife.Citizen c,String f){return "education:"+c.id+":"+f;}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return get(t,key(c,f));}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){set(t,key(c,f),n);}
    static void ensure(TownLife.Town t){
        for(String k:List.of(LESSONS,GRADUATES,EXPERIENCE))t.industryStocks.putIfAbsent(k,0);
        for(var c:t.people.values())if(c.generationIndex>0)ensureResident(t,c);
    }
    static String subject(String job){
        return switch(job){
            case "Mechanic","Miner","Factory machinist","Transport driver","Independent fabricator","Company freight driver" -> "technical";
            case "Nurse","Barista" -> "social";
            case "Teacher","Librarian" -> "academic";
            case "Market vendor","Private stall seller","Office clerk","Baker" -> "commerce";
            default -> "academic";
        };
    }
    static int aptitude(TownLife.Citizen c,String f){
        return 1+Math.floorMod(Long.rotateLeft(c.personalSeed,7+13*SUBJECTS.indexOf(f)),5);
    }
    static void ensureResident(TownLife.Town t,TownLife.Citizen c){
        for(String f:FIELDS){
            int initial=0;
            if(SUBJECTS.contains(f)&&c.age>=18)initial=10+aptitude(c,f)*3+(c.employed&&f.equals(subject(c.occupation))?25:0);
            t.industryStocks.putIfAbsent(key(c,f),initial);
        }
    }
    static void learn(TownLife.Town t,TownLife.Citizen c,String f,int gain){
        set(t,c,f,Math.min(100,get(t,c,f)+gain));
    }
    static void graduate(TownLife.Town t,TownLife.Citizen c){
        ensureResident(t,c);
        if(get(t,c,"graduate")>0)return;
        set(t,c,"graduate",1);set(t,GRADUATES,Math.addExact(get(t,GRADUATES),1));
    }
    static void attendance(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!c.alive||c.generationIndex<=0||c.travelRemaining>0)return;
        ensureResident(t,c);
        int day=clock.day;
        if(DemographyLife.child(c)&&c.age>=6&&c.place==TownLife.Place.SCHOOL&&"STUDYING".equals(c.activity)
                &&clock.minute==600&&(day-1)%7<5&&get(t,c,"last-school")<day){
            int funds=t.employerFunds.getOrDefault("school",0);
            if(funds<1)return;
            t.employerFunds.put("school",funds-1);
            t.employerFunds.merge(SCHOOL_SERVICE,1,Math::addExact);
            set(t,c,"last-school",day);
            set(t,c,"school-days",Math.addExact(get(t,c,"school-days"),1));
            set(t,LESSONS,Math.addExact(get(t,LESSONS),1));
            String f=RebirthLife.focus(t,c);
            if(f==null)f=SUBJECTS.get(Math.floorMod(day+c.generationIndex,SUBJECTS.size()));
            learn(t,c,f,aptitude(c,f)>=4?2:1);
        }
        if(DemographyLife.workingAge(c)&&c.employed&&"WORKING".equals(c.activity)&&clock.minute%60==0
                &&c.place==c.workplace&&get(t,c,"last-work")<day){
            set(t,c,"last-work",day);
            learn(t,c,subject(c.occupation),1);
            set(t,EXPERIENCE,Math.addExact(get(t,EXPERIENCE),1));
        }
    }
    static boolean availableRole(TownLife.Town t,TownLife.Citizen c,String job){
        String f=subject(job);
        int minimum=switch(job){case "Teacher","Nurse","Journalist" -> 35;case "Librarian","Mechanic","Factory machinist" -> 20;default -> 0;};
        if(get(t,c,f)<minimum)return false;
        if(NewspaperLife.OCCUPATION.equals(job)&&(t.employerFunds.getOrDefault(NewspaperLife.JOB,0)<NewspaperLife.REPORTER_PAY*30||c.money<200))return false;
        String employer=IndustryLife.companyFor(job);
        if(employer!=null&&t.employerFunds.getOrDefault(employer,0)<IndustryLife.HOURLY_WAGE)return false;
        return t.people.values().stream().filter(x->x.alive&&x.employed&&job.equals(x.occupation)).count()<(employer==null?5:2);
    }
    static int preferredRole(TownLife.Town t,TownLife.Citizen c,String[] jobs){
        ensureResident(t,c);
        int best=-1,bestScore=Integer.MIN_VALUE;
        for(int i=0;i<jobs.length;i++){
            String job=jobs[i],f=subject(job);
            int skill=get(t,c,f);
            if(!availableRole(t,c,job))continue;
            long occupied=t.people.values().stream().filter(x->x.alive&&x.employed&&job.equals(x.occupation)).count();
            int score=skill*2+aptitude(c,f)*8-(int)occupied*12;
            if("LEARN_SKILLS".equals(c.goal)&&f.equals("academic"))score+=8;
            if("MAKE_FRIENDS".equals(c.goal)&&f.equals("social"))score+=8;
            if("SAVE_MONEY".equals(c.goal)&&f.equals("commerce"))score+=8;
            score+=Math.floorMod(c.personalSeed+i*31,7);
            if(score>bestScore){bestScore=score;best=i;}
        }
        return best;
    }
    record Report(int lessons,int graduates,int workDays,int pupils,int schoolFunds){}
    static Report report(TownLife.Town t){
        int pupils=(int)t.people.values().stream().filter(c->c.alive&&DemographyLife.child(c)&&c.age>=6).count();
        return new Report(get(t,LESSONS),get(t,GRADUATES),get(t,EXPERIENCE),pupils,t.employerFunds.getOrDefault("school",0));
    }
}
