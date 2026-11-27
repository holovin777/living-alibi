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
    record View(String status,String searchingParent,int minutesOverdue,int incidents){}
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null||!DemographyLife.child(c))return null;
        int status=get(t,c,"status");
        var parent=DemographyLife.byIndex(t,get(t,c,"searcher"));
        return new View(switch(status){case 1->"OVERDUE";case 2->"SEARCHING";case 3->"LOCATED";default->"NONE";},
                parent==null?null:parent.name,status==0?0:Math.max(0,now(s)-get(t,c,"start")),get(t,c,"incidents"));
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
                if(status!=0){release(t,c);set(t,c,"status",0);}
                continue;
            }
            if(c.age<6)continue;
            if(status==0){
                // The usual school trip is not a missing-home incident.
                if(s.minute<RebirthLife.outingEnd(c))continue;
                if(s.minute>=1320&&c.place==c.home&&c.travelRemaining==0)continue;
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
            if(parent!=null&&parent.travelRemaining==0&&c.travelRemaining==0&&parent.place==c.place
                    &&parent.currentSpace().equals(c.currentSpace())){
                release(t,c);set(t,c,"status",3);
                ArchiveLife.record(t,"child-located:"+c.id+":"+get(t,c,"incidents"),s.day,s.minute,"Family",c.name+" was located by a parent.");
            }
        }
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen p,WorldState s){
        int assigned=get(t,p,"assigned");
        if(assigned==0)return null;
        var child=DemographyLife.byIndex(t,assigned);
        if(child==null||!child.alive||get(t,child,"status")!=2||get(t,child,"searcher")!=p.generationIndex||HomeRoutineLife.atHome(child))return null;
        int step=Math.max(0,(now(s)-get(t,child,"route-start"))/60);
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
