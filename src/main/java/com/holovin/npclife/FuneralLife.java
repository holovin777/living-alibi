package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** Persistent municipal funeral register. No fabricated attendance or religion. */
final class FuneralLife {
    private FuneralLife(){}
    static final String PROVIDER="funeral-services";
    static final int BASIC=60, FAMILY=180, DELAY=3;
    private static String key(TownLife.Citizen c,String field){return "funeral:"+c.id+":"+field;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String field){return t.industryStocks.getOrDefault(key(c,field),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String field,int value){t.industryStocks.put(key(c,field),value);}
    private static boolean directFamily(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        if(a.generationIndex<=0||b.generationIndex<=0)return false;
        for(String f:List.of("parent-a","parent-b")){
            if(DemographyLife.get(t,a,f)==b.generationIndex||DemographyLife.get(t,b,f)==a.generationIndex)return true;
            int p=DemographyLife.get(t,a,f);
            if(p>0&&(p==DemographyLife.get(t,b,"parent-a")||p==DemographyLife.get(t,b,"parent-b")))return true;
        }
        return DemographyLife.get(t,a,"partner")==b.generationIndex||DemographyLife.get(t,b,"partner")==a.generationIndex;
    }
    static boolean grieving(TownLife.Town t,TownLife.Citizen c,int day){return get(t,c,"grief-until")>day;}
    static int griefDays(TownLife.Town t,TownLife.Citizen living,TownLife.Citizen dead){
        var r=t.existingRelationship(living.id,dead.id);
        if(r==null&&living.generationIndex==0&&dead.generationIndex==0&&
                (living.id.equals("marco")&&dead.id.equals("elena")||living.id.equals("elena")&&dead.id.equals("marco")))r=t.relationship;
        boolean family=directFamily(t,living,dead);
        int closeness=r==null?(family?65:0):(r.affinity+r.trust)/2;
        if(!family&&(r==null||r.encounters<2||closeness<65))return 0;
        if(closeness<30)return 0;
        return 3+closeness/4+living.sociability/10;
    }
    /** 0 = unspecified/civil default, 1 = explicitly requested religious ceremony. */
    static void setReligiousPreference(TownLife.Town t,TownLife.Citizen c,boolean religious){
        if(!c.alive)throw new IllegalArgumentException("Only a living resident can record a preference.");
        put(t,c,"religious",religious?1:2);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=360)return;
        for(var dead:t.people.values()){
            if(dead.alive||get(t,dead,"buried")>0)continue;
            var death=t.archive.get("death:"+dead.id);
            if(death==null){
                // Legacy deaths without a timestamp receive an observation, not an invented death date.
                ArchiveLife.record(t,"death-observed:"+dead.id,s.day,s.minute,"Population",dead.name+" was found in the deceased resident register.");
            }
            int due=get(t,dead,"due");
            if(due==0){
                put(t,dead,"due",s.day+DELAY);
                // A previously archived death must not trigger fresh grief years later.
                if(death!=null&&s.day-death.day()<=1){
                    for(var c:t.people.values())if(c.alive){
                        int days=griefDays(t,c,dead);
                        if(days>0)put(t,c,"grief-until",Math.max(get(t,c,"grief-until"),s.day+days));
                    }
                }
                continue;
            }
            if(s.day<due)continue;
            bury(t,dead,s);
        }
    }
    private static void bury(TownLife.Town t,TownLife.Citizen dead,WorldState s){
        var family=t.people.values().stream().filter(c->c.alive&&c.age>=18&&c!=RebirthLife.child(t)&&directFamily(t,c,dead))
                .sorted(Comparator.comparing(c->c.id)).toList();
        // Protect a household's essentials and never spend the controlled resident's money automatically.
        long available=family.stream().mapToLong(c->Math.max(0,c.money-MunicipalLife.PROTECTED)).sum();
        int price=available>=FAMILY?FAMILY:BASIC;
        int paid=0;
        for(var c:family){
            int amount=Math.min(price-paid,Math.max(0,c.money-MunicipalLife.PROTECTED));
            c.money-=amount;paid+=amount;
            if(paid==price)break;
        }
        int city=Math.min(price-paid,Math.max(0,MunicipalLife.balance(t,MunicipalLife.CITY)-MunicipalLife.RESERVE));
        t.employerFunds.merge(MunicipalLife.CITY,-city,Integer::sum);
        paid+=city;
        t.employerFunds.merge(PROVIDER,paid,Math::addExact);
        put(t,dead,"buried",s.day);put(t,dead,"paid",paid);put(t,dead,"unfunded",price-paid);
        int preference=get(t,dead,"religious");
        boolean religious=preference==1||preference==0&&ConvictionLife.belief(t,dead).religious;
        String ceremony=religious?"Religious":"Civil";
        ArchiveLife.record(t,"grave:"+dead.id,s.day,s.minute,"Cemetery",
                dead.name+", age "+dead.age+". "+ceremony+" burial; "+(price==FAMILY?"family-funded ceremony":"basic burial")+
                ". Paid EUR "+paid+"; unfunded public-service cost EUR "+(price-paid)+".");
    }
    record Grave(String residentId,String name,int birthDay,int recordedDeathDay,int age,int burialDay,int paid,int unfunded){}
    static List<Grave> graves(TownLife.Town t){
        return t.people.values().stream().filter(c->!c.alive&&get(t,c,"buried")>0)
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->get(t,c,"buried")).thenComparing(c->c.id))
                .map(c->{var death=t.archive.get("death:"+c.id);return new Grave(c.id,c.name,DemographyLife.get(t,c,"born"),
                        death==null?0:death.day(),c.age,get(t,c,"buried"),get(t,c,"paid"),get(t,c,"unfunded"));}).toList();
    }
}
