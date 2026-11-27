package com.holovin.npclife;

import java.util.List;

/** Fictional job centre: guidance rather than guaranteed employment.
 * Training payments are conserved transfers from the existing social fund. */
final class EmploymentCenterLife {
    private EmploymentCenterLife(){}
    static final int MAX_LESSONS=3,CASH_LIMIT=35;
    private static String key(TownLife.Town t,String field){return "job-centre:"+t.player.incarnation+":"+field;}
    private static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(t,f),0);}
    private static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(t,f),n);}
    static boolean visitor(TownLife.Town t){return !RebirthLife.active(t)&&t.player.alive&&t.player.travelRemaining==0;}
    static boolean open(WorldState s){return (s.day-1)%7<5&&s.minute>=540&&s.minute+15<=780;}
    static boolean registered(TownLife.Town t){return get(t,"registered")>0;}
    static boolean canRegister(TownLife.Town t,WorldState s){
        return visitor(t)&&t.player.place==TownLife.Place.OFFICE&&open(s)&&t.player.jobId==null&&!registered(t);
    }
    static String register(TownLife.Town t,WorldState s){
        if(!canRegister(t,s))throw new IllegalArgumentException("Visit the job centre while unemployed, Monday–Friday 09:00–13:00.");
        set(t,"registered",s.day);
        return "Registered as a job seeker. Choose a suitable vacancy or attend vocational training.";
    }
    static boolean fundedLesson(TownLife.Town t,WorldState s){
        return visitor(t)&&registered(t)&&t.player.jobId==null&&t.player.money<CASH_LIMIT&&
                get(t,"lessons")<MAX_LESSONS&&get(t,"last-lesson")<s.day&&
                t.employerFunds.getOrDefault(SocialProtectionLife.ACCOUNT,0)>=CareerLife.LESSON_FEE;
    }
    static String study(TownLife.Town t,WorldState s,String subject){
        if(!fundedLesson(t,s))return CareerLife.study(t,subject,s.minute);
        // A temporary credit is consumed by the normal lesson, preserving its validation.
        // Restore it if the lesson is refused, including wrong place/time/subject.
        int original=t.player.money;
        t.player.money=Math.addExact(original,CareerLife.LESSON_FEE);
        try {CareerLife.study(t,subject,s.minute);}
        catch(RuntimeException ex){t.player.money=original;throw ex;}
        t.employerFunds.merge(SocialProtectionLife.ACCOUNT,-CareerLife.LESSON_FEE,Integer::sum);
        set(t,"lessons",get(t,"lessons")+1);set(t,"last-lesson",s.day);
        return "Completed a vocational lesson funded by the job centre (+1 skill).";
    }
    record Vacancy(String title,String workplace,String subject,int missingSkill,int wage){}
    record Report(boolean registered,boolean open,boolean canRegister,boolean fundedLesson,int lessonsRemaining,
                  int unemployedResidents,List<Vacancy> vacancies){}
    static Report report(TownLife.Town t,WorldState s){
        var p=t.player;
        var list=CareerLife.offers().stream()
                .filter(v->!v.id().equals(p.jobId)&&t.employerFunds.getOrDefault(v.id(),0)>=v.wage())
                .map(v->new Vacancy(v.title(),v.workplace().label,v.subject(),
                        Math.max(0,v.requiredSkill()-CareerLife.skill(p,v.subject())),v.wage()))
                .sorted(java.util.Comparator.comparingInt(Vacancy::missingSkill).thenComparing(Vacancy::title)).toList();
        int seekers=(int)t.people.values().stream().filter(c->c.alive&&!c.employed&&DemographyLife.workingAge(c)).count();
        return new Report(registered(t),open(s),canRegister(t,s),fundedLesson(t,s),
                Math.max(0,MAX_LESSONS-get(t,"lessons")),seekers,list);
    }
}
