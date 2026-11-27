package com.holovin.npclife;

import java.util.*;
/** Bounded autonomous fictional car crime. Persist one open incident independently of the visitor. */
final class NpcVehicleCrimeLife {
    private NpcVehicleCrimeLife(){}
    static final String PREFIX="npc-vehicle-crime:";
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(PREFIX+k,0);}
    static void set(TownLife.Town t,String k,int v){t.industryStocks.put(PREFIX+k,v);}
    static int now(WorldState s){return Math.toIntExact(((long)s.day-1)*1440+s.minute+1);}
    static boolean active(TownLife.Town t){return get(t,"owner")>0;}
    static boolean missing(TownLife.Town t,TownLife.Citizen c){return c.generationIndex>0&&get(t,"owner")==c.generationIndex;}
    static TownLife.Citizen person(TownLife.Town t,int index){
        if(index<=0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==index).findFirst().orElse(null);
    }
    static TownLife.Place place(TownLife.Town t,String k){
        int value=get(t,k);
        return value>0&&value<=TownLife.Place.values().length?TownLife.Place.values()[value-1]:null;
    }
    static TownLife.Place scene(TownLife.Town t){return active(t)?place(t,"scene"):null;}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return NpcTransportLife.eligible(c)&&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)
                &&c.travelRemaining==0&&c.currentSpace().equals(TownLife.publicSpace(c.place));
    }
    static void record(TownLife.Town t,WorldState s,TownLife.Citizen actor,TownLife.Citizen victim,
                       List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!NpcTransportLife.enabled(t)||active(t)||actor==victim||!eligible(t,actor,s)
                ||!NpcTransportLife.owns(t,victim)||VehicleTheftLife.missing(t,victim)
                ||NpcTransportLife.mode(t,victim)==NpcTransportLife.CAR||NpcTransportLife.parked(t,victim)!=actor.place)
            throw new IllegalArgumentException("No available resident vehicle incident.");
        var scene=actor.place;
        var witness=t.people.values().stream().filter(c->c!=actor&&c!=victim&&c.generationIndex>0&&c.age>=18
                &&c.presentAt(scene)&&c.currentSpace().equals(TownLife.publicSpace(scene))&&c.carefulness>=60)
                .min(Comparator.comparingInt(c->c.generationIndex)).orElse(null);
        int number=Math.addExact(get(t,"total"),1);
        set(t,"total",number);set(t,"owner",victim.generationIndex);set(t,"actor",actor.generationIndex);
        set(t,"witness",witness==null?0:witness.generationIndex);
        set(t,"scene",scene.ordinal()+1);set(t,"parked",scene.ordinal()+1);set(t,"reported",now(s));
        set(t,"fuel",NpcTransportLife.get(t,victim,"initialized")==0?6:NpcTransportLife.get(t,victim,"fuel"));
        set(t,"condition",NpcTransportLife.get(t,victim,"initialized")==0?80:NpcTransportLife.get(t,victim,"condition"));
        set(t,"examined",0);
        // Public report is about the missing vehicle; never publish the hidden offender.
        TownLife.publish(t,s,victim.id,scene,"A resident reported a missing car in "+scene.label+".",events,observations);
        ArchiveLife.record(t,"npc-car-report:"+number,s.day,s.minute,"Public safety","A resident vehicle was reported missing in "+scene.label+".");
    }
    static boolean borrowTrip(TownLife.Town t,TownLife.Citizen c,TownLife.Place desired,int units){
        if(!active(t)||get(t,"actor")!=c.generationIndex||place(t,"parked")!=c.place
                ||get(t,"fuel")<units||get(t,"condition")<units*3)return false;
        set(t,"fuel",get(t,"fuel")-units);set(t,"condition",get(t,"condition")-units*3);
        set(t,"parked",desired.ordinal()+1);
        NpcTransportLife.set(t,c,"mode",NpcTransportLife.CAR);
        return true;
    }
    static void recover(TownLife.Town t,WorldState s,boolean confirmed){
        var victim=person(t,get(t,"owner"));var actor=person(t,get(t,"actor"));
        int number=get(t,"total");
        if(victim!=null&&victim.alive){
            NpcTransportLife.set(t,victim,"initialized",1);NpcTransportLife.set(t,victim,"owned",1);
            NpcTransportLife.set(t,victim,"parked",get(t,"scene"));
            NpcTransportLife.set(t,victim,"fuel",get(t,"fuel"));NpcTransportLife.set(t,victim,"condition",get(t,"condition"));
        }
        if(confirmed&&actor!=null)NpcJusticeLife.vehicleConviction(t,s,actor,number);
        ArchiveLife.record(t,"npc-car-recovered:"+number,s.day,s.minute,"Public safety",confirmed?
                "A missing car was recovered following a confirmed investigation.":
                "A missing car was recovered; no offender was confirmed.");
        set(t,"recovered",get(t,"recovered")+1);set(t,"next",now(s)+3*1440);
        for(String key:List.of("owner","actor","witness","scene","parked","reported","fuel","condition","examined"))set(t,key,0);
    }
    static void tick(TownLife.Town t,WorldState s,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!NpcTransportLife.enabled(t))return;
        if(s.minute%30!=0)return;
        if(active(t)){
            int age=now(s)-get(t,"reported");var scene=scene(t);
            if(get(t,"examined")==0&&scene!=null&&t.people.values().stream().anyMatch(c->PoliceLife.detective(c)&&c.presentAt(scene)
                    &&PoliceLife.scheduled(c,s.day,s.minute)))set(t,"examined",1);
            // A documented eyewitness plus a scene visit is required by this fictional case model.
            var witness=person(t,get(t,"witness"));var actor=person(t,get(t,"actor"));
            if(age>=1440&&get(t,"examined")==1&&witness!=null&&witness.alive&&actor!=null&&actor.alive){
                recover(t,s,true);return;
            }
            if(age>=3*1440)recover(t,s,false);
            return;
        }
        if(now(s)<get(t,"next")||s.minute<540||s.minute>1170||t.people.size()<24)return;
        // Rare, reproducible decision windows; do not scan every pair in the town.
        if(Math.floorMod(Objects.hash(s.day,s.minute,"resident-car-crime"),100)>=4)return;
        var parked=new EnumMap<TownLife.Place,TownLife.Citizen>(TownLife.Place.class);
        for(var c:t.people.values())if(NpcTransportLife.owns(t,c)&&!VehicleTheftLife.missing(t,c)
                &&NpcTransportLife.mode(t,c)!=NpcTransportLife.CAR&&NpcTransportLife.parked(t,c)!=null)
            parked.putIfAbsent(NpcTransportLife.parked(t,c),c);
        TownLife.Citizen chosen=null,victim=null;int score=74;
        for(var c:t.people.values()){
            if(!eligible(t,c,s)||NpcTransportLife.owns(t,c)||NpcJusticeLife.convictions(t,c)>0)continue;
            var target=parked.get(c.place);if(target==null||target==c)continue;
            int pressure=MotiveLife.assess(t,s,c,target).strength();
            if(pressure>score){score=pressure;chosen=c;victim=target;}
        }
        if(chosen!=null)record(t,s,chosen,victim,events,observations);
    }
    record View(int reports,int recovered,boolean localMissingVehicle){}
    static View view(TownLife.Town t){return new View(get(t,"total"),get(t,"recovered"),scene(t)==t.player.place);}
}
