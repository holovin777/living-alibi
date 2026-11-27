package com.holovin.npclife;

import java.util.List;

/** Missing-home incidents follow real resident routes, never a teleport or omniscient location lookup. */
final class ChildSearchLife {
    private ChildSearchLife(){}
    private static String key(TownLife.Citizen c,String f){return "child-search:"+c.id+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    private static int now(WorldState s){return Math.toIntExact((s.day-1L)*1440+s.minute);}
    private static final int WAITING_FOR_FAMILY=4;
    private static TownLife.Place lastSeen(TownLife.Town t,TownLife.Citizen c){
        int place=get(t,c,"last-seen-place");
        return place>0&&place<=TownLife.Place.values().length?TownLife.Place.values()[place-1]:null;
    }
    private static void sighting(TownLife.Town t,TownLife.Citizen c,WorldState s){
        set(t,c,"last-seen-place",c.place.ordinal()+1);
        set(t,c,"last-seen-at",now(s)+1);
    }
    private static final List<TownLife.Place> ROUTE=List.of(TownLife.Place.PARK,TownLife.Place.LIBRARY,TownLife.Place.SCHOOL,TownLife.Place.CENTRO,TownLife.Place.PORTICI,TownLife.Place.MARKET);
    record View(String status,String searchingParent,int minutesOverdue,int incidents,int contactsNotified,boolean policeReported,String searchingOfficer){}
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        var s=new WorldState();s.day=day;s.minute=minute;return view(t,c,s);
    }
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null||!DemographyLife.child(c))return null;
        int status=get(t,c,"status");
        var parent=DemographyLife.byIndex(t,get(t,c,"searcher"));
        return new View(switch(status){case 1->"OVERDUE";case 2->"SEARCHING";case 3->"LOCATED";case WAITING_FOR_FAMILY->"WAITING_FOR_FAMILY";default->"NONE";},
                parent==null?null:parent.name,status==0?0:Math.max(0,now(s)-get(t,c,"start")),get(t,c,"incidents"),
                get(t,c,"contacts"),get(t,c,"reported")>0,
                officerName(t,c));
    }
    private static String officerName(TownLife.Town t,TownLife.Citizen c){
        var officer=DemographyLife.byIndex(t,get(t,c,"officer"));
        return officer==null?null:officer.name;
    }
    /** A face-to-face request can start a search before any curfew timeout. */
    static void reportedBy(TownLife.Town t,TownLife.Citizen child,TownLife.Citizen helper,WorldState s){
        if(child==null||helper==null||!DemographyLife.child(child)||helper.age<18
                ||!helper.alive||!meet(helper,child))return;
        int status=get(t,child,"status");
        if(status==0||status==3){
            release(t,child);releaseOfficer(t,child);
            set(t,child,"status",1);set(t,child,"start",now(s));
            set(t,child,"incidents",Math.addExact(get(t,child,"incidents"),1));
            set(t,child,"departed",now(s)+1);
            set(t,child,"contacts",0);set(t,child,"contact-attempt",0);
            set(t,child,"reported",0);
        }
        if(get(t,child,"reported")==0){
            set(t,child,"reported",1);
            ArchiveLife.record(t,"child-report:"+child.id+":"+get(t,child,"incidents"),s.day,s.minute,
                    "Family","An adult reported a lost child to police after meeting the child.");
        }
        // This information comes from a real face-to-face encounter.
        sighting(t,child,s);
        assignParent(t,child,s);
        // Prefer the actual officer who heard the child, rather than an
        // unrelated officer elsewhere on patrol.
        if(PoliceLife.officer(helper)&&helper.generationIndex>0
                &&(get(t,helper,"assigned")==0||get(t,helper,"assigned")==child.generationIndex)
                &&!EmploymentLife.sick(helper,s.day)&&!NpcJusticeLife.confined(t,helper,s)){
            var previous=DemographyLife.byIndex(t,get(t,child,"officer"));
            if(previous!=helper){
                releaseOfficer(t,child);
                set(t,child,"officer",helper.generationIndex);
                set(t,helper,"assigned",child.generationIndex);
                set(t,child,"police-start",now(s));
            }
            foundByPolice(t,child,s);
        }else police(t,child,s);
    }
    static void foundWithParent(TownLife.Town t,TownLife.Citizen child,TownLife.Citizen parent,WorldState s){
        if(FamilyDialogueLife.parentOf(t,parent,child)&&meet(parent,child)
                &&get(t,child,"status")>0&&get(t,child,"status")!=3)
            reunited(t,child,parent,s);
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
        if(officer!=null&&(!officer.alive||!PoliceLife.officer(officer)
                ||(get(t,c,"status")!=WAITING_FOR_FAMILY&&!PoliceLife.scheduled(officer,s.day,s.minute))
                ||EmploymentLife.sick(officer,s.day)||NpcJusticeLife.confined(t,officer,s))){
            releaseOfficer(t,c);officer=null;
        }
        if(officer==null){
            officer=t.people.values().stream().filter(p->p.alive&&PoliceLife.officer(p)&&PoliceLife.scheduled(p,s.day,s.minute)
                    &&get(t,p,"assigned")==0&&!EmploymentLife.sick(p,s.day)&&!NpcJusticeLife.confined(t,p,s)&&!RebirthLife.linkedAdult(t,p)).findFirst().orElse(null);
            if(officer!=null){set(t,c,"officer",officer.generationIndex);set(t,officer,"assigned",c.generationIndex);set(t,c,"police-start",now(s));}
        }
        if(meet(officer,c))foundByPolice(t,c,s);
    }
    private static void foundByPolice(TownLife.Town t,TownLife.Citizen c,WorldState s){
        sighting(t,c,s);
        if(get(t,c,"status")!=WAITING_FOR_FAMILY){
            set(t,c,"status",WAITING_FOR_FAMILY);
            ArchiveLife.record(t,"child-police-found:"+c.id+":"+get(t,c,"incidents"),s.day,s.minute,
                    "Family","A police officer found a missing child and notified the family of the meeting district.");
        }
        assignParent(t,c,s);
    }
    private static void reunited(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen parent,WorldState s){
        boolean policeCase=get(t,c,"reported")>0;
        release(t,c);releaseOfficer(t,c);set(t,c,"status",3);
        if(policeCase&&!RebirthLife.linkedAdult(t,parent)&&get(t,parent,"assigned")==0){
            // Give the family time to actually speak, rather than immediately
            // sending the arriving parent back to their ordinary routine.
            sighting(t,c,s);
            set(t,c,"searcher",parent.generationIndex);
            set(t,parent,"assigned",c.generationIndex);
            set(t,c,"reunion-until",now(s)+15);
        }
        ArchiveLife.record(t,"child-located:"+c.id+":"+get(t,c,"incidents"),s.day,s.minute,
                "Family",c.name+" was reunited with a parent.");
    }
    private static void assignParent(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(get(t,c,"searcher")!=0)return;
        boolean reported=get(t,c,"reported")>0;
        for(var p:ParentingLife.parents(t,c)){
            if(!eligible(t,p,s,reported))continue;
            set(t,c,"searcher",p.generationIndex);set(t,p,"assigned",c.generationIndex);
            if(get(t,c,"status")!=WAITING_FOR_FAMILY)set(t,c,"status",2);
            set(t,c,"route-start",now(s));return;
        }
    }
    private static boolean eligible(TownLife.Town t,TownLife.Citizen p,WorldState s,boolean reported){
        return p!=null&&p.alive&&p.age>=18&&!RebirthLife.linkedAdult(t,p)&&!EmploymentLife.sick(p,s.day)
                &&!NpcJusticeLife.confined(t,p,s)&&(reported||!"WORKING".equals(p.activity))
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
                set(t,c,"last-seen-place",0);set(t,c,"last-seen-at",0);set(t,c,"reunion-until",0);
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
            if(status==3){
                if(now(s)>=get(t,c,"reunion-until"))release(t,c);
                continue;
            }
            var parent=DemographyLife.byIndex(t,get(t,c,"searcher"));
            if(parent!=null&&(!parent.alive||EmploymentLife.sick(parent,s.day)||NpcJusticeLife.confined(t,parent,s))){
                release(t,c);
                if(status!=WAITING_FOR_FAMILY)set(t,c,"status",1);
                parent=null;
            }
            // The child's actual meeting with a parent completes the case.
            var arrived=ParentingLife.parents(t,c).stream().filter(p->meet(p,c)).findFirst().orElse(null);
            if(arrived!=null){reunited(t,c,arrived,s);continue;}
            if(status==WAITING_FOR_FAMILY){
                var officer=DemographyLife.byIndex(t,get(t,c,"officer"));
                if(meet(officer,c))sighting(t,c,s);
                else if(now(s)-(get(t,c,"last-seen-at")-1)>=30){
                    // Lost contact: search the last reported area, not the
                    // child's unknown live location.
                    set(t,c,"status",get(t,c,"searcher")>0?2:1);
                    releaseOfficer(t,c);
                }
            }
            assignParent(t,c,s);
            int overdue=now(s)-get(t,c,"start");
            if(overdue>=15)notifyContacts(t,c,s);
            if(get(t,c,"reported")>0||overdue>=30&&!ParentingLife.parents(t,c).isEmpty())police(t,c,s);
        }
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen p,WorldState s){
        int assigned=get(t,p,"assigned");
        if(assigned==0)return null;
        var child=DemographyLife.byIndex(t,assigned);
        if(child==null||!child.alive||child.age>=18||get(t,child,"status")==0||HomeRoutineLife.atHome(child))return null;
        boolean officer=get(t,child,"officer")==p.generationIndex;
        if(!officer&&get(t,child,"searcher")!=p.generationIndex)return null;
        int status=get(t,child,"status");
        var seen=lastSeen(t,child);
        if(status==3)return !officer&&now(s)<get(t,child,"reunion-until")?seen:null;
        if(seen!=null&&(status==WAITING_FOR_FAMILY||now(s)-(get(t,child,"last-seen-at")-1)<60))return seen;
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
