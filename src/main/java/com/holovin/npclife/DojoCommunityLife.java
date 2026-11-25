package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** The club is founded by a visitor's first linked-adult class. Afterwards
 * real town residents attend independently, whether or not the player joins. */
final class DojoCommunityLife {
    private DojoCommunityLife(){}
    static final String FOUNDED="dojo-community:founded";
    static final String INSTRUCTOR="dojo-community:instructor";
    static void found(TownLife.Town t,int day){
        if(founded(t))return;
        var coach=instructor(t,day);
        if(coach!=null)t.industryStocks.put(INSTRUCTOR,coach.generationIndex);
        t.industryStocks.put(FOUNDED,1);
    }
    static boolean founded(TownLife.Town t){return t.industryStocks.getOrDefault(FOUNDED,0)>0;}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c,int day){
        return c.alive&&c.generationIndex>0&&c.age>=18&&c.age<=65&&!RebirthLife.linkedAdult(t,c)
                &&!EmploymentLife.sick(c,day)&&c.health>=65;
    }
    static TownLife.Citizen instructor(TownLife.Town t,int day){
        if(founded(t)){
            var chosen=DemographyLife.byIndex(t,t.industryStocks.getOrDefault(INSTRUCTOR,0));
            return chosen!=null&&eligible(t,chosen,day)?chosen:null;
        }
        return t.people.values().stream().filter(c->eligible(t,c,day))
                .filter(c->c.age>=25&&c.carefulness>=50)
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->DojoLife.get(t,c,"rank")).reversed()
                    .thenComparing(Comparator.comparingInt((TownLife.Citizen c)->EducationLife.get(t,c,"fitness")).reversed())
                    .thenComparingInt(c->c.generationIndex)).findFirst().orElse(null);
    }
    static boolean scheduled(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!founded(t)||!eligible(t,c,day)||!DojoLife.open(day,1080)||minute<1050||minute>=1260)return false;
        if(c==instructor(t,day))return true;
        return "STAY_HEALTHY".equals(c.goal)||Math.floorMod(c.personalSeed,5)==0;
    }
    record Member(String id,String name,String role){}
    record View(boolean founded,List<Member> present,boolean instructorPresent){}
    static View view(TownLife.Town t,int day,int minute){
        var teacher=instructor(t,day);
        var present=t.people.values().stream().filter(c->scheduled(t,c,day,minute)&&c.place==TownLife.Place.SCHOOL&&RebirthLife.awakePresent(c))
            .map(c->new Member(c.id,c.name,c==teacher?"instructor":"student")).toList();
        return new View(founded(t),present,present.stream().anyMatch(m->m.role().equals("instructor")));
    }
    static RebirthLife.HomeAction greet(TownLife.Town t,int day,int minute){
        var c=RebirthLife.requireAdult(t);
        var teacher=instructor(t,day);
        if(c.place!=TownLife.Place.SCHOOL||!RebirthLife.awakePresent(c)||!view(t,day,minute).instructorPresent()||
                DojoLife.get(t,c,"last-rei")>=day)throw new IllegalArgumentException("Greet the attending instructor once per day.");
        String key=SocialNetwork.key(c.id,teacher.id);
        var relation=t.peerRelationships.computeIfAbsent(key,k->new SocialLife.Relationship(
                c.id.compareTo(teacher.id)<=0?c.id:teacher.id,c.id.compareTo(teacher.id)<=0?teacher.id:c.id));
        relation.affinity=Math.min(100,relation.affinity+1);
        relation.trust=Math.min(100,relation.trust+1);
        DojoLife.set(t,c,"last-rei",day);
        return new RebirthLife.HomeAction(1,"Exchanged a respectful dojo greeting.");
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(clock.minute!=1110||!founded(t)||!DojoLife.open(clock.day,1080))return;
        var teacher=instructor(t,clock.day);
        if(teacher==null||teacher.place!=TownLife.Place.SCHOOL||!RebirthLife.awakePresent(teacher))return;
        for(var c:t.people.values()){
            if(c==teacher||!scheduled(t,c,clock.day,clock.minute)||c.place!=TownLife.Place.SCHOOL||
                !RebirthLife.awakePresent(c)||c.money<25||c.fatigue>75||c.hunger>75||c.thirst>75||
                DojoLife.get(t,c,"community-last-day")>=clock.day)continue;
            int funds=Math.addExact(t.employerFunds.getOrDefault("school",0),5);
            c.money-=5;t.employerFunds.put("school",funds);
            c.fatigue=Math.min(100,c.fatigue+8);
            EducationLife.learn(t,c,"fitness",1);
            DojoLife.set(t,c,"community-last-day",clock.day);
            DojoLife.set(t,c,"sessions",Math.addExact(DojoLife.get(t,c,"sessions"),1));
            String mode=DojoLife.MODES.get(Math.floorMod(DojoLife.get(t,c,"sessions")-1,3));
            DojoLife.set(t,c,mode,Math.min(100,DojoLife.get(t,c,mode)+1));
            var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(c.id,teacher.id),k->new SocialLife.Relationship(c.id.compareTo(teacher.id)<=0?c.id:teacher.id,c.id.compareTo(teacher.id)<=0?teacher.id:c.id));
            relation.affinity=Math.min(100,relation.affinity+1);
            relation.trust=Math.min(100,relation.trust+1);
        }
    }
}
