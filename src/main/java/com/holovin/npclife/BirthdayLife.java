package com.holovin.npclife;

import java.util.*;

/** Annual, private celebrations using the same calendar as resident ageing. */
final class BirthdayLife {
    private BirthdayLife(){}
    private static String key(TownLife.Citizen c,String field){return "birthday:"+c.id+":"+field;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static boolean birthday(TownLife.Town t,TownLife.Citizen c,int day){
        int born=DemographyLife.get(t,c,"born");
        int origin=born>0?born:1;
        return c.generationIndex>0&&day>origin&&(day-origin)%365==0;
    }
    private static boolean available(TownLife.Citizen c){
        return HomeRoutineLife.atHome(c)&&!Set.of("SLEEPING","WORKING","SICK_LEAVE","RESTING_WHILE_SICK").contains(c.activity)
            &&c.health>=50&&c.fatigue<90;
    }
    private static boolean parent(TownLife.Town t,TownLife.Citizen adult,TownLife.Citizen child){
        return adult.generationIndex>0&&(DemographyLife.get(t,child,"parent-a")==adult.generationIndex||
            DemographyLife.get(t,child,"parent-b")==adult.generationIndex);
    }
    private static boolean relative(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        return a!=b&&a.generationIndex>0&&b.generationIndex>0&&
            (parent(t,a,b)||parent(t,b,a)||DemographyLife.get(t,a,"partner")==b.generationIndex||
             java.util.Arrays.stream(new int[]{DemographyLife.get(t,a,"parent-a"),DemographyLife.get(t,a,"parent-b")})
                 .anyMatch(n->n>0&&(n==DemographyLife.get(t,b,"parent-a")||n==DemographyLife.get(t,b,"parent-b"))));
    }
    static void tick(TownLife.Town t,WorldState clock){
        int day=clock.day,minute=clock.minute;
        if(minute<1080||minute>1230||minute%30!=0)return;
        for(var celebrant:t.people.values()){
            if(!birthday(t,celebrant,day)||!available(celebrant)||get(t,celebrant,"last-day")==day)continue;
            var family=t.people.values().stream().filter(c->relative(t,celebrant,c)&&available(c)&&
                c.currentSpace().equals(celebrant.currentSpace())).sorted(Comparator.comparing(c->c.id)).toList();
            if(family.isEmpty())continue;
            int gift=0;
            // One affordable cash gift per child, never automatically spend a controlled adult's money.
            if(celebrant.age<18){
                var donor=family.stream().filter(c->c.age>=18&&parent(t,c,celebrant)&&c.money>=85&&!RebirthLife.linkedAdult(t,c))
                    .max(Comparator.comparingInt(c->c.money)).orElse(null);
                if(donor!=null){celebrant.money=Math.addExact(celebrant.money,5);donor.money-=5;gift=5;}
            }
            put(t,celebrant,"last-day",day);put(t,celebrant,"minute",minute);
            put(t,celebrant,"count",Math.addExact(get(t,celebrant,"count"),1));
            put(t,celebrant,"last-gift",gift);
            put(t,celebrant,"gift-total",Math.addExact(get(t,celebrant,"gift-total"),gift));
            for(var c:family){
                var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(celebrant.id,c.id),k->new SocialLife.Relationship(celebrant.id,c.id));
                relation.affinity=Math.min(100,relation.affinity+2);
                relation.trust=Math.min(100,relation.trust+1);
                relation.conversations++;
                if(relation.encounters==0)relation.encounters=1;
            }
        }
    }
    static boolean celebrating(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!available(c))return false;
        return t.people.values().stream().anyMatch(p->available(p)&&p.currentSpace().equals(c.currentSpace())&&
            (p==c||relative(t,p,c))&&get(t,p,"last-day")==day&&minute>=get(t,p,"minute")&&minute<get(t,p,"minute")+30);
    }
    record Memory(int celebrations,int lastDay,int lastGift,int totalGifts,int nextDay){}
    static Memory memory(TownLife.Town t,TownLife.Citizen c,int day){
        int born=DemographyLife.get(t,c,"born");
        int origin=born>0?born:1;
        int next=origin+365*Math.max(1,(Math.max(0,day-origin)/365)+1);
        if(birthday(t,c,day))next=day;
        return new Memory(get(t,c,"count"),get(t,c,"last-day"),get(t,c,"last-gift"),get(t,c,"gift-total"),next);
    }
}
