package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Fictional organized-crime faction. Membership and case links are private engine state. */
final class MafiaLife {
    private MafiaLife() {}
    private static final String SEEDED="mafia:seeded";
    private static final String MEMBER="mafia:member:";
    private static final String LINK="mafia:case:";
    private static final String RESOLVED="mafia:resolved:";
    private static final int LEADER=2, MEMBER_ROLE=1, INACTIVE=3;

    private static int role(TownLife.Town t,TownLife.Citizen c) {
        return t.industryStocks.getOrDefault(MEMBER+c.id,0);
    }
    static boolean active(TownLife.Town t,TownLife.Citizen c) {
        int r=role(t,c);
        return c.alive && c.age>=18 && !PoliceLife.staff(c) && (r==LEADER||r==MEMBER_ROLE);
    }
    /** One small fictional group, with ordinary occupations and no public name tags. */
    static void establish(TownLife.Town t) {
        if(t.people.size()<24 || t.industryStocks.containsKey(SEEDED))return;
        var candidates=t.people.values().stream()
                .filter(c->c.alive && c.generationIndex>0 && c.age>=25 && c.age<60 && !PoliceLife.staff(c))
                .sorted(Comparator.comparingLong((TownLife.Citizen c)->c.personalSeed)
                        .thenComparing(c->c.id)).limit(3).toList();
        if(candidates.size()<3)return;
        for(int i=0;i<candidates.size();i++)
            t.industryStocks.put(MEMBER+candidates.get(i).id,i==0?LEADER:MEMBER_ROLE);
        t.industryStocks.put(SEEDED,1);
    }
    /** Modest fictional group pressure; normal opportunity, evidence and cooldowns still apply. */
    static MotiveLife.Assessment assess(TownLife.Town t,TownLife.Citizen offender,MotiveLife.Assessment ordinary) {
        if(!active(t,offender))return ordinary;
        return new MotiveLife.Assessment("GROUP_PRESSURE",MotiveLife.clamp(ordinary.strength()+12));
    }
    static void noteCase(TownLife.Town t,InvestigationLife.CaseFile c) {
        var offender=t.people.get(c.offenderId);
        if(offender==null || !active(t,offender))return;
        t.industryStocks.putIfAbsent(LINK+c.id,1);
        // The publicly reported incident is not proof of group involvement.
        ArchiveLife.record(t,"safety-report:"+c.id,c.day,c.minute,"Public safety",
                "Police received a theft report in "+c.place.label+". The circumstances are under investigation.");
    }
    static void tick(TownLife.Town t,WorldState clock) {
        if(clock.minute!=600)return;
        establish(t);
        if(!t.industryStocks.containsKey(SEEDED))return;
        // Resolved cases weaken the faction, without automatically imprisoning NPCs.
        for(var c:t.cases.values()) {
            if(!c.solved || !t.industryStocks.containsKey(LINK+c.id)
                    || t.industryStocks.containsKey(RESOLVED+c.id))continue;
            t.industryStocks.put(RESOLVED+c.id,1);
            t.industryStocks.put(MEMBER+c.offenderId,INACTIVE);
            ArchiveLife.record(t,"safety-resolved:"+c.id,clock.day,clock.minute,"Public safety",
                    "Police reported that a previously reported theft investigation has been resolved.");
        }
        var living=t.people.values().stream().filter(c->active(t,c))
                .sorted(Comparator.comparing(c->c.id)).toList();
        // Succession is internal; no resident is recruited automatically after dissolution.
        if(!living.isEmpty() && living.stream().noneMatch(c->role(t,c)==LEADER))
            t.industryStocks.put(MEMBER+living.get(0).id,LEADER);
    }
}
