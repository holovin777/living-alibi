package com.holovin.npclife;

/** Optional visitor crew participation; abstract in-game tasks, not operational firefighting instruction. */
final class FireDutyLife {
    private FireDutyLife(){}
    static final int BONUS=3;
    private static String key(TownLife.Town t,String field){return "fire-duty:"+t.player.incarnation+":"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    private static void set(TownLife.Town t,String field,int n){t.industryStocks.put(key(t,field),n);}
    static boolean ready(TownLife.Town t){
        var p=t.player;
        return p.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&p.travelRemaining==0&&!p.cycling
            &&FireTrainingLife.JOB.equals(p.jobId)&&FireTrainingLife.qualified(t)&&p.health>=60&&p.fatigue<85;
    }
    static boolean canStandby(TownLife.Town t,WorldState s){
        return ready(t)&&t.player.place==FireServiceLife.DEPOT&&get(t,"until")<=FireServiceLife.now(s);
    }
    static String standby(TownLife.Town t,WorldState s){
        if(!canStandby(t,s))throw new IllegalArgumentException("Standby unavailable.");
        set(t,"until",FireServiceLife.now(s)+120);
        return "Ready for dispatch / In pronta disponibilita for 120 game minutes.";
    }
    static boolean onStandby(TownLife.Town t,WorldState s){return ready(t)&&t.player.place==FireServiceLife.DEPOT&&get(t,"until")>FireServiceLife.now(s);}
    static boolean canJoin(TownLife.Town t,WorldState s){
        int incident=FireServiceLife.get(t,"started");
        return onStandby(t,s)&&incident>0&&get(t,"joined")!=incident
            &&"EN_ROUTE".equals(FireServiceLife.view(t,s).status());
    }
    record Journey(String message,int minutes){}
    static Journey join(TownLife.Town t,WorldState s){
        if(!canJoin(t,s))throw new IllegalArgumentException("No departing crew available.");
        int minutes=Math.max(1,FireServiceLife.get(t,"arrive")-FireServiceLife.now(s));
        set(t,"joined",FireServiceLife.get(t,"started"));set(t,"until",0);
        t.player.place=FireServiceLife.site(t);t.player.destination=t.player.place;
        t.player.fatigue=Math.min(100,t.player.fatigue+3);
        return new Journey("Joined emergency crew / Squadra di intervento. Arriving at scene.",minutes);
    }
    static boolean canAssist(TownLife.Town t,WorldState s){
        int incident=FireServiceLife.get(t,"started");
        return ready(t)&&incident>0&&get(t,"joined")==incident&&get(t,"assisted")!=incident
            &&t.player.place==FireServiceLife.site(t)&&"FIGHTING".equals(FireServiceLife.view(t,s).status());
    }
    static Journey assist(TownLife.Town t,WorldState s){
        if(!canAssist(t,s))throw new IllegalArgumentException("No assigned scene task available.");
        set(t,"assisted",FireServiceLife.get(t,"started"));
        set(t,"completed",Math.addExact(get(t,"completed"),1));
        t.player.fatigue=Math.min(100,t.player.fatigue+5);
        int now=FireServiceLife.now(s);
        FireServiceLife.set(t,"end",Math.max(now+1,FireServiceLife.get(t,"end")-5));
        int bonus=Math.min(BONUS,Math.max(0,t.employerFunds.getOrDefault(FireServiceLife.ACCOUNT,0)));
        t.employerFunds.merge(FireServiceLife.ACCOUNT,-bonus,Integer::sum);
        t.player.money=Math.addExact(t.player.money,bonus);
        return new Journey("Crew support completed / Supporto squadra completato. EUR "+bonus+" bonus.",5);
    }
    record View(boolean standby,boolean assigned,int completed){}
    static View view(TownLife.Town t,WorldState s){
        int active=FireServiceLife.get(t,"started");
        return new View(onStandby(t,s),active>0&&get(t,"joined")==active,get(t,"completed"));
    }
}
