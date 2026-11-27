package com.holovin.npclife;

import java.util.*;

/** Fictional parenting preferences, shared by player and non-player households.
 * Requests arise only during actual meetings; no compulsory eviction or debt.
 */
final class ParentingLife {
    private ParentingLife(){}
    enum Style { WARM, STRICT, INDEPENDENCE, DISTANT }
    enum Request { NONE, HELP, CONTRIBUTE, FIND_WORK }
    static Style style(TownLife.Citizen parent){
        int variation=Math.floorMod(parent.personalSeed,41)-20;
        if(parent.sociability+variation<35)return Style.DISTANT;
        if(parent.carefulness+variation>80)return Style.STRICT;
        if(parent.curiosity+variation>70)return Style.INDEPENDENCE;
        return Style.WARM;
    }
    static String key(TownLife.Citizen child,TownLife.Citizen parent,String f){
        return "childhood:"+child.id+":parent:"+parent.generationIndex+":"+f;
    }
    static int get(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p,String f){return t.industryStocks.getOrDefault(key(c,p,f),0);}
    static void put(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p,String f,int n){t.industryStocks.put(key(c,p,f),n);}
    static List<TownLife.Citizen> parents(TownLife.Town t,TownLife.Citizen c){
        if(c==null||c.generationIndex<=0)return List.of();
        var result=new ArrayList<TownLife.Citizen>();
        for(String field:List.of("parent-a","parent-b")){
            var p=DemographyLife.byIndex(t,DemographyLife.get(t,c,field));
            if(p!=null&&p!=c&&p.alive&&!result.contains(p))result.add(p);
        }
        return result;
    }
    static boolean together(TownLife.Citizen c,TownLife.Citizen p){
        return RebirthLife.awakePresent(c)&&RebirthLife.awakePresent(p)&&HomeRoutineLife.atHome(c)
                &&HomeRoutineLife.atHome(p)&&c.homeUnit.equals(p.homeUnit);
    }
    static SocialLife.Relationship relation(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p){
        String a=c.id.compareTo(p.id)<0?c.id:p.id,b=a.equals(c.id)?p.id:c.id;
        return t.peerRelationships.computeIfAbsent(SocialNetwork.key(a,b),k->new SocialLife.Relationship(a,b));
    }
    static Request request(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p){
        return Request.values()[Math.max(0,Math.min(Request.values().length-1,get(t,c,p,"request")))];
    }
    static Request preferred(TownLife.Citizen c,TownLife.Citizen p){
        if(c.age<6)return Request.NONE;
        Style style=style(p);
        if(c.age<18)return style==Style.STRICT||style==Style.INDEPENDENCE?Request.HELP:Request.NONE;
        if(style==Style.INDEPENDENCE||style==Style.STRICT){
            if(!c.employed)return Request.FIND_WORK;
            return Request.CONTRIBUTE;
        }
        if(style==Style.WARM)return Request.HELP;
        return c.employed?Request.CONTRIBUTE:Request.NONE;
    }
    static int amount(TownLife.Citizen c,TownLife.Citizen p){return style(p)==Style.STRICT?10:5;}
    static void prompt(TownLife.Town t,TownLife.Citizen c,int day){
        for(var p:parents(t,c)){
            if(!together(c,p)||get(t,c,p,"day")>=day)continue;
            Request next=preferred(c,p);
            if(next==Request.NONE)continue;
            put(t,c,p,"day",day);put(t,c,p,"request",next.ordinal());
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=420&&s.minute!=1080&&s.minute!=1200)return;
        for(var c:t.people.values())if(c.alive&&c.generationIndex>0){
            for(var p:parents(t,c)){
                if(!RebirthLife.together(c,p)){put(t,c,p,"request",0);continue;}
                // A job actually obtained fulfils the parent's request.
                if(request(t,c,p)==Request.FIND_WORK&&c.employed){
                    respond(t,c,p,true);
                }
            }
            prompt(t,c,s.day);
            if(c==RebirthLife.child(t))continue;
            for(var p:parents(t,c)){
                if(!together(c,p))continue;
                Request request=request(t,c,p);
                if(request==Request.HELP&&c.carefulness>=45)respond(t,c,p,true);
                else if(request==Request.CONTRIBUTE&&c.money>=amount(c,p)+30)respond(t,c,p,true);
            }
        }
    }
    static void respond(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p,boolean accept){
        Request request=request(t,c,p);
        if(request==Request.NONE)throw new IllegalArgumentException("No pending family request.");
        if(accept&&request==Request.CONTRIBUTE){
            int sum=amount(c,p);
            if(c.age<18||c.money<sum+30)throw new IllegalArgumentException("Keep at least EUR 30 for your own needs.");
            int newWallet=Math.addExact(p.money,sum);
            c.money-=sum;p.money=newWallet;
        }
        if(accept&&request==Request.FIND_WORK&&!c.employed)throw new IllegalArgumentException("Find a job to fulfil this request.");
        if(accept&&request==Request.HELP){
            String dirt="home-routine:"+c.homeUnit+":dirt";
            t.industryStocks.put(dirt,Math.max(0,t.industryStocks.getOrDefault(dirt,0)-10));
        }
        var r=relation(t,c,p);
        if(accept){r.affinity=Math.min(100,r.affinity+2);r.trust=Math.min(100,r.trust+1);r.tension=Math.max(0,r.tension-2);}
        else {int pressure=style(p)==Style.STRICT?5:style(p)==Style.INDEPENDENCE?3:1;
            r.tension=Math.min(100,r.tension+pressure);r.affinity=Math.max(0,r.affinity-1);}
        r.conversations++;
        put(t,c,p,"request",0);
    }
    record Notice(String parentId,String parentName,String request,String message,String messageIt,int euros,boolean canAccept){}
    static List<Notice> view(TownLife.Town t,TownLife.Citizen c){
        var result=new ArrayList<Notice>();
        for(var p:parents(t,c)){
            if(!together(c,p))continue;
            Request q=request(t,c,p);
            if(q==Request.NONE)continue;
            String en=switch(q){case HELP->"Could you help around the house?";case CONTRIBUTE->"It's time to contribute to the household expenses.";case FIND_WORK->"You're grown up now. I want you to find work and support yourself.";default->"";};
            String it=switch(q){case HELP->"Mi dai una mano in casa?";case CONTRIBUTE->"È ora di contribuire alle spese di casa.";case FIND_WORK->"Ormai sei grande. Vorrei che trovassi un lavoro e ti mantenessi da solo.";default->"";};
            int euros=q==Request.CONTRIBUTE?amount(c,p):0;
            result.add(new Notice(p.id,p.name,q.name(),en,it,euros,q==Request.HELP||q==Request.CONTRIBUTE&&c.money>=euros+30||q==Request.FIND_WORK&&c.employed));
        }
        return List.copyOf(result);
    }
    static RebirthLife.HomeAction act(TownLife.Town t,String action){
        var c=RebirthLife.child(t);
        String[] parts=action.split(":",2);
        if(c==null||parts.length!=2||!Set.of("accept","decline").contains(parts[0]))throw new IllegalArgumentException("Invalid family response.");
        var p=parents(t,c).stream().filter(x->x.id.equals(parts[1])&&together(c,x)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Your parent is not available at home."));
        Request q=request(t,c,p);
        boolean accept=parts[0].equals("accept");
        respond(t,c,p,accept);
        return new RebirthLife.HomeAction(accept&&q==Request.HELP?15:1,accept?"You helped "+p.name+".":"You declined "+p.name+"'s request.");
    }
}
