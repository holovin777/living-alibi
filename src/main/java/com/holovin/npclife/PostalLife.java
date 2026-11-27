package com.holovin.npclife;

/** First postal route: existing office staff deliver real condominium statements.
 * Private per-recipient counts are persisted but never projected to public UI. */
final class PostalLife {
    private PostalLife(){}
    static final int DELIVERY_MINUTES=15;
    private static final String PREFIX="postal:", PENDING=PREFIX+"pending",
            WORKER=PREFIX+"worker", PROGRESS=PREFIX+"progress",
            DELIVERED=PREFIX+"delivered", RETURNED=PREFIX+"returned";
    private static String queue(TownLife.Citizen c){return PREFIX+"queued:"+c.id;}
    private static String mailbox(TownLife.Citizen c){return PREFIX+"mailbox:"+c.id;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int value){t.industryStocks.put(k,value);}
    static void queueStatement(TownLife.Town t,TownLife.Citizen c){
        if(!c.alive||AddressLife.building(c)!=1)return;
        set(t,queue(c),Math.addExact(get(t,queue(c)),1));
        set(t,PENDING,Math.addExact(get(t,PENDING),1));
    }
    static int deliveredTo(TownLife.Town t,TownLife.Citizen c){return get(t,mailbox(c));}
    private static boolean hours(WorldState s){return (s.day-1)%7<5&&s.minute>=540&&s.minute<960;}
    private static boolean available(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.generationIndex>0&&c.alive&&c.age>=18&&c.age<65&&c.employed&&
                "Office clerk".equals(c.occupation)&&c.workplace==TownLife.Place.OFFICE&&
                c.home!=TownLife.Place.NORTH_RESIDENCES&&!RebirthLife.linkedAdult(t,c)&&
                !EmploymentLife.sick(c,s.day)&&!NpcJusticeLife.confined(t,c,s)&&
                !PublicSafetyLife.alarmed(t,c,s.day,s.minute)&&
                c.health>=50&&c.hunger<75&&c.thirst<75&&c.fatigue<75;
    }
    static boolean assigned(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c.generationIndex>0&&get(t,WORKER)==c.generationIndex&&get(t,PENDING)>0&&
                hours(s)&&available(t,c,s);
    }
    private static void clear(TownLife.Town t){set(t,WORKER,0);set(t,PROGRESS,0);}
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,WORKER)>0){
            var worker=DemographyLife.byIndex(t,get(t,WORKER));
            if(worker==null||!assigned(t,worker,s)){clear(t);return;}
            if(worker.travelRemaining>0||worker.place!=TownLife.Place.NORTH_RESIDENCES||
                    !"WORKING".equals(worker.activity))return;
            int minutes=get(t,PROGRESS)+1;
            set(t,PROGRESS,minutes);
            if(minutes<DELIVERY_MINUTES)return;
            for(var c:t.people.values()){
                int letters=get(t,queue(c));
                if(letters==0)continue;
                if(c.alive&&AddressLife.building(c)==1){
                    set(t,mailbox(c),Math.addExact(deliveredTo(t,c),letters));
                    set(t,DELIVERED,Math.addExact(get(t,DELIVERED),letters));
                }else{
                    set(t,RETURNED,Math.addExact(get(t,RETURNED),letters));
                }
                set(t,queue(c),0);
                set(t,PENDING,get(t,PENDING)-letters);
            }
            worker.fatigue=Math.min(100,worker.fatigue+2);
            clear(t);return;
        }
        if(get(t,PENDING)==0||!hours(s)||s.minute>840||s.minute%30!=0)return;
        var worker=t.people.values().stream().filter(c->available(t,c,s)&&c.travelRemaining==0&&
                c.place==TownLife.Place.OFFICE&&"WORKING".equals(c.activity))
                .sorted(java.util.Comparator.comparing(c->c.id)).findFirst().orElse(null);
        if(worker!=null){set(t,WORKER,worker.generationIndex);set(t,PROGRESS,0);}
    }
    record Report(String status,int pending,int delivered,int returned){}
    static Report report(TownLife.Town t){
        String status=get(t,WORKER)>0?(get(t,PROGRESS)>0?"POSTAL_DELIVERING":"POSTAL_DISPATCHED"):
                get(t,PENDING)>0?"POSTAL_QUEUED":"POSTAL_IDLE";
        return new Report(status,get(t,PENDING),get(t,DELIVERED),get(t,RETURNED));
    }
}
