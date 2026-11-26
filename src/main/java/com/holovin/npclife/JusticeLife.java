package com.holovin.npclife;

import java.util.List;

/** Fictional, compressed justice system. No real-world sentencing rules are implied. */
final class JusticeLife {
    private JusticeLife(){}
    private static String key(TownLife.Town t,String field){return "combat:"+t.player.incarnation+":justice:"+field;}
    private static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    private static void put(TownLife.Town t,String field,int value){t.industryStocks.put(key(t,field),value);}
    private static int time(WorldState s){return Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1;}
    static boolean confined(TownLife.Town t){return get(t,"phase")==2||get(t,"phase")==3;}
    record View(String status,int minutesRemaining,int convictions,int finePaid,int fineOutstanding,int assaults){}
    static View view(TownLife.Town t,WorldState s){
        int phase=get(t,"phase");
        return new View(switch(phase){case 1->"REPORTED";case 2->"DETAINED";case 3->"PRISON";case 4->"RELEASED";default->"NONE";},
                phase>=1&&phase<=3?Math.max(0,get(t,"until")-time(s)):0,
                get(t,"convictions"),get(t,"fine-paid"),get(t,"fine-outstanding"),get(t,"assaults"));
    }
    static void requireFree(TownLife.Town t){
        if(confined(t))throw new IllegalArgumentException("You are in custody. Use the custody controls.");
    }
    static void report(TownLife.Town t,WorldState s,boolean officer,boolean armed,
                       List<TownLife.Event> truth,List<TownLife.Observation> observations){
        requireFree(t);
        put(t,"assaults",Math.addExact(get(t,"assaults"),1));
        put(t,"pending",Math.addExact(get(t,"pending"),1));
        if(armed)put(t,"armed",1);
        if(officer)arrest(t,s,truth,observations);
        else if(get(t,"phase")!=1){
            put(t,"phase",1);put(t,"until",Math.addExact(time(s),60));
        }
    }
    private static void arrest(TownLife.Town t,WorldState s,List<TownLife.Event> truth,List<TownLife.Observation> observations){
        put(t,"phase",2);put(t,"until",Math.addExact(time(s),60));
        // Abstract police transfer to custody; no simulated chase.
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        t.player.travelRemaining=0;t.player.cycling=false;
        t.industryStocks.put("combat:"+t.player.incarnation+":baton",0);
        truth.add(new TownLife.Event(s.day,s.minute,"visitor:"+t.player.incarnation,
                t.player.place,"custody:"+t.player.incarnation,"Police detained a visitor following a recorded assault."));
    }
    static void tick(TownLife.Town t,WorldState s,List<TownLife.Event> truth,List<TownLife.Observation> observations){
        if(!t.player.alive||RebirthLife.active(t))return;
        int phase=get(t,"phase");
        if(phase==1&&time(s)>=get(t,"until")){
            arrest(t,s,truth,observations);return;
        }
        if(phase==2||phase==3){
            // Basic institutional meals and rest; ordinary citizens continue their lives.
            t.player.hunger=Math.min(45,t.player.hunger);
            t.player.thirst=Math.min(40,t.player.thirst);
            t.player.fatigue=Math.min(50,t.player.fatigue);
        }
        if(phase==2&&time(s)>=get(t,"until")){
            int days=Math.min(7,Math.max(1,get(t,"pending")+get(t,"armed")+get(t,"convictions")));
            int fine=30*days;
            int paid=Math.min(t.player.money,fine);
            t.player.money-=paid;
            put(t,"fine-paid",Math.addExact(get(t,"fine-paid"),paid));
            put(t,"fine-outstanding",Math.addExact(get(t,"fine-outstanding"),fine-paid));
            put(t,"convictions",Math.addExact(get(t,"convictions"),1));
            put(t,"pending",0);put(t,"armed",0);
            put(t,"phase",3);put(t,"until",Math.addExact(time(s),days*1440));
            truth.add(new TownLife.Event(s.day,s.minute,"visitor:"+t.player.incarnation,
                    t.player.place,"custody:"+t.player.incarnation,"A fictional court imposed a prison sentence."));
        } else if(phase==3&&time(s)>=get(t,"until")){
            put(t,"phase",4);put(t,"until",0);
            truth.add(new TownLife.Event(s.day,s.minute,"visitor:"+t.player.incarnation,
                    t.player.place,"custody:"+t.player.incarnation,"The visitor completed the prison sentence."));
        }
    }
    static int waitDuration(TownLife.Town t,WorldState s,int requested){
        if(!confined(t))throw new IllegalArgumentException("You are not in custody.");
        return Math.max(1,Math.min(requested,get(t,"until")-time(s)));
    }
    static String payFine(TownLife.Town t){
        requireFree(t);
        int payment=Math.min(t.player.money,get(t,"fine-outstanding"));
        if(payment<=0)throw new IllegalArgumentException("No payable fine or insufficient funds.");
        t.player.money-=payment;
        put(t,"fine-outstanding",get(t,"fine-outstanding")-payment);
        put(t,"fine-paid",Math.addExact(get(t,"fine-paid"),payment));
        return "Fine payment recorded.";
    }
}
