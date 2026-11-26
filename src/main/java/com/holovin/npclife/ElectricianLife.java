package com.holovin.npclife;

import java.util.List;

/** Optional municipal repair work. Initial player orders remain available. */
final class ElectricianLife {
    private ElectricianLife(){}
    static final String JOB="Electrician";
    static final int WAGE=12;
    static boolean ready(TownLife.Town t){
        var o=t.technicalOrders.get("street-lights");
        return o!=null&&o.completedDay>0&&o.phase==TechnicalWorkLife.Phase.OPEN
                &&o.condition<UtilityLife.LIGHTS_MIN_CONDITION&&TechnicalWorkLife.materialsReady(t,o)
                &&t.employerFunds.getOrDefault(StreetCareLife.ACCOUNT,0)>=WAGE;
    }
    static boolean shift(TownLife.Town t,int minute){
        return ready(t)&&minute>=540&&minute<=660;
    }
    static void tick(TownLife.Town t,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(clock.minute!=660||!ready(t))return;
        var worker=t.people.values().stream().filter(c->c.alive&&c.employed&&JOB.equals(c.occupation)
                &&c.presentAt(TownLife.Place.PARK)&&"WORKING".equals(c.activity)
                &&!EmploymentLife.sick(c,clock.day)&&!RebirthLife.linkedAdult(t,c)).findFirst().orElse(null);
        if(worker==null)return;
        var o=t.technicalOrders.get("street-lights");
        int wallet=Math.addExact(worker.money,WAGE);
        t.technicalMaterials.merge(o.materialA,-o.requiredA,Integer::sum);
        if(o.materialB!=null)t.technicalMaterials.merge(o.materialB,-o.requiredB,Integer::sum);
        t.employerFunds.merge(StreetCareLife.ACCOUNT,-WAGE,Integer::sum);
        worker.money=wallet;
        o.condition=Math.min(100,o.condition+40);
        o.phase=TechnicalWorkLife.Phase.COMPLETED;o.completedDay=clock.day;
        TownLife.publish(t,clock,worker.id,worker.place,worker.name+" repaired public street lighting.",events,observations);
    }
}
