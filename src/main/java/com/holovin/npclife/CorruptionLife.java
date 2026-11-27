package com.holovin.npclife;

/** Fictional municipal integrity simulation. The private transaction record is
 * evidence, not a public accusation. Only a completed audit and court review
 * create a public conviction. No autonomous offence for player-owned identities.
 */
final class CorruptionLife {
    private CorruptionLife(){}
    private static final String P="corruption:";
    private static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(P+f,0);}
    private static void set(TownLife.Town t,String f,int value){t.industryStocks.put(P+f,value);}
    static boolean canMisconduct(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=18&&ElectionLife.get(t,ElectionLife.MAYOR)==c.generationIndex&&get(t,"stage")==0
            &&MunicipalLife.balance(t,MunicipalLife.CITY)>=MunicipalLife.RESERVE+30
            &&ElectionLife.atComune(c,s.minute)&&!NpcJusticeLife.confined(t,c,s);
    }
    static void offense(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canMisconduct(t,c,s))throw new IllegalArgumentException("No available municipal action.");
        int balance=MunicipalLife.balance(t,MunicipalLife.CITY),wallet=Math.addExact(c.money,30);
        set(t,"serial",Math.addExact(get(t,"serial"),1));
        set(t,"subject",c.generationIndex);set(t,"amount",30);set(t,"day",s.day);set(t,"stage",1);
        // Abstract game action only. The money exists and the audit record persists.
        t.employerFunds.put(MunicipalLife.CITY,balance-30);c.money=wallet;
    }
    static boolean canAudit(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return get(t,"stage")==1&&s.day>get(t,"day")&&c!=null&&c.alive&&c.generationIndex!=get(t,"subject")
            &&PoliceLife.detective(c)&&ElectionLife.atComune(c,s.minute)&&!EmploymentLife.sick(c,s.day)
            &&!NpcJusticeLife.confined(t,c,s);
    }
    static void audit(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canAudit(t,c,s))throw new IllegalArgumentException("An available detective and a pending audit are required.");
        set(t,"auditor",c.generationIndex);set(t,"review-day",s.day+2);set(t,"stage",2);
    }
    static String act(TownLife.Town t,WorldState s,String action){
        var c=RebirthLife.requireAdult(t);
        return switch(action){
            case "offense" -> {offense(t,c,s);yield "A fictional offence was committed. It is subject to investigation.";}
            case "audit" -> {audit(t,c,s);yield "Budget records reviewed. Evidence submitted for court review.";}
            default -> throw new IllegalArgumentException("Unknown integrity action.");
        };
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute==600&&get(t,"stage")==0&&s.day%7==0){
            var mayor=DemographyLife.byIndex(t,ElectionLife.get(t,ElectionLife.MAYOR));
            if(mayor!=null&&!RelationshipBoundaryLife.controlled(t,mayor)&&canMisconduct(t,mayor,s)
                    &&Math.floorMod(mayor.personalSeed+s.day*31L,100)<Math.max(2,(60-mayor.carefulness)/4))
                offense(t,mayor,s);
        }
        if(s.minute==660&&get(t,"stage")==1){
            t.people.values().stream().filter(c->!RelationshipBoundaryLife.controlled(t,c)&&canAudit(t,c,s))
                .findFirst().ifPresent(c->audit(t,c,s));
        }
        if(s.minute!=720||get(t,"stage")!=2||s.day<get(t,"review-day"))return;
        var subject=DemographyLife.byIndex(t,get(t,"subject"));
        if(subject!=null&&subject.alive){
            int amount=get(t,"amount"),paid=Math.min(subject.money,amount);
            subject.money-=paid;
            t.employerFunds.merge(MunicipalLife.CITY,paid,Math::addExact);
            subject.unpaidBills=Math.addExact(subject.unpaidBills,amount-paid);
            if(ElectionLife.get(t,ElectionLife.MAYOR)==subject.generationIndex){
                ElectionLife.set(t,ElectionLife.MAYOR,0);ElectionLife.set(t,ElectionLife.OWNER,0);
                ElectionLife.set(t,ElectionLife.POLICY,ElectionLife.BALANCED);
            }
            NpcJusticeLife.corruptionConviction(t,s,subject,get(t,"serial"));
            ArchiveLife.record(t,"corruption-verdict:"+get(t,"serial"),s.day,s.minute,"Municipality",
                "A fictional court convicted "+subject.name+" of misuse of public funds following an audit. Restitution ordered.");
        }
        set(t,"stage",0);set(t,"subject",0);set(t,"auditor",0);
    }
}
