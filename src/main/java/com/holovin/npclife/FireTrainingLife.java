package com.holovin.npclife;

import java.util.List;

/** Abstract vocational coursework, not real emergency response instruction. */
final class FireTrainingLife {
    private FireTrainingLife(){}
    static final String JOB="firefighter", SUBJECT="fire-service";
    static final int FEE=5, WAGE=18;
    static final List<String> MODULES=List.of("Safety and teamwork / Sicurezza e squadra","Equipment familiarization / Attrezzatura","Supervised readiness / Preparazione");
    private static String key(TownLife.Town t){return "fire-training:"+t.player.incarnation+":modules";}
    static int completed(TownLife.Town t){return Math.min(MODULES.size(),t.industryStocks.getOrDefault(key(t),0));}
    static boolean qualified(TownLife.Town t){return completed(t)==MODULES.size();}
    static boolean canTrain(TownLife.Town t,WorldState s){
        if(!t.player.alive||RebirthLife.active(t)||JusticeLife.confined(t)||t.player.travelRemaining>0||t.player.cycling||qualified(t)||t.player.money<FEE)return false;
        int age=PassportLife.visitorAge(t,t.player.incarnation,s.day);
        return (age<0||age>=18)&&s.minute>=480&&s.minute+30<=1080
                &&t.player.place==(completed(t)<2?TownLife.Place.SCHOOL:FireServiceLife.DEPOT);
    }
    static String train(TownLife.Town t,WorldState s){
        if(!canTrain(t,s))throw new IllegalArgumentException("Training unavailable. First two modules at school, final module at fire base, 08:00–18:00.");
        int index=completed(t);
        t.player.money-=FEE;t.employerFunds.merge(index<2?"school":"city-maintenance",FEE,Math::addExact);
        t.industryStocks.put(key(t),index+1);
        return "Completed "+MODULES.get(index)+". "+(qualified(t)?"Firefighter qualification issued.":"Progress "+(index+1)+"/"+MODULES.size()+".");
    }
}
