package com.holovin.npclife;

/** Compressed fictional court process for newly confirmed NPC cases, never an accusation alone. */
final class NpcJusticeLife {
    private NpcJusticeLife(){}
    private static final String PREFIX="combat:npc-justice:";
    private static int time(WorldState s){return Math.toIntExact(((long)s.day-1)*1440+s.minute+1);}
    private static String caseKey(InvestigationLife.CaseFile c){return PREFIX+"case:"+c.id;}
    private static String until(TownLife.Citizen c){return PREFIX+"until:"+c.id;}
    private static String count(TownLife.Citizen c){return PREFIX+"convictions:"+c.id;}
    private static boolean eligible(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&c.alive&&c.generationIndex>0&&c.age>=18&&!RebirthLife.linkedAdult(t,c);
    }
    static void queue(TownLife.Town t,InvestigationLife.CaseFile c){
        if(c.solved&&eligible(t,t.people.get(c.offenderId)))t.industryStocks.putIfAbsent(caseKey(c),1);
    }
    static boolean confined(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=18&&t.industryStocks.getOrDefault(until(c),0)>time(s);
    }
    static void custody(TownLife.Citizen c){
        c.place=TownLife.Place.OFFICE;c.destination=c.place;c.travelRemaining=0;
        c.activity="IN_CUSTODY";
        c.hunger=Math.min(c.hunger,45);c.thirst=Math.min(c.thirst,40);c.fatigue=Math.min(c.fatigue,50);
    }
    static int convictions(TownLife.Town t,TownLife.Citizen c){return t.industryStocks.getOrDefault(count(c),0);}
    static void vehicleConviction(TownLife.Town t,WorldState s,TownLife.Citizen offender,int incident){
        if(!eligible(t,offender))return;
        String key=PREFIX+"vehicle-case:"+incident;
        if(t.industryStocks.putIfAbsent(key,1)!=null)return;
        int now=time(s);
        t.industryStocks.put(until(offender),Math.addExact(Math.max(now,t.industryStocks.getOrDefault(until(offender),0)),2880));
        t.industryStocks.merge(count(offender),1,Math::addExact);
        custody(offender);
        ArchiveLife.record(t,"npc-vehicle-court:"+incident,s.day,s.minute,"Public safety",
                "A fictional court imposed a custodial sentence in a confirmed vehicle theft case.");
    }
    static void corruptionConviction(TownLife.Town t,WorldState s,TownLife.Citizen offender,int incident){
        if(offender==null||!offender.alive||offender.age<18)return;
        String key=PREFIX+"corruption-case:"+incident;
        if(t.industryStocks.putIfAbsent(key,1)!=null)return;
        t.industryStocks.put(until(offender),Math.addExact(Math.max(time(s),t.industryStocks.getOrDefault(until(offender),0)),4320));
        t.industryStocks.merge(count(offender),1,Math::addExact);
        custody(offender);
    }
    static void propertyConviction(TownLife.Town t,WorldState s,TownLife.Citizen offender,int incident){
        if(!eligible(t,offender))return;
        String key=PREFIX+"property-case:"+incident;
        if(t.industryStocks.putIfAbsent(key,1)!=null)return;
        int now=time(s);
        t.industryStocks.put(until(offender),Math.addExact(Math.max(now,t.industryStocks.getOrDefault(until(offender),0)),2880));
        t.industryStocks.merge(count(offender),1,Math::addExact);
        custody(offender);
        ArchiveLife.record(t,"npc-property-court:"+incident,s.day,s.minute,"Public safety",
                "A fictional court imposed a custodial sentence in a confirmed property crime.");
    }
    static void tick(TownLife.Town t,WorldState s){
        int now=time(s);
        for(var c:t.people.values()){
            if(confined(t,c,s))custody(c);
            else if("IN_CUSTODY".equals(c.activity)){
                c.activity="FREE_TIME";
                if(c.alive)ArchiveLife.record(t,"npc-release:"+c.id+":"+now,s.day,s.minute,"Public safety",
                        "A resident completed a fictional custodial sentence.");
            }
        }
        if(s.minute%10!=0)return;
        for(var c:t.cases.values()){
            int due=t.industryStocks.getOrDefault(caseKey(c),0);
            if(due==0||due==2)continue;
            var offender=t.people.get(c.offenderId);
            if(!c.solved||!eligible(t,offender)){t.industryStocks.put(caseKey(c),2);continue;}
            if(due==1){t.industryStocks.put(caseKey(c),Math.addExact(now,1440));continue;}
            if(due>now)continue;
            t.industryStocks.put(caseKey(c),2);
            int end=Math.addExact(Math.max(now,t.industryStocks.getOrDefault(until(offender),0)),2880);
            t.industryStocks.put(until(offender),end);
            t.industryStocks.merge(count(offender),1,Math::addExact);
            custody(offender);
            ArchiveLife.record(t,"npc-court:"+c.id,s.day,s.minute,"Public safety",
                    "A fictional court imposed a custodial sentence in a confirmed theft case.");
        }
    }
}
