package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** Persistent district damage and resource-backed municipal restoration. */
final class FireRecoveryLife {
    private FireRecoveryLife(){}
    static final int CLOSED_BELOW=70, REPAIR=20, MATERIAL_COST=4, LABOR=6;
    private static String key(TownLife.Place p,String f){return "fire-recovery:"+p.name()+":"+f;}
    static int condition(TownLife.Town t,TownLife.Place p){return t.industryStocks.getOrDefault(key(p,"condition"),100);}
    static boolean closed(TownLife.Town t,TownLife.Place p){return p!=null&&FireServiceLife.SITES.contains(p)&&(FireServiceLife.site(t)==p||condition(t,p)<CLOSED_BELOW);}
    static void damage(TownLife.Town t,TownLife.Place p,int amount){
        if(!FireServiceLife.SITES.contains(p)||amount<=0)return;
        int before=condition(t,p),after=Math.max(0,before-amount);
        t.industryStocks.put(key(p,"condition"),after);
        HomeInsuranceLife.damage(t,p,before-after);
    }
    static boolean resources(TownLife.Town t,TownLife.Place p){
        return FireServiceLife.SITES.contains(p)&&FireServiceLife.site(t)!=p&&condition(t,p)<100
            &&FactoryNetworkLife.stock(t,"building-material")>0
            &&t.employerFunds.getOrDefault("city-maintenance",0)>=MATERIAL_COST+LABOR;
    }
    static boolean canRepair(TownLife.Town t,WorldState s,TownLife.Place p){
        var player=t.player;
        return player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&player.travelRemaining==0&&!player.cycling
            &&player.place==p&&player.engineeringSkill>=3&&s.minute>=480&&s.minute+60<=1080&&resources(t,p);
    }
    private static void repair(TownLife.Town t,TownLife.Place p){
        FactoryNetworkLife.change(t,"building-material",-1);
        t.employerFunds.merge("city-maintenance",-MATERIAL_COST-LABOR,Integer::sum);
        t.employerFunds.merge(FactoryNetworkLife.supplier("building-material"),MATERIAL_COST,Math::addExact);
        t.industryStocks.put(key(p,"condition"),Math.min(100,condition(t,p)+REPAIR));
        t.industryStocks.merge(key(p,"repairs"),1,Math::addExact);
    }
    static String playerRepair(TownLife.Town t,WorldState s,TownLife.Place p){
        if(!canRepair(t,s,p))throw new IllegalArgumentException("Restoration requires engineering 3, building material and municipal funds.");
        repair(t,p);t.player.money=Math.addExact(t.player.money,LABOR);
        return "Restoration / Ripristino: "+p.label+" condition "+condition(t,p)+"%. Earned EUR "+LABOR+".";
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=840)return;
        var p=FireServiceLife.SITES.stream().filter(x->resources(t,x)&&t.industryStocks.getOrDefault(key(x,"day"),0)<s.day)
            .min(Comparator.comparingInt(x->condition(t,x))).orElse(null);
        if(p==null||t.industryStocks.getOrDefault("fire-recovery:last-auto-day",0)>=s.day)return;
        var worker=t.people.values().stream().filter(c->"Carpenter".equals(c.occupation)&&c.presentAt(TownLife.Place.WORKSHOP)
            &&WorkplaceHierarchy.onDuty(c,s.day)&&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c))
            .min(Comparator.comparing(c->c.id)).orElse(null);
        if(worker==null)return;
        repair(t,p);worker.money=Math.addExact(worker.money,LABOR);
        t.industryStocks.put(key(p,"day"),s.day);t.industryStocks.put("fire-recovery:last-auto-day",s.day);
    }
    record District(String id,String label,int condition,boolean closed,int repairs){}
    static List<District> view(TownLife.Town t){
        return FireServiceLife.SITES.stream().filter(p->condition(t,p)<100||FireServiceLife.site(t)==p)
            .map(p->new District(p.name(),p.label,condition(t,p),closed(t,p),t.industryStocks.getOrDefault(key(p,"repairs"),0))).toList();
    }
}
