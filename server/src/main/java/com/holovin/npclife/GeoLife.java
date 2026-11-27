package com.holovin.npclife;
import java.util.*;

/** An adapter for the original linked life, not a second playable identity. */
final class GeoLife {
    record View(String phase,String name,int age,String activity,int savings,int maxWaitDays,
                int schoolDays,boolean graduated,String parentA,String parentB,boolean canWalk,int radius){}
    static View view(TownLife.Town t,WorldState s){
        int phase=RebirthLife.phase(t);var c=RebirthLife.child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        return new View(switch(phase){case RebirthLife.CHOICE->"CHOICE";case RebirthLife.CHILDHOOD->"CHILDHOOD";case RebirthLife.LINKED_ADULT->"LINKED_ADULT";default->"ADULT";},
            c==null?null:c.name,c==null?0:c.age,c==null?"":c.activity,c==null?t.player.money:c.money,RebirthLife.maxWaitDays(t),
            c==null?0:EducationLife.get(t,c,"school-days"),c!=null&&EducationLife.get(t,c,"graduate")>0,a==null?null:a.name,b==null?null:b.name,canWalk(t,s),24);
    }
    static List<GeoProjection.Choice> choices(TownLife.Town t,WorldState s,GeoProjection map){
        var result=new ArrayList<GeoProjection.Choice>();
        if(!RebirthLife.active(t)||JusticeLife.confined(t))return result;
        var life=view(t,s);var c=RebirthLife.child(t);
        if(life.phase().equals("CHOICE")){
            for(var f:RebirthLife.families(t,s.day))add(result,"rebirth-family-"+f.first()+"-"+f.second(),f.firstName()+" + "+f.secondName()+" · "+f.district()+" ("+f.firstJob()+" / "+f.secondJob()+")");
            add(result,"rebirth-wait","Wait 1 day / Attendi un giorno");
            add(result,"rebirth-adult","Start as a new adult / Inizia adulto");
            return List.copyOf(result);
        }
        if(c==null||!c.alive)return List.of();
        if(life.phase().equals("CHILDHOOD")){
            if(life.maxWaitDays()>0)add(result,"rebirth-wait","Advance 1 day / Avanza un giorno");
            if(life.maxWaitDays()>1)add(result,"rebirth-wait-fast","Advance up to "+life.maxWaitDays()+" days / Avanza");
            if(c.age>=18&&life.graduated())add(result,"rebirth-continue","Continue adult life / Vita adulta");
            var child=RebirthLife.childOptions(t,s.day,s.minute);
            if(child!=null){
                add(result,"rebirth-child-hour","Advance 1 hour / Avanza un'ora");
                for(String subject:EducationLife.SUBJECTS)add(result,"rebirth-focus-"+subject,"School focus / Studio: "+subject);
                add(result,"rebirth-focus-balanced","Balanced studies / Studio equilibrato");
                if(child.allowanceAvailable()>0)add(result,"rebirth-allowance","Ask allowance / Paghetta");
                for(var hobby:child.hobbies())add(result,"rebirth-hobby-"+hobby.id(),hobby.label());
                for(String id:child.destinations())add(result,"rebirth-child-travel-"+id,"Outing / Passeggiata → "+TownLife.Place.valueOf(id).label);
                if(child.outing())add(result,"rebirth-child-home","Return home / Torna a casa");
            }
        }else{
            add(result,"rebirth-wait-hour","Advance 1 hour / Avanza un'ora");
            add(result,"rebirth-wait-adult-day","Advance 1 day / Avanza un giorno");
            var adult=RebirthLife.adultOptions(t,s.day);
            var home=RebirthLife.homeOptions(t,s.day,s.minute);
            if(adult!=null){
                add(result,"rebirth-routine","Resume daily routine / Riprendi routine");
                for(String role:adult.jobs())add(result,"rebirth-job-"+role,"Choose occupation / Professione: "+role);
                for(String id:adult.destinations()){
                    var p=TownLife.Place.valueOf(id);
                    if(p!=c.place&&(!id.startsWith("OUTER_")||map.point(p).distance(map.point(c.place))<100))
                        add(result,"rebirth-travel-"+id,"Travel / Vai → "+p.label);
                }
            }
            var cafe=RebirthLife.cafe(t,s.day,s.minute);
            if(cafe!=null&&cafe.open()){
                for(var order:cafe.orders())if(order.available())add(result,"rebirth-cafe-"+order.id(),"Bar Centrale · "+order.label()+" · €"+order.price());
                if(cafe.canRest())add(result,"rebirth-cafe-rest","Coffee break / Pausa · 20 min");
                for(var guest:cafe.guests())add(result,"rebirth-cafe-chat-"+guest.id(),"Chat at cafe / Conversa: "+guest.name());
            }
            if(home!=null){
                for(String action:home.actions())add(result,"rebirth-home-"+action,"Home / Casa: "+action);
                if(home.atHome())add(result,"rebirth-home-exit","Leave home / Esci");
            }
        }
        for(var other:RebirthLife.conversationPartners(t,s.day))add(result,"rebirth-chat-"+other.id,"Talk / Parla: "+other.name);
        return List.copyOf(result);
    }
    private static void add(List<GeoProjection.Choice> choices,String id,String label){choices.add(new GeoProjection.Choice(id,label));}
    static boolean canWalk(TownLife.Town t,WorldState s){
        var c=RebirthLife.child(t);
        return RebirthLife.active(t)&&c!=null&&c.age>=6&&RebirthLife.awakePresent(c)&&!JusticeLife.confined(t)
                &&!EmploymentLife.sick(c,s.day)&&!"IN_CUSTODY".equals(c.activity)
                &&(RebirthLife.linkedChild(t,c)||RebirthLife.linkedAdult(t,c));
    }
    static boolean canTalk(TownLife.Town t,TownLife.Citizen c){
        var self=RebirthLife.child(t);
        return self!=null&&self.age>=3&&RebirthLife.awakePresent(self)&&RebirthLife.awakePresent(c)
                &&RebirthLife.visible(t).contains(c)&&!JusticeLife.confined(t);
    }
    static boolean atHome(TownLife.Citizen c){return c!=null&&c.alive&&c.travelRemaining==0&&c.currentSpace().equals(c.homeUnit);}
    static List<GeoProjection.Actor> household(TownLife.Town t,WorldState s,GeoProjection map){
        var self=RebirthLife.child(t);if(!atHome(self))return List.of();
        var center=map.point(self.place);
        var people=RebirthLife.visible(t).stream().filter(c->!"SLEEPING".equals(c.activity)).limit(8).toList();
        var result=new ArrayList<GeoProjection.Actor>();
        for(int i=0;i<people.size();i++){
            var c=people.get(i);
            result.add(new GeoProjection.Actor(c.id,center.x()-4.5+i%4*3,center.z()-1.5+i/4*3,HomeRoutineLife.activity(t,c,s.day,s.minute),canTalk(t,c),"STAND",0,"","",GameSession.appearanceStage(c)));
        }
        return List.copyOf(result);
    }
}
