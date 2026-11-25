package com.holovin.npclife;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/** Persistent family selection and linked newborn preview; adult controls remain locked. */
final class RebirthLife {
    private RebirthLife(){}
    static final String PHASE="rebirth:phase", CHILD="rebirth:child";
    static final int ADULT=0, CHOICE=1, CHILDHOOD=2, LINKED_ADULT=3;
    static void ensure(TownLife.Town t){
        t.industryStocks.putIfAbsent(PHASE,ADULT);t.industryStocks.putIfAbsent(CHILD,0);
        t.industryStocks.putIfAbsent(FOCUS,0);t.industryStocks.putIfAbsent(LAST_CHAT,0);
        t.industryStocks.putIfAbsent(TRAVEL,0);
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
        DemographyLife.set(t,newborn,"played-incarnation",t.player.incarnation);
        t.industryStocks.put(FOCUS,0);t.industryStocks.put(LAST_CHAT,0);t.industryStocks.put(TRAVEL,0);
        synchronize(t);
        return "Born as "+newborn.name+". Follow family life and discover new choices as you grow.";
    }
    static void synchronize(TownLife.Town t){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return;
        var c=child(t);
        if(c==null||!c.alive)return; // Death settlement needs the authoritative clock.

        // The child is the only living identity; the old visitor shell cannot
        // earn, shop, travel independently, vote or consume duplicate supplies.
        t.player.place=c.place;t.player.destination=c.place;
        t.player.health=c.health;t.player.hunger=c.hunger;t.player.thirst=c.thirst;t.player.fatigue=c.fatigue;
    }
    /** Bind old saves to the existing citizen; never create a replacement person. */
    static boolean restoreIdentity(TownLife.Town t){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return false;
        var c=child(t);
        if(c==null)throw new IllegalStateException("Linked resident is missing from the save.");
        int previous=DemographyLife.get(t,c,"played-incarnation");
        if(previous==t.player.incarnation)return false;
        if(previous!=0)throw new IllegalStateException("Linked resident has a different incarnation.");
        DemographyLife.set(t,c,"played-incarnation",t.player.incarnation);
        return true;
    }
    /** Settle a real resident, not the inactive visitor shell, exactly once. */
    static boolean settleLinkedDeath(TownLife.Town t,int day,int minute){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return false;
        restoreIdentity(t);
        var c=child(t);
        if(c.alive)return false;
        int incarnation=t.player.incarnation;
        if(incarnation==Integer.MAX_VALUE)throw new IllegalStateException("Maximum incarnation reached.");
        if(t.pastLives.containsKey(incarnation))throw new IllegalStateException("Life was already archived.");
        if(DemographyLife.get(t,c,"estate")==0){
            ArchiveLife.record(t,"death:"+c.id,day,minute,"Population",c.name+" was recorded as deceased.");
            DemographyLife.estate(t,c);
        }
        t.pastLives.put(incarnation,new PlayerLife.LifeRecord(incarnation,day,minute,c.place,c.name));
        // Legacy assets were already settled when this family life began.
        // Do not run visitor cleanup again or distribute the resident's wallet twice.
        var next=new PlayerLife.Player();
        next.incarnation=incarnation+1;next.alive=false;next.health=0;next.money=0;
        next.place=c.place;next.destination=c.place;
        t.player=next;
        t.industryStocks.put(CHILD,0);t.industryStocks.put(PHASE,CHOICE);
        t.industryStocks.put(FOCUS,0);t.industryStocks.put(LAST_CHAT,0);t.industryStocks.put(TRAVEL,0);
        return true;
    }
    record PastLife(int incarnation,String name,String residentId,int day,int minute,int age,String district){}
    static List<PastLife> history(TownLife.Town t){
        var result=new ArrayList<PastLife>();
        for(var record:t.pastLives.values()){
            var person=t.people.values().stream().filter(c->DemographyLife.get(t,c,"played-incarnation")==record.incarnation()).findFirst().orElse(null);
            result.add(new PastLife(record.incarnation(),record.privateName(),person==null?null:person.id,
                    record.day(),record.minute(),person==null?-1:person.age,record.place().label));
        }
        result.sort(java.util.Comparator.comparingInt(PastLife::incarnation).reversed());
        return List.copyOf(result);
    }
    private static final String FOCUS="rebirth:focus", LAST_CHAT="rebirth:last-chat", TRAVEL="rebirth:travel";
    static boolean linkedAdult(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&phase(t)==LINKED_ADULT&&c==child(t)&&c.alive&&c.age>=18;
    }
    static TownLife.Citizen requireAdult(TownLife.Town t){
        var c=child(t);
        if(!linkedAdult(t,c))throw new IllegalArgumentException("Continue after graduation first.");
        return c;
    }
    static String continueAdult(TownLife.Town t){
        var c=child(t);
        if(phase(t)!=CHILDHOOD||c==null||!c.alive||c.age<18||EducationLife.get(t,c,"graduate")==0)
            throw new IllegalArgumentException("Adulthood requires the existing graduate.");
        t.industryStocks.put(PHASE,LINKED_ADULT);
        return "Continue as "+c.name+". Your savings, education and relationships remain yours.";
    }
    static TownLife.Place manualDestination(TownLife.Town t,TownLife.Citizen c){
        if(!linkedAdult(t,c))return null;
        int n=t.industryStocks.getOrDefault(TRAVEL,0);
        var places=PlayerLife.publicPlaces();
        return n>=1&&n<=places.size()?places.get(n-1):null;
    }
    static int travel(TownLife.Town t,TownLife.Place destination,int day){
        var c=requireAdult(t);
        if(c.travelRemaining>0||EmploymentLife.sick(c,day))throw new IllegalArgumentException("Finish traveling or recover first.");
        int index=PlayerLife.publicPlaces().indexOf(destination);
        if(index<0)throw new IllegalArgumentException("Choose a public destination.");
        t.industryStocks.put(TRAVEL,index+1);
        if(c.place==destination)return 0;
        c.destination=destination;c.travelRemaining=TownLife.travelTime(c.place,destination);
        c.activity="TRAVELING";
        return c.travelRemaining;
    }
    static String resumeRoutine(TownLife.Town t){
        requireAdult(t);
        t.industryStocks.put(TRAVEL,0);
        return "Resumed daily work, shopping and home routine.";
    }
    static boolean continueAdultWaiting(TownLife.Town t){
        return linkedAdult(t,child(t));
    }
    record AdultOptions(String occupation,boolean employed,String activity,String destination,List<String> jobs,List<String> destinations){}
    static AdultOptions adultOptions(TownLife.Town t,int day){
        var c=child(t);
        if(!linkedAdult(t,c))return null;
        var destination=manualDestination(t,c);
        return new AdultOptions(c.occupation,c.employed,c.activity,destination==null?null:destination.name(),
                EmploymentLife.openRoles(t,c,day),c.travelRemaining==0&&!EmploymentLife.sick(c,day)?PlayerLife.publicPlaces().stream().map(Enum::name).toList():List.of());
    }
    static boolean linkedChild(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&phase(t)==CHILDHOOD&&c==child(t)&&c.alive&&c.age<18;
    }
    static String focus(TownLife.Town t,TownLife.Citizen c){
        if(!linkedChild(t,c)||c.age<6)return null;
        return storedFocus(t);
    }
    private static String storedFocus(TownLife.Town t){
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
        if(c==null||(!linkedChild(t,c)&&!linkedAdult(t,c))||c.travelRemaining>0)return List.of();
        return t.people.values().stream().filter(p->p!=c&&p.alive&&p.travelRemaining==0
                &&p.currentSpace().equals(c.currentSpace())).toList();
    }
    static List<TownLife.Citizen> conversationPartners(TownLife.Town t,int day){
        var c=child(t);
        if(c==null||!awakePresent(c)||c.age<3||t.industryStocks.getOrDefault(LAST_CHAT,0)>=day)return List.of();
        return visible(t).stream().filter(p->awakePresent(p)&&
                (p.generationIndex>0&&(p.generationIndex==DemographyLife.get(t,c,"parent-a")
                ||p.generationIndex==DemographyLife.get(t,c,"parent-b"))
                ||(c.age>=18?p.age>=18:(p.age<18&&Math.abs(p.age-c.age)<=3)))).toList();
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
    record KnownPerson(String id,String name,boolean family,boolean alive,int affinity,int trust,int conversations){}
    record Identity(String residentId,int generationIndex,int bornDay,int savings,boolean graduated,List<KnownPerson> relationships){}
    static Identity identity(TownLife.Town t,TownLife.Citizen c){
        if(c==null)return null;
        int a=DemographyLife.get(t,c,"parent-a"),b=DemographyLife.get(t,c,"parent-b");
        var known=new ArrayList<KnownPerson>();
        for(var p:t.people.values()){
            if(p==c)continue;
            boolean family=p.generationIndex>0&&(p.generationIndex==a||p.generationIndex==b);
            var relation=t.peerRelationships.get(SocialNetwork.key(c.id,p.id));
            if(!family&&(relation==null||relation.conversations==0))continue;
            known.add(new KnownPerson(p.id,p.name,family,p.alive,relation==null?0:relation.affinity,
                    relation==null?0:relation.trust,relation==null?0:relation.conversations));
        }
        known.sort(java.util.Comparator.comparing(KnownPerson::family).reversed()
                .thenComparing(KnownPerson::name).thenComparing(KnownPerson::id));
        return new Identity(c.id,c.generationIndex,DemographyLife.get(t,c,"born"),c.money,
                EducationLife.get(t,c,"graduate")>0,List.copyOf(known));
    }
    record Report(String phase,List<Family> families,String childName,int age,String parentA,String parentB,String district,boolean preview,int maxWaitDays,String schoolFocus,Map<String,Integer> skills,int schoolDays,List<Conversation> conversations,Identity identity,AdultOptions adultOptions,List<PastLife> pastLives){}
    static Report report(TownLife.Town t,int day){
        int phase=phase(t);var c=child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        var skills=new LinkedHashMap<String,Integer>();
        if(c!=null)for(String subject:EducationLife.SUBJECTS)skills.put(subject,EducationLife.get(t,c,subject));
        var conversations=conversationPartners(t,day).stream().map(p->new Conversation(p.id,p.name)).toList();
        return new Report(phase==CHOICE?"CHOICE":phase==CHILDHOOD?"CHILDHOOD":phase==LINKED_ADULT?"LINKED_ADULT":"ADULT",phase==CHOICE?families(t,day):List.of(),
            c==null?null:c.name,c==null?0:c.age,a==null?null:a.name,b==null?null:b.name,c==null?null:c.home.label,phase==CHILDHOOD,maxWaitDays(t),c==null||c.age<6?null:storedFocus(t),skills,c==null?0:EducationLife.get(t,c,"school-days"),conversations,identity(t,c),adultOptions(t,day),history(t));
    }
}
