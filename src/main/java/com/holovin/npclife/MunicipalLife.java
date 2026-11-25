package com.holovin.npclife;

import java.util.List;

/** Weekly municipal budget cycle. All receipts and grants transfer existing cash. */
final class MunicipalLife {
    private MunicipalLife(){}
    static final String CITY="city-maintenance", CLINIC="municipal-clinic", CARE="medical-services", MAINTENANCE="municipal-maintenance";
    static final String LAST="comune:last-week", TAX="comune:tax", EDUCATION="comune:education",
            HEALTH="comune:health", UPKEEP="comune:upkeep", AID="comune:aid";
    static final int PROTECTED=100, RESERVE=40, MAX_TAX=15;
    static final List<String> KEYS=List.of(LAST,TAX,EDUCATION,HEALTH,UPKEEP,AID);
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static void ensure(TownLife.Town t){for(String k:KEYS)t.industryStocks.putIfAbsent(k,0);}
    static int balance(TownLife.Town t,String id){return t.employerFunds.getOrDefault(id,0);}
    static int levy(int money){return Math.min(MAX_TAX,Math.max(0,money-PROTECTED)/20);}
    static void receiveTax(TownLife.Town t,int amount){
        int city=Math.addExact(balance(t,CITY),amount),total=Math.addExact(get(t,TAX),amount);
        t.employerFunds.put(CITY,city);set(t,TAX,total);
    }
    static boolean grant(TownLife.Town t,String target,int amount,String metric){
        if(balance(t,CITY)-RESERVE<amount)return false;
        int destination=Math.addExact(balance(t,target),amount),total=Math.addExact(get(t,metric),amount);
        t.employerFunds.put(CITY,balance(t,CITY)-amount);
        t.employerFunds.put(target,destination);set(t,metric,total);return true;
    }
    static void upkeep(TownLife.Town t,String id,String material,int improvement){
        var o=t.technicalOrders.get(id);
        if(o==null||o.phase!=TechnicalWorkLife.Phase.COMPLETED||o.condition<70||o.condition>=100||
                t.technicalMaterials.getOrDefault(material,0)<1)return;
        if(!grant(t,MAINTENANCE,4,UPKEEP))return;
        t.technicalMaterials.merge(material,-1,Integer::sum);
        o.condition=Math.min(100,o.condition+improvement);
    }
    static int medicalAid(TownLife.Town t,TownLife.Citizen c,int bill){
        if(c.money>=PROTECTED||bill<=0)return 0;
        int n=Math.min(10,Math.min(bill,balance(t,CLINIC)));
        if(n==0)return 0;
        int care=Math.addExact(balance(t,CARE),n),aid=Math.addExact(get(t,AID),n);
        t.employerFunds.put(CLINIC,balance(t,CLINIC)-n);
        t.employerFunds.put(CARE,care);set(t,AID,aid);
        return n;
    }
    static void tick(TownLife.Town t,int day,int minute){
        int week=(day-1)/7;
        if(minute!=360||week<=get(t,LAST))return;
        int opening=balance(t,CITY);
        set(t,LAST,week);
        if(t.player.alive){
            int n=levy(t.player.money);receiveTax(t,n);t.player.money-=n;
        }
        for(var c:t.people.values())if(c.alive&&c.generationIndex>0&&!DemographyLife.child(c)){
            int n=levy(c.money);receiveTax(t,n);c.money-=n;
        }
        upkeep(t,"water-pump","spare-parts",8);
        upkeep(t,"street-lights","steel",5);
        grant(t,"school",6,EDUCATION);
        grant(t,"library",4,EDUCATION);
        grant(t,CLINIC,8,HEALTH);
        int closing=balance(t,CITY);
        if(opening>RESERVE&&closing<=RESERVE)ArchiveLife.record(t,"budget-low:"+week,day,minute,"Municipality","Municipal cash reached its minimum reserve; discretionary grants paused.");
        else if(opening<=RESERVE&&closing>RESERVE)ArchiveLife.record(t,"budget-recovery:"+week,day,minute,"Municipality","Municipal cash recovered above its minimum reserve.");
    }
    record Report(int budget,int taxes,int education,int healthcare,int maintenance,int medicalAid,int schoolFunds,int libraryFunds,int clinicFunds,int minimumReserve,int protectedCash) {}
    static Report report(TownLife.Town t){
        return new Report(balance(t,CITY),get(t,TAX),get(t,EDUCATION),get(t,HEALTH),get(t,UPKEEP),get(t,AID),
                balance(t,"school"),balance(t,"library"),balance(t,CLINIC),RESERVE,PROTECTED);
    }
}
