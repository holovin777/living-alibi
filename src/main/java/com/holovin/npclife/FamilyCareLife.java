package com.holovin.npclife;

/** Practical support in a shared household, not remote aid or a medical cure. */
final class FamilyCareLife {
    private FamilyCareLife(){}
    private static String key(TownLife.Citizen c){return "family-care:"+c.id+":last-day";}
    static boolean help(TownLife.Town t,TownLife.Citizen donor,TownLife.Citizen patient,int day){
        if(donor==patient||!donor.alive||!patient.alive||donor.generationIndex<=0||
                patient.generationIndex<=0||donor.age<18||patient.age<18||
                RebirthLife.linkedAdult(t,donor)||RebirthLife.linkedAdult(t,patient)||
                donor.travelRemaining>0||patient.travelRemaining>0||donor.place!=donor.home||
                patient.place!=patient.home||donor.home!=patient.home||
                !donor.homeUnit.equals(patient.homeUnit)||!donor.currentSpace().equals(donor.homeUnit)||
                !patient.currentSpace().equals(patient.homeUnit)||
                !PeerAidLife.family(t,donor,patient)||EmploymentLife.sick(donor,day)||
                "SLEEPING".equals(donor.activity)||"SLEEPING".equals(patient.activity)||
                donor.health<60||donor.fatigue>=75||
                !(patient.age>=67||EmploymentLife.sick(patient,day))||
                t.industryStocks.getOrDefault(key(donor),0)>=day)return false;
        boolean food=patient.food<2&&donor.food>=3&&donor.hunger<65;
        boolean water=patient.water<2&&donor.water>=3&&donor.thirst<65;
        if(!food&&!water)return false;
        if(food){donor.food--;patient.food++;}
        if(water){donor.water--;patient.water++;}
        donor.fatigue=Math.min(100,donor.fatigue+2);
        t.industryStocks.put(key(donor),day);
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=420&&s.minute!=1140)return;
        var atHome=t.people.values().stream().filter(c->c.alive&&c.place==c.home&&c.travelRemaining==0).toList();
        for(var patient:atHome){
            if(patient.age<67&&!EmploymentLife.sick(patient,s.day))continue;
            if(patient.food>=2&&patient.water>=2)continue;
            for(var donor:atHome)if(help(t,donor,patient,s.day))break;
        }
    }
}
