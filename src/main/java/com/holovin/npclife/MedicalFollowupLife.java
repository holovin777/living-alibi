package com.holovin.npclife;

/** Fictional medical follow-up: private assessment, finite course and gradual
 * recovery. These abstract game supplies are not real medications or advice. */
final class MedicalFollowupLife {
    private MedicalFollowupLife(){}
    static int get(TownLife.Town t,TownLife.Citizen c,String field){return EducationLife.get(t,c,"medical-"+field);}
    static void set(TownLife.Town t,TownLife.Citizen c,String field,int n){EducationLife.set(t,c,"medical-"+field,n);}
    record View(int lastExamDay,int assessedHealth,int prescribedUntil,int remainingSupplies,int completedDays,
                boolean canExam,boolean canCollect,boolean canRecover){}
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        boolean clinic=RebirthLife.awakePresent(c)&&c.place==TownLife.Place.CLINIC&&!RebirthLife.atHome(c)&&minute>=480;
        int prescription=get(t,c,"prescription");
        int supplies=get(t,c,"supplies");
        boolean needs=c.health<100||EmploymentLife.sick(c,day);
        return new View(get(t,c,"exam-day"),get(t,c,"assessed-health"),prescription,supplies,get(t,c,"completed"),
                clinic&&minute+20<=1140&&c.money>=12&&get(t,c,"exam-day")!=day,
                clinic&&minute+10<=1140&&c.money>=9&&prescription>=day&&supplies==0&&get(t,c,"collected")!=get(t,c,"exam-day")&&needs,
                RebirthLife.atHome(c)&&supplies>0&&get(t,c,"last-recovery")<day&&needs&&c.water>=2&&c.food>=1);
    }
    static RebirthLife.HomeAction act(TownLife.Town t,TownLife.Citizen c,int day,int minute,String action){
        var v=view(t,c,day,minute);
        switch(action){
            case "exam" -> {
                if(!v.canExam())throw new IllegalArgumentException("Medical assessment is unavailable.");
                int funds=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),12);
                c.money-=12;t.employerFunds.put("city-maintenance",funds);
                set(t,c,"exam-day",day);set(t,c,"assessed-health",c.health);
                set(t,c,"prescription",c.health<100||EmploymentLife.sick(c,day)?Math.addExact(day,3):0);
                return new RebirthLife.HomeAction(20,"Completed a private health assessment.");
            }
            case "collect" -> {
                if(!v.canCollect())throw new IllegalArgumentException("A current assessment and available funds are required.");
                int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),9);
                c.money-=9;t.employerFunds.put("shop-assistant",funds);
                set(t,c,"supplies",3);set(t,c,"collected",v.lastExamDay());set(t,c,"completed",0);
                return new RebirthLife.HomeAction(10,"Collected a three-day recovery kit.");
            }
            case "recover" -> {
                if(!v.canRecover())throw new IllegalArgumentException("Rest at home with a recovery kit, food and water.");
                c.food--;c.water--;set(t,c,"supplies",v.remainingSupplies()-1);
                set(t,c,"last-recovery",day);set(t,c,"completed",v.completedDays()+1);
                c.health=Math.min(100,c.health+4);c.fatigue=Math.max(0,c.fatigue-8);
                c.hunger=Math.max(0,c.hunger-25);c.thirst=Math.max(0,c.thirst-25);
                return new RebirthLife.HomeAction(60,"Completed a day of supported recovery.");
            }
            default -> throw new IllegalArgumentException("Unknown medical follow-up action.");
        }
    }
}
