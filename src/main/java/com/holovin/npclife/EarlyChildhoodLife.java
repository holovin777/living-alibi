package com.holovin.npclife;

import java.util.*;

/** Early years: small interactive activities with actual household members. */
final class EarlyChildhoodLife {
    private EarlyChildhoodLife(){}
    record View(boolean atHome,List<String> parentsPresent,List<String> actions){}
    static boolean atHome(TownLife.Citizen c){
        return c!=null&&c.alive&&c.travelRemaining==0&&c.place==c.home&&c.currentSpace().equals(c.homeUnit);
    }
    static List<TownLife.Citizen> parents(TownLife.Town t,TownLife.Citizen c){
        var result=new ArrayList<TownLife.Citizen>();
        for(String field:List.of("parent-a","parent-b")){
            var p=DemographyLife.byIndex(t,DemographyLife.get(t,c,field));
            if(p!=null&&RebirthLife.awakePresent(p)&&p.currentSpace().equals(c.currentSpace()))result.add(p);
        }
        return result;
    }
    static View view(TownLife.Town t){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedChild(t,c)||c.age>=6)return null;
        boolean home=atHome(c);
        var present=home?parents(t,c):List.<TownLife.Citizen>of();
        var actions=new ArrayList<String>();
        if(home){
            actions.add("rest");
            if(RebirthLife.awakePresent(c)){
                actions.add("call");
                if(!present.isEmpty()||c.age>=3)actions.add("play");
            }
        }
        return new View(home,present.stream().map(p->p.name).toList(),List.copyOf(actions));
    }
    static RebirthLife.HomeAction act(TownLife.Town t,String action){
        var view=view(t);
        if(view==null||!view.actions().contains(action))throw new IllegalArgumentException("This childhood activity is unavailable.");
        var c=RebirthLife.child(t);
        var present=parents(t,c);
        if("rest".equals(action)){
            c.fatigue=Math.max(0,c.fatigue-15);
            return new RebirthLife.HomeAction(60,"Rested at home.");
        }
        if("call".equals(action)&&present.isEmpty())
            return new RebirthLife.HomeAction(5,"No awake parent is in the room right now.");
        if(!present.isEmpty()){
            var parent=present.getFirst();
            var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(c.id,parent.id),k->new SocialLife.Relationship(c.id,parent.id));
            relation.affinity=Math.min(100,relation.affinity+1);
            if("call".equals(action)){
                if(c.food<2&&parent.food>1){parent.food--;c.food++;}
                if(c.water<2&&parent.water>1){parent.water--;c.water++;}
                return new RebirthLife.HomeAction(10,parent.name+" came to comfort you.");
            }
            return new RebirthLife.HomeAction(20,"Played with "+parent.name+".");
        }
        return new RebirthLife.HomeAction(20,"Played at home.");
    }
}
