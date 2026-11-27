package com.holovin.npclife;

import java.util.*;

/** Bounded shift routines. Journal events are not completed production orders.
 * Uses existing persisted skill and stock maps; never changes wages or inventory. */
final class WorkplaceManagement {
    private WorkplaceManagement(){}
    static String teamKey(TownLife.Citizen c,String phase){
        return "workplace-management:"+c.workplace.name()+":"+c.occupation+":"+phase;
    }
    static void tick(TownLife.Town t,WorldState s,List<TownLife.Event> truth,List<TownLife.Observation> seen){
        String phase=switch(s.minute){
            case 540 -> "briefing";
            case 600 -> "mentoring";
            case 840 -> "review";
            default -> null;
        };
        if(phase==null)return;
        Set<String> processed=new HashSet<>();
        for(var worker:t.people.values()){
            if(!WorkplaceHierarchy.onDuty(worker,s.day))continue;
            String key=teamKey(worker,phase);
            if(!processed.add(key)||t.industryStocks.getOrDefault(key,0)>=s.day)continue;
            var lead=WorkplaceHierarchy.lead(t,worker,s.day);
            if(lead==null||!WorkplaceHierarchy.onDuty(lead,s.day)||lead.fatigue>=80
                    ||RebirthLife.linkedAdult(t,lead)||TownLife.inPlayerConversation(t,lead))continue;
            var peers=WorkplaceHierarchy.team(t,lead).stream()
                    .filter(p->SocialNetwork.coworkersOnShift(lead,p,s.day)
                            &&!RebirthLife.linkedAdult(t,p)&&!TownLife.inPlayerConversation(t,p))
                    .sorted(Comparator.comparing(p->p.id)).toList();
            if(peers.isEmpty())continue;
            String event;
            switch(phase){
                case "briefing" -> event=lead.name+" reviewed today's work priorities with "+peers.size()+" colleagues.";
                case "mentoring" -> {
                    var novice=peers.stream().filter(p->p.fatigue<75
                            &&WorkplaceHierarchy.tenure(p,s.day)<14
                            &&WorkplaceHierarchy.skill(t,p)<WorkplaceHierarchy.skill(t,lead))
                            .min(Comparator.<TownLife.Citizen>comparingInt(p->WorkplaceHierarchy.skill(t,p))
                                    .thenComparing(p->p.id)).orElse(null);
                    if(novice==null)continue;
                    // At most one learner per team/day. Never exceed the mentor's skill.
                    EducationLife.learn(t,novice,EducationLife.subject(novice.occupation),1);
                    event=lead.name+" coached "+novice.name+" on "+novice.occupation+" duties.";
                }
                case "review" -> event=lead.name+" reviewed the team's work checklist with "+peers.size()+" colleagues.";
                default -> throw new IllegalStateException(phase);
            }
            t.industryStocks.put(key,s.day);
            TownLife.publish(t,s,lead.id,lead.place,event,truth,seen);
        }
    }
}
