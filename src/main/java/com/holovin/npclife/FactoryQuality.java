package com.holovin.npclife;

/** Deterministic quality inspection. Rejected units are scrapped, never sold.
 * Defect credit persists across batches so single-unit plants can also have waste.
 * Supplier reputation is a simplified yield score, not actual customer feedback. */
final class FactoryQuality {
    private FactoryQuality(){}
    static String key(FactoryNetworkLife.Plant p,String field){
        return "factory-quality:"+p.id()+":"+field;
    }
    private static int get(TownLife.Town t,FactoryNetworkLife.Plant p,String field){
        return t.industryStocks.getOrDefault(key(p,field),0);
    }
    private static void set(TownLife.Town t,FactoryNetworkLife.Plant p,String field,int value){
        t.industryStocks.put(key(p,field),value);
    }
    static int rejected(TownLife.Town t,FactoryNetworkLife.Plant p){return get(t,p,"rejected");}
    static int accepted(TownLife.Town t,FactoryNetworkLife.Plant p){return get(t,p,"accepted");}
    static int reputation(TownLife.Town t,FactoryNetworkLife.Plant p){
        return t.industryStocks.getOrDefault(key(p,"reputation"),80);
    }
    static int risk(TownLife.Town t,FactoryNetworkLife.Plant p,int day){
        int equipment=Math.max(0,80-FactoryMaintenance.condition(t,p))/2;
        int team=day<=0?0:t.people.values().stream().filter(c->WorkplaceHierarchy.onDuty(c,day)
                &&c.workplace==p.site()&&p.occupation().equals(c.occupation))
                .map(c->WorkplacePerformance.report(t,c)).filter(r->r.samples()>=3)
                .mapToInt(r->Math.max(0,70-r.quality())/2).sum();
        // Use mean rather than penalizing plants merely for having more staff.
        long count=day<=0?0:t.people.values().stream().filter(c->WorkplaceHierarchy.onDuty(c,day)
                &&c.workplace==p.site()&&p.occupation().equals(c.occupation)
                &&WorkplacePerformance.report(t,c).samples()>=3).count();
        return Math.min(75,equipment+(count==0?0:(int)(team/count)));
    }
    static int inspect(TownLife.Town t,FactoryNetworkLife.Plant p,int day){
        int credit=get(t,p,"credit")+p.units()*risk(t,p,day);
        int bad=Math.min(p.units(),credit/100),good=p.units()-bad;
        set(t,p,"credit",credit%100);
        set(t,p,"rejected",(int)Math.min(Integer.MAX_VALUE,(long)rejected(t,p)+bad));
        set(t,p,"accepted",(int)Math.min(Integer.MAX_VALUE,(long)accepted(t,p)+good));
        set(t,p,"reputation",Math.max(0,Math.min(100,reputation(t,p)+(bad==0?1:-8*bad))));
        WasteLife.rejected(t,p,bad);
        return good;
    }
    static int wholesalePrice(TownLife.Town t,FactoryNetworkLife.Plant p,int base){
        int reputation=reputation(t,p);
        return Math.max(1,base*(reputation<40?80:reputation<60?90:100)/100);
    }
}
