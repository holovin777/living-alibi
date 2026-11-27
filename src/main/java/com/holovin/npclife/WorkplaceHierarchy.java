package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Functional team ranks derived from current employment, tenure and skills.
 * They are not new occupations or payroll grades. No separate save state. */
final class WorkplaceHierarchy {
    private WorkplaceHierarchy() {}
    enum Rank { TRAINEE, WORKER, SENIOR, TEAM_LEAD }
    static boolean eligible(TownLife.Citizen c) {
        return c.alive && c.employed && c.age>=18 && c.age<67 &&
                c.workplace!=null && c.occupation!=null;
    }
    static List<TownLife.Citizen> team(TownLife.Town t,TownLife.Citizen c) {
        if(!eligible(c))return List.of();
        return t.people.values().stream().filter(p->eligible(p)&&p.workplace==c.workplace
                &&p.occupation.equals(c.occupation)).toList();
    }
    static int tenure(TownLife.Citizen c,int day) {
        // Unknown legacy hire dates start at day 1 rather than assuming years of service.
        return Math.max(0,day-Math.max(1,c.lastHiredDay));
    }
    static int skill(TownLife.Town t,TownLife.Citizen c) {
        return EducationLife.get(t,c,EducationLife.subject(c.occupation));
    }
    static boolean onDuty(TownLife.Citizen c,int day) {
        return eligible(c)&&c.presentAt(c.workplace)&&"WORKING".equals(c.activity)
                &&!EmploymentLife.sick(c,day);
    }
    static TownLife.Citizen lead(TownLife.Town t,TownLife.Citizen c,int day) {
        var roster=team(t,c);
        if(roster.size()<2)return null;
        return roster.stream().filter(p->tenure(p,day)>=30&&skill(t,p)>=60&&p.carefulness>=55
                        &&(WorkplacePerformance.report(t,p).samples()<5||WorkplacePerformance.report(t,p).quality()>=55))
                .sorted(Comparator.<TownLife.Citizen>comparingInt(p->skill(t,p)).reversed()
                    .thenComparing(Comparator.<TownLife.Citizen>comparingInt(p->tenure(p,day)).reversed())
                    .thenComparing(Comparator.<TownLife.Citizen>comparingInt(p->p.carefulness).reversed())
                    .thenComparing(p->p.id)).findFirst().orElse(null);
    }
    static Rank rank(TownLife.Town t,TownLife.Citizen c,int day) {
        if(!eligible(c))return null;
        if(lead(t,c,day)==c)return Rank.TEAM_LEAD;
        if(tenure(c,day)<7)return Rank.TRAINEE;
        if(tenure(c,day)>=14&&skill(t,c)>=45)return Rank.SENIOR;
        return Rank.WORKER;
    }
}
