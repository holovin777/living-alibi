package com.holovin.npclife;

/** Contracts follow the actual job and cash transfers. Existing saves are
 * grandfathered as permanent. Fiscal registration enables simplified,
 * conserved payroll deductions, independent of the weekly wealth levy. */
final class EmploymentContractLife {
    private EmploymentContractLife(){}
    static final int PROBATION_DAYS=7, TERM_DAYS=30, PROMOTION_SHIFTS=10;
    static final String PENSION_ACCOUNT="pension-fund";
    private static final String PREFIX="employment-contract:";
    private static String key(TownLife.Town t,String f){return PREFIX+"visitor:"+t.player.incarnation+":"+f;}
    private static String key(TownLife.Citizen c,String f){return PREFIX+"citizen:"+c.id+":"+f;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static int releaseTfr(TownLife.Town t){
        int escrow=get(t,key(t,"tfr"));
        if(escrow==0)return 0;
        int wallet=Math.addExact(t.player.money,escrow);
        int total=Math.addExact(get(t,key(t,"tfr-released")),escrow);
        t.player.money=wallet;
        set(t,key(t,"tfr"),0);set(t,key(t,"tfr-released"),total);
        return escrow;
    }
    static void clearVisitor(TownLife.Town t){
        releaseTfr(t);
        for(String f:new String[]{"start","end","probation","shifts","paid","last-pay-day","last-pay",
                "last-net","last-tax","last-pension","last-tfr","net-total","tax-total","ended"})set(t,key(t,f),0);
    }
    static void signVisitor(TownLife.Town t,int day){
        clearVisitor(t);
        set(t,key(t,"start"),day);set(t,key(t,"end"),Math.addExact(day,TERM_DAYS));
        set(t,key(t,"probation"),Math.addExact(day,PROBATION_DAYS));
    }
    /** Caller transfers the gross wage out of the employer or freight escrow.
     * This method is the sole recipient of that wage. Rates and whole-euro
     * rounding are fictional, not Italian payroll or legal advice. */
    static int recordPay(TownLife.Town t,int day,int wage){
        if(wage<=0)throw new IllegalArgumentException("Gross wage must be positive.");
        if(t.player.jobId==null){
            t.player.money=Math.addExact(t.player.money,wage);
            return wage;
        }
        boolean fiscal=CivilRegistryLife.fiscalRegistered(t);
        int tax=fiscal?wage/10:0,pension=fiscal?wage/20:0,tfr=fiscal?wage/20:0;
        int net=wage-tax-pension-tfr;
        int wallet=Math.addExact(t.player.money,net);
        int pensionFund=Math.addExact(t.employerFunds.getOrDefault(PENSION_ACCOUNT,0),pension);
        int pensionTotal=Math.addExact(get(t,key(t,"pension-total")),pension);
        int escrow=Math.addExact(get(t,key(t,"tfr")),tfr);
        int paid=Math.addExact(get(t,key(t,"paid")),wage);
        int netTotal=Math.addExact(get(t,key(t,"net-total")),net);
        int taxTotal=Math.addExact(get(t,key(t,"tax-total")),tax);
        int shifts=Math.addExact(get(t,key(t,"shifts")),1);
        if(tax>0)MunicipalLife.receiveTax(t,tax);
        t.player.money=wallet;
        t.employerFunds.put(PENSION_ACCOUNT,pensionFund);
        set(t,key(t,"pension-total"),pensionTotal);set(t,key(t,"tfr"),escrow);
        if(get(t,key(t,"start"))==0)set(t,key(t,"start"),day);
        set(t,key(t,"shifts"),shifts);set(t,key(t,"paid"),paid);
        set(t,key(t,"net-total"),netTotal);set(t,key(t,"tax-total"),taxTotal);
        set(t,key(t,"last-pay-day"),day);set(t,key(t,"last-pay"),wage);
        set(t,key(t,"last-net"),net);set(t,key(t,"last-tax"),tax);
        set(t,key(t,"last-pension"),pension);set(t,key(t,"last-tfr"),tfr);
        if(shifts>=PROMOTION_SHIFTS&&day>=get(t,key(t,"probation")))set(t,key(t,"end"),0);
        return net;
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
                releaseTfr(t);
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
        int settlement=releaseTfr(t);
        t.player.jobId=null;set(t,key(t,"ended"),day);set(t,key(t,"end"),0);
        return "Employment ended voluntarily. TFR paid: "+settlement+" EUR.";
    }
    record Report(boolean active,String status,int startDay,int endDay,int paidShifts,int totalPaid,int lastPayDay,int lastPay,boolean canResign,
                  boolean fiscalPayroll,int lastNet,int lastTax,int lastPension,int lastTfr,
                  int tfrBalance,int pensionTotal,int tfrReleased){}
    static Report report(TownLife.Town t,int day){
        var o=workplace(t);
        boolean active=!RebirthLife.active(t)&&t.player.alive&&o!=null;
        int end=get(t,key(t,"end")),probation=get(t,key(t,"probation"));
        String status=!active?"UNEMPLOYED":end==0?"PERMANENT":day<probation?"PROBATION":"FIXED_TERM";
        return new Report(active,status,get(t,key(t,"start")),end,get(t,key(t,"shifts")),get(t,key(t,"paid")),
                get(t,key(t,"last-pay-day")),get(t,key(t,"last-pay")),active&&t.player.travelRemaining==0&&
                t.player.place==o.workplace()&&!(FreightLife.active(t)&&!FreightLife.automated(t)),
                CivilRegistryLife.fiscalRegistered(t),
                t.industryStocks.getOrDefault(key(t,"last-net"),get(t,key(t,"last-pay"))),
                get(t,key(t,"last-tax")),get(t,key(t,"last-pension")),get(t,key(t,"last-tfr")),
                get(t,key(t,"tfr")),get(t,key(t,"pension-total")),get(t,key(t,"tfr-released")));
    }
}
