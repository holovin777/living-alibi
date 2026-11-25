package com.holovin.npclife;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/** Persistent family selection and linked newborn preview; adult controls remain locked. */
final class RebirthLife {
    private RebirthLife(){}
    static final String PHASE="rebirth:phase", CHILD="rebirth:child";
    static final int ADULT=0, CHOICE=1, CHILDHOOD=2;
    static void ensure(TownLife.Town t){
        t.industryStocks.putIfAbsent(PHASE,ADULT);t.industryStocks.putIfAbsent(CHILD,0);
    }
    static int phase(TownLife.Town t){return t.industryStocks.getOrDefault(PHASE,ADULT);}
    static boolean active(TownLife.Town t){return phase(t)!=ADULT;}
    static TownLife.Citizen child(TownLife.Town t){return DemographyLife.byIndex(t,t.industryStocks.getOrDefault(CHILD,0));}
    static void prepare(TownLife.Town t,int day,int minute){
        if(active(t))return;
        PlayerLife.beginNewLife(t,day,minute);
        t.player.alive=false;t.player.health=0;t.player.money=0;
        t.industryStocks.put(PHASE,CHOICE);t.industryStocks.put(CHILD,0);
    }
    record Family(int first,int second,String firstName,String secondName,String firstJob,String secondJob,String district){}
    static List<Family> families(TownLife.Town t,int day){
        var result=new ArrayList<Family>();
        for(var a:t.people.values())if(DemographyLife.parentAge(a)){
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,a,"partner"));
            if(b!=null&&a.generationIndex<b.generationIndex&&DemographyLife.canBirth(t,a,b,day))
                result.add(new Family(a.generationIndex,b.generationIndex,a.name,b.name,a.occupation,b.occupation,a.home.label));
        }
        return List.copyOf(result);
    }
    static String choose(TownLife.Town t,int day,int first,int second){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("No family selection is pending.");
        if(families(t,day).stream().noneMatch(f->f.first()==first&&f.second()==second))
            throw new IllegalArgumentException("This family is not currently eligible.");
        var a=DemographyLife.byIndex(t,first);var b=DemographyLife.byIndex(t,second);
        var newborn=DemographyLife.birth(t,a,b,day);
        if(newborn==null)throw new IllegalArgumentException("This family is no longer available.");
        t.industryStocks.put(CHILD,newborn.generationIndex);t.industryStocks.put(PHASE,CHILDHOOD);
        t.industryStocks.put(FOCUS,0);t.industryStocks.put(LAST_CHAT,0);
        synchronize(t);
        return "Born as "+newborn.name+". Follow family life and discover new choices as you grow.";
    }
    static void synchronize(TownLife.Town t){
        if(phase(t)!=CHILDHOOD)return;
        var c=child(t);
        if(c==null||!c.alive){
            t.industryStocks.put(CHILD,0);t.industryStocks.put(PHASE,CHOICE);
            t.player.health=0;
            return;
        }
        // The child is the only living identity; the old visitor shell cannot
        // earn, shop, travel independently, vote or consume duplicate supplies.
        t.player.place=c.place;t.player.destination=c.place;
        t.player.health=c.health;t.player.hunger=c.hunger;t.player.thirst=c.thirst;t.player.fatigue=c.fatigue;
    }
    private static final String FOCUS="rebirth:focus", LAST_CHAT="rebirth:last-chat";
    static boolean linkedChild(TownLife.Town t,TownLife.Citizen c){
        return phase(t)==CHILDHOOD&&c==child(t)&&c.alive&&c.age<18;
    }
    static String focus(TownLife.Town t,TownLife.Citizen c){
        if(!linkedChild(t,c)||c.age<6)return null;
        int n=t.industryStocks.getOrDefault(FOCUS,0);
        return n>=1&&n<=EducationLife.SUBJECTS.size()?EducationLife.SUBJECTS.get(n-1):null;
    }
    static String chooseFocus(TownLife.Town t,String subject){
        var c=child(t);
        if(c==null||!linkedChild(t,c)||c.age<6)
            throw new IllegalArgumentException("School focus is available from age six until adulthood.");
        int n=EducationLife.SUBJECTS.indexOf(subject);
        if(n<0&&!"balanced".equals(subject))throw new IllegalArgumentException("Unknown school subject.");
        t.industryStocks.put(FOCUS,n+1);
        return "School focus: "+subject+". Skills grow only during actual lessons.";
    }
    static boolean awakePresent(TownLife.Citizen c){
        return c!=null&&c.alive&&c.travelRemaining==0&&!"SLEEPING".equals(c.activity);
    }
    static List<TownLife.Citizen> visible(TownLife.Town t){
        var c=child(t);
        if(c==null||!linkedChild(t,c)||c.travelRemaining>0)return List.of();
        return t.people.values().stream().filter(p->p!=c&&p.alive&&p.travelRemaining==0
                &&p.currentSpace().equals(c.currentSpace())).toList();
    }
    static List<TownLife.Citizen> conversationPartners(TownLife.Town t,int day){
        var c=child(t);
        if(c==null||!awakePresent(c)||c.age<3||t.industryStocks.getOrDefault(LAST_CHAT,0)>=day)return List.of();
        return visible(t).stream().filter(p->awakePresent(p)&&
                (p.generationIndex>0&&(p.generationIndex==DemographyLife.get(t,c,"parent-a")
                ||p.generationIndex==DemographyLife.get(t,c,"parent-b"))
                ||(p.age<18&&Math.abs(p.age-c.age)<=3))).toList();
    }
    static String chat(TownLife.Town t,int day,String id){
        var c=child(t);
        var other=conversationPartners(t,day).stream().filter(p->p.id.equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("No available childhood conversation with this person today."));
        String key=SocialNetwork.key(c.id,other.id);
        var relation=t.peerRelationships.computeIfAbsent(key,k->new SocialLife.Relationship(
                c.id.compareTo(other.id)<=0?c.id:other.id,c.id.compareTo(other.id)<=0?other.id:c.id));
        relation.affinity=Math.min(100,relation.affinity+2);
        relation.trust=Math.min(100,relation.trust+1);
        relation.conversations++;
        t.industryStocks.put(LAST_CHAT,day);
        return "Spent time talking with "+other.name+".";
    }
    record Conversation(String id,String name){}
    // Bound each request: early childhood is faster, adolescence is slower.
    static int maxWaitDays(TownLife.Town t){
        if(phase(t)==CHOICE)return 1;
        var c=child(t);
        if(phase(t)!=CHILDHOOD||c==null||!c.alive||c.age>=18)return 0;
        return c.age<6?7:c.age<13?3:1;
    }
    static boolean canContinueWaiting(TownLife.Town t,int initialPhase,int initialMaxDays){
        return phase(t)==initialPhase&&maxWaitDays(t)>0&&maxWaitDays(t)==initialMaxDays;
    }
    static String adultFallback(TownLife.Town t){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("Only available before choosing a family.");
        var p=new PlayerLife.Player();p.incarnation=t.player.incarnation;t.player=p;
        t.industryStocks.put(PHASE,ADULT);t.industryStocks.put(CHILD,0);
        return "Started an adult life in the park.";
    }
    record Report(String phase,List<Family> families,String childName,int age,String parentA,String parentB,String district,boolean preview,int maxWaitDays,String schoolFocus,Map<String,Integer> skills,int schoolDays,List<Conversation> conversations){}
    static Report report(TownLife.Town t,int day){
        int phase=phase(t);var c=child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        var skills=new LinkedHashMap<String,Integer>();
        if(c!=null)for(String subject:EducationLife.SUBJECTS)skills.put(subject,EducationLife.get(t,c,subject));
        var conversations=conversationPartners(t,day).stream().map(p->new Conversation(p.id,p.name)).toList();
        return new Report(phase==CHOICE?"CHOICE":phase==CHILDHOOD?"CHILDHOOD":"ADULT",phase==CHOICE?families(t,day):List.of(),
            c==null?null:c.name,c==null?0:c.age,a==null?null:a.name,b==null?null:b.name,c==null?null:c.home.label,phase==CHILDHOOD,maxWaitDays(t),c==null?null:focus(t,c),skills,c==null?0:EducationLife.get(t,c,"school-days"),conversations);
    }
}
