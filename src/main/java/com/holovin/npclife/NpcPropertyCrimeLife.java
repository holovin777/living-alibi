package com.holovin.npclife;

import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Abstract economic incidents, not a simulation of entry, force or security bypass. */
final class NpcPropertyCrimeLife {
    private NpcPropertyCrimeLife(){}
    enum Kind { SHOP, WORKSHOP, BANK, HOME }
    private static final String PREFIX="npc-property-crime:";
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(PREFIX+key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(PREFIX+key,value);}
    static boolean active(TownLife.Town t){return get(t,"kind")>0;}
    static Kind kind(TownLife.Town t){return active(t)?Kind.values()[get(t,"kind")-1]:null;}
    static TownLife.Place scene(TownLife.Town t){return active(t)?TownLife.Place.values()[get(t,"scene")-1]:null;}
    static int now(WorldState s){return NpcVehicleCrimeLife.now(s);}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&NpcVehicleCrimeLife.eligible(t,c,s)&&NpcJusticeLife.convictions(t,c)==0;
    }
    static int available(TownLife.Town t,Kind kind,TownLife.Citizen victim){
        return switch(kind){
            case SHOP -> Math.max(0,t.employerFunds.getOrDefault("shop-assistant",0)-20);
            case WORKSHOP -> t.business.registered?Math.max(0,t.business.cash):Math.max(0,t.employerFunds.getOrDefault("technician",0)-20);
            case BANK -> BankLife.free(t);
            case HOME -> victim!=null&&victim.alive?Math.max(0,victim.money-10):0;
        };
    }
    static void balance(TownLife.Town t,Kind kind,TownLife.Citizen victim,boolean privateWorkshop,int change){
        switch(kind){
            case SHOP -> t.employerFunds.merge("shop-assistant",change,Math::addExact);
            case WORKSHOP -> {
                if(privateWorkshop)t.business.cash=Math.addExact(t.business.cash,change);
                else t.employerFunds.merge("technician",change,Math::addExact);
            }
            case BANK -> BankLife.cash(t,Math.addExact(BankLife.cash(t),change));
            case HOME -> victim.money=Math.addExact(victim.money,change);
        }
    }
    static boolean validScene(Kind kind,TownLife.Place place,TownLife.Citizen victim){
        return switch(kind){
            case SHOP -> place==MARKET||place==PORTICI||place==BAKERY;
            case WORKSHOP -> place==WORKSHOP;
            case BANK -> place==OFFICE;
            case HOME -> victim!=null&&victim.home==place&&victim.generationIndex>0;
        };
    }
    static void record(TownLife.Town t,WorldState s,Kind kind,TownLife.Citizen actor,TownLife.Citizen victim,
                       List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(active(t)||!eligible(t,actor,s)||actor==victim||!validScene(kind,actor.place,victim))
            throw new IllegalArgumentException("No eligible property incident.");
        int amount=Math.min(20,available(t,kind,victim));
        if(amount<1)throw new IllegalArgumentException("No available property balance.");
        boolean privateWorkshop=kind==Kind.WORKSHOP&&t.business.registered;
        var witness=t.people.values().stream().filter(c->c!=actor&&c!=victim&&c.age>=18&&c.generationIndex>0
                &&c.presentAt(actor.place)&&c.currentSpace().equals(TownLife.publicSpace(actor.place))&&c.carefulness>=60)
                .min(Comparator.comparingInt(c->c.generationIndex)).orElse(null);
        int number=Math.addExact(get(t,"total"),1);
        balance(t,kind,victim,privateWorkshop,-amount);
        actor.money=Math.addExact(actor.money,amount);
        set(t,"total",number);set(t,"kind",kind.ordinal()+1);set(t,"scene",actor.place.ordinal()+1);
        set(t,"actor",actor.generationIndex);set(t,"victim",victim==null?0:victim.generationIndex);
        set(t,"witness",witness==null?0:witness.generationIndex);set(t,"amount",amount);
        set(t,"private-workshop",privateWorkshop?1:0);set(t,"workshop-owner",privateWorkshop?t.business.ownerIncarnation:0);
        set(t,"reported",now(s));set(t,"examined",0);
        set(t,"insured",privateWorkshop&&SecurityLife.workshopContract(t,s.day,"insurance-until")?1:0);
        set(t,"loss",Math.addExact(get(t,"loss"),amount));
        String message="A property loss was reported in "+actor.place.label+".";
        // Public reports disclose neither the suspect nor the private household.
        TownLife.publish(t,s,"property-report:"+number,actor.place,message,events,observations);
        ArchiveLife.record(t,"npc-property-report:"+number,s.day,s.minute,"Public safety",message);
    }
    static void resolve(TownLife.Town t,WorldState s,boolean confirmed){
        if(!active(t))return;
        var actor=NpcVehicleCrimeLife.person(t,get(t,"actor"));
        var victim=NpcVehicleCrimeLife.person(t,get(t,"victim"));
        Kind kind=kind(t);int number=get(t,"total");
        int returned=0;
        boolean validRecipient=(kind!=Kind.HOME||victim!=null&&victim.alive)&&
            (get(t,"private-workshop")==0||t.business.registered&&t.business.ownerIncarnation==get(t,"workshop-owner"));
        if(confirmed&&actor!=null&&actor.alive){
            if(validRecipient){
                returned=Math.min(actor.money,get(t,"amount"));
                actor.money-=returned;
                balance(t,kind,victim,get(t,"private-workshop")==1,returned);
                set(t,"recovered",Math.addExact(get(t,"recovered"),returned));
            }
            NpcJusticeLife.propertyConviction(t,s,actor,number);
        }
        if(get(t,"insured")==1)SecurityLife.compensate(t,get(t,"workshop-owner"),get(t,"amount")-returned);
        ArchiveLife.record(t,"npc-property-closed:"+number,s.day,s.minute,"Public safety",
                confirmed?"A property investigation was confirmed. Restitution recorded: "+returned+" EUR.":
                "A property investigation closed without a confirmed offender.");
        set(t,"next",now(s)+4*1440);
        for(String key:List.of("kind","scene","actor","victim","witness","amount","private-workshop","workshop-owner","reported","examined","insured"))set(t,key,0);
    }
    static void tick(TownLife.Town t,WorldState s,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!NpcTransportLife.enabled(t)||s.minute%30!=0)return;
        if(active(t)){
            int age=now(s)-get(t,"reported");
            if(get(t,"examined")==0&&t.people.values().stream().anyMatch(c->PoliceLife.detective(c)&&
                    c.presentAt(scene(t))&&PoliceLife.scheduled(c,s.day,s.minute)))set(t,"examined",1);
            var witness=NpcVehicleCrimeLife.person(t,get(t,"witness"));
            var actor=NpcVehicleCrimeLife.person(t,get(t,"actor"));
            if(age>=1440&&get(t,"examined")==1&&witness!=null&&witness.alive&&actor!=null&&actor.alive){resolve(t,s,true);return;}
            if(age>=4*1440)resolve(t,s,false);
            return;
        }
        if(t.people.size()<24||now(s)<get(t,"next")||NpcVehicleCrimeLife.active(t))return;
        // Rare game events, independent of the player's presence. No tactical targeting.
        if(Math.floorMod(Long.hashCode((long)now(s)*0x9e3779b97f4a7c15L),100)>=2)return;
        var residents=new ArrayList<TownLife.Citizen>();
        for(var c:t.people.values())if(eligible(t,c,s))residents.add(c);
        if(residents.isEmpty())return;
        var actor=residents.get(Math.floorMod(Objects.hash(s.day,s.minute,"property-event"),residents.size()));
        var victim=residents.stream().filter(c->c!=actor&&c.home==actor.place&&c.money>10).findFirst().orElse(null);
        Kind kind=switch(actor.place){
            case MARKET,PORTICI,BAKERY -> Kind.SHOP;
            case WORKSHOP -> Kind.WORKSHOP;
            case OFFICE -> Kind.BANK;
            default -> Kind.HOME;
        };
        if(!validScene(kind,actor.place,victim)||available(t,kind,victim)<1)return;
        var reference=victim!=null?victim:t.people.values().stream().filter(c->c!=actor&&c.alive).findFirst().orElse(null);
        if(reference!=null&&MotiveLife.assess(t,s,actor,reference).strength()>=75){
            if(SecurityLife.prevent(t,s,kind,actor.place))set(t,"next",now(s)+1440);
            else record(t,s,kind,actor,victim,events,observations);
        }
    }
    record View(int reports,int totalLoss,int restitution,boolean localInvestigation){}
    static View view(TownLife.Town t){return new View(get(t,"total"),get(t,"loss"),get(t,"recovered"),scene(t)==t.player.place);}
}
