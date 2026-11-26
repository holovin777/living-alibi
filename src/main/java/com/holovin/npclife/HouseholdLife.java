package com.holovin.npclife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Autonomous family events only; controlled adults retain their own choices. */
final class HouseholdLife {
    private HouseholdLife(){}
    private static String key(String unit,String field){return "household:"+unit+":"+field;}
    private static int get(TownLife.Town t,String unit,String f){return t.industryStocks.getOrDefault(key(unit,f),0);}
    private static void put(TownLife.Town t,String unit,String f,int n){t.industryStocks.put(key(unit,f),n);}
    private static boolean awake(TownLife.Citizen c){
        return HomeRoutineLife.atHome(c)&&!Set.of("SLEEPING","WORKING","SICK_LEAVE","RESTING_WHILE_SICK").contains(c.activity);
    }
    private static boolean related(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        if(a.generationIndex<=0||b.generationIndex<=0)return false;
        return DemographyLife.get(t,a,"partner")==b.generationIndex||
            DemographyLife.get(t,a,"parent-a")==b.generationIndex||DemographyLife.get(t,a,"parent-b")==b.generationIndex||
            DemographyLife.get(t,b,"parent-a")==a.generationIndex||DemographyLife.get(t,b,"parent-b")==a.generationIndex;
    }
    private static void cohabit(TownLife.Town t,int day){
        for(var a:t.people.values()){
            if(!a.alive||a.generationIndex<=0||RebirthLife.linkedAdult(t,a))continue;
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,a,"partner"));
            if(b==null||!b.alive||a.generationIndex>=b.generationIndex||RebirthLife.linkedAdult(t,b)||
                    DemographyLife.get(t,b,"partner")!=a.generationIndex||a.homeUnit.equals(b.homeUnit)||
                    day-DemographyLife.get(t,a,"union-day")<30)continue;
            var relation=t.existingRelationship(a.id,b.id);
            if(relation==null||relation.trust<70||relation.affinity<75||relation.tension>25)continue;
            if(!a.homeUnit.equals(TownLife.privateUnit(a.id))||!b.homeUnit.equals(TownLife.privateUnit(b.id)))continue;
            if(!HomeRoutineLife.atHome(a)||!HomeRoutineLife.atHome(b))continue;
            RentalLife.moveHome(t,b,a.home,a.homeUnit);
            // Same district/home footprint only after a genuine stable partnership.
        }
    }
    static void tick(TownLife.Town t,WorldState clock){
        int day=clock.day,minute=clock.minute;
        if(minute==360)cohabit(t,day);
        if(minute<1080||minute>1260||minute%30!=0)return;
        Map<String,List<TownLife.Citizen>> homes=new LinkedHashMap<>();
        for(var c:t.people.values())if(c.generationIndex>0&&awake(c))
            homes.computeIfAbsent(c.homeUnit,k->new ArrayList<>()).add(c);
        for(var e:homes.entrySet()){
            String unit=e.getKey();var members=e.getValue();
            if(members.size()<2||members.stream().anyMatch(c->RebirthLife.linkedAdult(t,c)))continue;
            boolean family=false;
            for(var a:members)for(var b:members)if(a!=b&&related(t,a,b))family=true;
            if(!family||get(t,unit,"meal-day")==day)continue;
            var cook=members.stream().filter(c->c.age>=18&&c.food>=members.size())
                    .max(java.util.Comparator.comparingInt(c->c.food)).orElse(null);
            if(cook==null)continue;
            cook.food-=members.size();
            for(var c:members)c.hunger=Math.max(0,c.hunger-35);
            put(t,unit,"meal-day",day);put(t,unit,"meal-minute",minute);
            put(t,unit,"meals",get(t,unit,"meals")+1);
            // Freeze the dinner topic with the existing meal event.
            boolean strainedDinner=members.stream().anyMatch(a->members.stream().anyMatch(b->
                a!=b&&related(t,a,b)&&(a.fatigue+b.fatigue>=160||a.unpaidBills+b.unpaidBills>=100)));
            int topic=strainedDinner?1:members.stream().anyMatch(c->c.age>=6&&c.age<18)?2:
                members.stream().anyMatch(c->c.sociability>=70)?3:4;
            put(t,unit,"meal-topic",topic);
            for(var a:members)for(var b:members){
                if(a.generationIndex>=b.generationIndex||!related(t,a,b))continue;
                var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(a.id,b.id),k->new SocialLife.Relationship(a.id,b.id));
                relation.conversations++;
                boolean strained=a.fatigue+b.fatigue>=160||a.unpaidBills+b.unpaidBills>=100;
                relation.tension=Math.max(0,Math.min(100,relation.tension+(strained?3:-2)));
                if(strained){relation.arguments++;relation.affinity=Math.max(0,relation.affinity-1);}
                else {relation.affinity=Math.min(100,relation.affinity+1);relation.trust=Math.min(100,relation.trust+1);}
            }
            if((day-1)%7==0)for(var c:members){
                if(c.age>=18&&members.stream().anyMatch(p->p!=c&&DemographyLife.get(t,c,"partner")==p.generationIndex))
                    FamilyBudgetLife.autonomousDeposit(t,c);
            }
        }
    }
    static boolean dinnerNow(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        int start=get(t,c.homeUnit,"meal-minute");
        return awake(c)&&get(t,c.homeUnit,"meal-day")==day&&minute>=start&&minute<start+20;
    }
    record ResidentView(String id,int age,String activity){}
    record Speech(String speakerId,String english,String italian){}
    record View(int members,int children,int present,int dinners,boolean dinnerNow,List<ResidentView> visibleResidents,Speech speech){}
    // Read-only, time-derived dialogue for an already committed dinner. No
    // new social event, inferred identity, inventory change or private log.
    private static Speech speech(TownLife.Town t,TownLife.Citizen host,int day,int minute,
                                 boolean inside,List<ResidentView> visible){
        if(!inside||!dinnerNow(t,host,day,minute)||host.age<6)return null;
        int topic=get(t,host.homeUnit,"meal-topic");
        if(topic<1||topic>4)return null;
        var other=visible.stream().filter(c->c.age()>=6)
            .filter(c->related(t,host,t.get(c.id())))
            .filter(c->topic!=2||((c.age()<18)!=(host.age<18)))
            .findFirst().orElse(null);
        if(other==null)return null;
        boolean reply=minute-get(t,host.homeUnit,"meal-minute")>=10;
        String speaker=reply?other.id():host.id;
        return switch(topic){
            case 1 -> reply?new Speech(speaker,"I know. We can talk about it after dinner.","Lo so. Ne parliamo dopo cena."):
                new Speech(speaker,"It's been a tiring day. We need to talk.","È stata una giornata faticosa. Dobbiamo parlare.");
            case 2 -> {
                boolean childSpeaker=(reply?other.age():host.age)<18;
                if(childSpeaker)yield new Speech(speaker,"Can I finish my homework after dinner?","Posso finire i compiti dopo cena?");
                yield reply?new Speech(speaker,"Of course, I can help you if you need it.","Certo, se vuoi ti do una mano."):
                    new Speech(speaker,"Have you got any homework left?","Hai ancora dei compiti da fare?");
            }
            case 3 -> reply?new Speech(speaker,"Then you'd better do the dishes too!","Allora ti tocca anche lavare i piatti!"):
                new Speech(speaker,"I'm the family's official food taster.","Sono l'assaggiatore ufficiale della famiglia.");
            default -> reply?new Speech(speaker,"A quiet evening sounds good to me.","Una serata tranquilla mi farebbe bene."):
                new Speech(speaker,"Shall we take it easy this evening?","Facciamo una serata tranquilla?");
        };
    }
    static View view(TownLife.Town t,TownLife.Citizen host,int day,int minute){
        return view(t,host,day,minute,false);
    }
    static View view(TownLife.Town t,TownLife.Citizen host,int day,int minute,boolean inside){
        var members=t.people.values().stream().filter(c->c.alive&&host.homeUnit.equals(c.homeUnit)).toList();
        var visible=inside?members.stream()
            .filter(c->!c.id.equals(host.id)&&awake(c)&&c.age>=3)
            .map(c->new ResidentView(c.id,c.age,HomeRoutineLife.activity(t,c,day,minute))).toList()
            :List.<ResidentView>of();
        String unit=host.homeUnit;
        return new View(members.size(),(int)members.stream().filter(c->c.age<18).count(),
            (int)members.stream().filter(HouseholdLife::awake).count(),get(t,unit,"meals"),
            dinnerNow(t,host,day,minute),visible,speech(t,host,day,minute,inside,visible));
    }
}
