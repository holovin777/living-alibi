package com.holovin.npclife;

/** Contracts follow the actual job and cash transfers. Existing saves are
 * grandfathered as permanent; no new wage or tax is invented here. */
final class EmploymentContractLife {
    private EmploymentContractLife(){}
    static final int PROBATION_DAYS=7, TERM_DAYS=30, PROMOTION_SHIFTS=10;
    private static final String PREFIX="employment-contract:";
    private static String key(TownLife.Town t,String f){return PREFIX+"visitor:"+t.player.incarnation+":"+f;}
    private static String key(TownLife.Citizen c,String f){return PREFIX+"citizen:"+c.id+":"+f;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static void clearVisitor(TownLife.Town t){
        for(String f:new String[]{"start","end","probation","shifts","paid","last-pay-day","last-pay"})set(t,key(t,f),0);
    }
    static void signVisitor(TownLife.Town t,int day){
        clearVisitor(t);
        set(t,key(t,"start"),day);set(t,key(t,"end"),Math.addExact(day,TERM_DAYS));
        set(t,key(t,"probation"),Math.addExact(day,PROBATION_DAYS));
    }
    static void recordPay(TownLife.Town t,int day,int wage){
        if(t.player.jobId==null||wage<=0)return;
        if(get(t,key(t,"start"))==0)set(t,key(t,"start"),day);
        set(t,key(t,"shifts"),Math.addExact(get(t,key(t,"shifts")),1));
        set(t,key(t,"paid"),Math.addExact(get(t,key(t,"paid")),wage));
        set(t,key(t,"last-pay-day"),day);set(t,key(t,"last-pay"),wage);
        if(get(t,key(t,"shifts"))>=PROMOTION_SHIFTS&&day>=get(t,key(t,"probation")))set(t,key(t,"end"),0);
    }
    static void signCitizen(TownLife.Town t,TownLife.Citizen c,int day){
        set(t,key(c,"start"),day);set(t,key(c,"end"),Math.addExact(day,TERM_DAYS));
        set(t,key(c,"probation"),Math.addExact(day,PROBATION_DAYS));set(t,key(c,"ended"),0);
    }
    static void endCitizen(TownLife.Town t,TownLife.Citizen c,int day){
        set(t,key(c,"ended"),day);set(t,key(c,"end"),0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute==0){
            int end=get(t,key(t,"end"));
            if(!RebirthLife.active(t)&&t.player.alive&&t.player.jobId!=null&&end>0&&s.day>=end&&
                    !(FreightLife.active(t)&&!FreightLife.automated(t))){
                t.player.jobId=null;
                set(t,key(t,"end"),0);
                set(t,key(t,"ended"),s.day);
            }
        }
        if(s.minute!=390)return;
        for(var c:t.people.values()){
            if(c.generationIndex<=0)continue;
            if(!c.alive||!c.employed||!DemographyLife.workingAge(c)){
                if(get(t,key(c,"start"))>0&&get(t,key(c,"ended"))==0)endCitizen(t,c,s.day);
                continue;
            }
            if(get(t,key(c,"start"))==0||get(t,key(c,"ended"))>0){
                // Older employment and special job assignments are grandfathered.
                set(t,key(c,"start"),s.day);set(t,key(c,"end"),0);set(t,key(c,"probation"),0);set(t,key(c,"ended"),0);
            }else if(get(t,key(c,"end"))>0&&s.day>=get(t,key(c,"end"))){
                // If an NPC retained the position through its fixed term,
                // the existing employment model has accepted the renewal.
                set(t,key(c,"end"),0);
            }
        }
    }
    private static CareerLife.Offer workplace(TownLife.Town t){
        return CareerLife.offers().stream().filter(o->o.id().equals(t.player.jobId)).findFirst().orElse(null);
    }
    static String resign(TownLife.Town t,int day){
        if(RebirthLife.active(t))throw new IllegalArgumentException("Visitor contract service only.");
        TransportLife.ready(t.player);
        var o=workplace(t);
        if(o==null||t.player.place!=o.workplace()||FreightLife.active(t)&&!FreightLife.automated(t))
            throw new IllegalArgumentException("Visit your workplace and finish active freight deliveries before resigning.");
        t.player.jobId=null;set(t,key(t,"ended"),day);set(t,key(t,"end"),0);
        return "Employment ended voluntarily.";
    }
    record Report(boolean active,String status,int startDay,int endDay,int paidShifts,int totalPaid,int lastPayDay,int lastPay,boolean canResign){}
    static Report report(TownLife.Town t,int day){
        var o=workplace(t);
        boolean active=!RebirthLife.active(t)&&t.player.alive&&o!=null;
        int end=get(t,key(t,"end")),probation=get(t,key(t,"probation"));
        String status=!active?"UNEMPLOYED":end==0?"PERMANENT":day<probation?"PROBATION":"FIXED_TERM";
        return new Report(active,status,get(t,key(t,"start")),end,get(t,key(t,"shifts")),get(t,key(t,"paid")),
                get(t,key(t,"last-pay-day")),get(t,key(t,"last-pay")),active&&t.player.travelRemaining==0&&
                t.player.place==o.workplace()&&!(FreightLife.active(t)&&!FreightLife.automated(t)));
    }
}
