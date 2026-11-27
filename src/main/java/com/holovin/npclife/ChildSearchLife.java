package com.holovin.npclife;

import java.util.List;

/** Missing-home incidents follow real resident routes, never a teleport or omniscient location lookup. */
final class ChildSearchLife {
    private ChildSearchLife(){}
    private static String key(TownLife.Citizen c,String f){return "child-search:"+c.id+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    private static int now(WorldState s){return Math.toIntExact((s.day-1L)*1440+s.minute);}
    private static final List<TownLife.Place> ROUTE=List.of(TownLife.Place.PARK,TownLife.Place.LIBRARY,TownLife.Place.SCHOOL,TownLife.Place.CENTRO,TownLife.Place.PORTICI,TownLife.Place.MARKET);
    record View(String status,String searchingParent,int minutesOverdue,int incidents,int contactsNotified,boolean policeReported,String searchingOfficer){}
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        var s=new WorldState();s.day=day;s.minute=minute;return view(t,c,s);
    }
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null||!DemographyLife.child(c))return null;
        int status=get(t,c,"status");
        var parent=DemographyLife.byIndex(t,get(t,c,"searcher"));
        return new View(switch(status){case 1->"OVERDUE";case 2->"SEARCHING";case 3->"LOCATED";default->"NONE";},
                parent==null?null:parent.name,status==0?0:Math.max(0,now(s)-get(t,c,"start")),get(t,c,"incidents"),
                get(t,c,"contacts"),get(t,c,"reported")>0,
                officerName(t,c));
    }
    private static String officerName(TownLife.Town t,TownLife.Citizen c){
        var officer=DemographyLife.byIndex(t,get(t,c,"officer"));
        return officer==null?null:officer.name;
    }
    static void departed(TownLife.Town t,TownLife.Citizen c,WorldState s){
        set(t,c,"departed",now(s)+1);
    }
    private static void releaseOfficer(TownLife.Town t,TownLife.Citizen c){
        var officer=DemographyLife.byIndex(t,get(t,c,"officer"));
        if(officer!=null&&get(t,officer,"assigned")==c.generationIndex)set(t,officer,"assigned",0);
        set(t,c,"officer",0);
    }
    private static boolean meet(TownLife.Citizen a,TownLife.Citizen b){
        return a!=null&&a.alive&&a.travelRemaining==0&&b.travelRemaining==0
                &&a.place==b.place&&a.currentSpace().equals(b.currentSpace());
    }
    private static void notifyContacts(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(get(t,c,"contact-attempt")>0)return;
        set(t,c,"contact-attempt",1);
        var informed=new java.util.HashSet<String>();
        for(var p:ParentingLife.parents(t,c)){
            for(var friend:t.people.values()){
                if(friend==p||friend==c||!friend.alive||friend.age<18)continue;
                var rel=t.existingRelationship(p.id,friend.id);
                if(rel==null||rel.trust<55||rel.conversations<1||!informed.add(friend.id))continue;
                ArchiveLife.record(t,"child-contact:"+c.id+":"+get(t,c,"incidents")+":"+friend.id,s.day,s.minute,"Family",
                        p.name+" asked "+friend.name+" for news of a missing child.");
                if(informed.size()>=3)break;
            }
            if(informed.size()>=3)break;
        }
        set(t,c,"contacts",informed.size());
    }
    private static void police(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(get(t,c,"reported")==0){
            set(t,c,"reported",1);
            ArchiveLife.record(t,"child-report:"+c.id+":"+get(t,c,"incidents"),s.day,s.minute,"Family","A family reported a missing child to police.");
        }
        var officer=DemographyLife.byIndex(t,get(t,c,"officer"));
        if(officer!=null&&(!officer.alive||!PoliceLife.officer(officer)||!PoliceLife.scheduled(officer,s.day,s.minute)||EmploymentLife.sick(officer,s.day)||NpcJusticeLife.confined(t,officer,s))){
            releaseOfficer(t,c);officer=null;
        }
        if(officer==null){
            officer=t.people.values().stream().filter(p->p.alive&&PoliceLife.officer(p)&&PoliceLife.scheduled(p,s.day,s.minute)
                    &&get(t,p,"assigned")==0&&!EmploymentLife.sick(p,s.day)&&!NpcJusticeLife.confined(t,p,s)&&!RebirthLife.linkedAdult(t,p)).findFirst().orElse(null);
            if(officer!=null){set(t,c,"officer",officer.generationIndex);set(t,officer,"assigned",c.generationIndex);set(t,c,"police-start",now(s));}
        }
        if(meet(officer,c))located(t,c,s,"a police officer");
    }
    private static void located(TownLife.Town t,TownLife.Citizen c,WorldState s,String who){
        release(t,c);releaseOfficer(t,c);set(t,c,"status",3);
        ArchiveLife.record(t,"child-located:"+c.id+":"+get(t,c,"incidents"),s.day,s.minute,"Family",c.name+" was located by "+who+".");
    }
    private static boolean eligible(TownLife.Town t,TownLife.Citizen p,WorldState s){
        return p!=null&&p.alive&&p.age>=18&&!RebirthLife.linkedAdult(t,p)&&!EmploymentLife.sick(p,s.day)
                &&!NpcJusticeLife.confined(t,p,s)&&!"WORKING".equals(p.activity)
                &&get(t,p,"assigned")==0;
    }
    private static void release(TownLife.Town t,TownLife.Citizen child){
        var p=DemographyLife.byIndex(t,get(t,child,"searcher"));
        if(p!=null&&get(t,p,"assigned")==child.generationIndex)set(t,p,"assigned",0);
        set(t,child,"searcher",0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute%15!=0)return;
        for(var c:t.people.values()){
            int status=get(t,c,"status");
            if(!c.alive||!DemographyLife.child(c)||HomeRoutineLife.atHome(c)){
                if(status!=0){release(t,c);releaseOfficer(t,c);set(t,c,"status",0);}
                set(t,c,"departed",0);set(t,c,"contacts",0);set(t,c,"reported",0);set(t,c,"contact-attempt",0);
                continue;
            }
            boolean free=RebirthLife.freeChild(t,c);
            if(c.age<6&&!free)continue;
            if(status==0){
                // The usual school trip is not a missing-home incident.
                if(free){
                    if(get(t,c,"departed")==0){departed(t,c,s);continue;}
                    if(now(s)-(get(t,c,"departed")-1)<15)continue;
                }else{
                    if(s.minute<RebirthLife.outingEnd(c))continue;
                    if(s.minute>=1320&&c.place==c.home&&c.travelRemaining==0)continue;
                }
                set(t,c,"start",now(s));set(t,c,"status",1);
                set(t,c,"incidents",Math.addExact(get(t,c,"incidents"),1));status=1;
            }
            if(status==3)continue;
            var parent=DemographyLife.byIndex(t,get(t,c,"searcher"));
            if(parent!=null&&(!parent.alive||EmploymentLife.sick(parent,s.day)||NpcJusticeLife.confined(t,parent,s))){
                release(t,c);set(t,c,"status",1);parent=null;
            }
            if(parent==null){
                for(String field:List.of("parent-a","parent-b")){
                    var p=DemographyLife.byIndex(t,DemographyLife.get(t,c,field));
                    if(eligible(t,p,s)){
                        parent=p;set(t,c,"searcher",p.generationIndex);set(t,p,"assigned",c.generationIndex);
                        set(t,c,"status",2);set(t,c,"route-start",now(s));break;
                    }
                }
            }
            if(meet(parent,c)){located(t,c,s,"a parent");continue;}
            int overdue=now(s)-get(t,c,"start");
            if(overdue>=15)notifyContacts(t,c,s);
            if(overdue>=30&&!ParentingLife.parents(t,c).isEmpty())police(t,c,s);
        }
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen p,WorldState s){
        int assigned=get(t,p,"assigned");
        if(assigned==0)return null;
        var child=DemographyLife.byIndex(t,assigned);
        if(child==null||!child.alive||child.age>=18||get(t,child,"status")==0||get(t,child,"status")==3||HomeRoutineLife.atHome(child))return null;
        boolean officer=get(t,child,"officer")==p.generationIndex;
        if(!officer&&get(t,child,"searcher")!=p.generationIndex)return null;
        int step=Math.max(0,(now(s)-get(t,child,officer?"police-start":"route-start"))/60);
        return ROUTE.get(Math.floorMod(step,ROUTE.size()));
    }
    /** Age-appropriate autonomous after-school plans; stable for each resident and day. */
    static TownLife.Place leisure(TownLife.Citizen c,WorldState s){
        if(c.age<6||s.minute<840)return c.home;
        int end=RebirthLife.outingEnd(c);
        int choice=Math.floorMod(c.personalSeed+s.day*31L,5);
        // Some teenagers occasionally stay out after the expected return time.
        if(c.age>=13&&choice==4)end+=60;
        if(s.minute>=end)return c.home;
        if(choice==0)return c.home;
        if(choice==1||choice==4)return TownLife.Place.PARK;
        if(choice==2)return TownLife.Place.LIBRARY;
        return c.age>=13?TownLife.Place.CENTRO:TownLife.Place.PARK;
    }
}
