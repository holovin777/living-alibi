package com.holovin.npclife;

import java.util.*;

/** Daily purchase orders against existing shared industrial stock, settled on delivery.
 * One order per plant/input/day. No extra inventory, credit or fictitious cash. */
final class ProcurementLife {
    private ProcurementLife(){}
    private static String key(FactoryNetworkLife.Plant p,String item,String f){
        return "procurement:"+p.id()+":"+item+":"+f;
    }
    private static int get(TownLife.Town t,FactoryNetworkLife.Plant p,String item,String f){
        return t.industryStocks.getOrDefault(key(p,item,f),0);
    }
    private static void set(TownLife.Town t,FactoryNetworkLife.Plant p,String item,String f,int n){
        t.industryStocks.put(key(p,item,f),n);
    }
    static boolean authorize(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p){
        String a=FactoryNetworkLife.account(p);
        if(BusinessSolvencyLife.suspended(t,a))return false;
        var director=BusinessManagementLife.director(t,a);
        if(BusinessOwnershipLife.yours(t,a)&&director!=null)
            return WorkplaceHierarchy.onDuty(director,s.day)&&director.fatigue<85
                &&!RelationshipBoundaryLife.controlled(t,director)&&!NpcJusticeLife.confined(t,director,s)
                &&!TownLife.inPlayerConversation(t,director);
        return SupplyChainLife.attending(t,p.site(),p.occupation());
    }
    static void plan(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p){
        if(!authorize(t,s,p)||!FactoryMaintenance.operational(t,p)
            ||FactoryNetworkLife.stock(t,p.output())+p.units()>12)return;
        String a=FactoryNetworkLife.account(p);
        var director=BusinessManagementLife.director(t,a);
        var buyer=director!=null?director:t.people.values().stream()
            .filter(c->p.occupation().equals(c.occupation)&&WorkplaceHierarchy.onDuty(c,s.day))
            .max(Comparator.comparingInt((TownLife.Citizen c)->c.carefulness).thenComparing(c->c.id)).orElse(null);
        for(var entry:p.inputs().entrySet()){
            String item=entry.getKey();
            if(get(t,p,item,"day")==s.day)continue;
            boolean imported=FactoryNetworkLife.imported(item);
            // Imported material already paid on arrival remains in the common inventory.
            if(imported&&FactoryNetworkLife.stock(t,item)>=entry.getValue())continue;
            int units=imported?2:entry.getValue();
            String supplier=FactoryNetworkLife.supplier(item);
            int price=2*units;
            // A low-reputation domestic supplier accepts a modest negotiated bulk discount.
            if(!imported&&units>=2&&buyer!=null&&buyer.carefulness>=65
                &&FactoryNetworkLife.PLANTS.stream().anyMatch(x->FactoryNetworkLife.account(x).equals(supplier)
                    &&FactoryQuality.reputation(t,x)<60))price--;
            int required=price+(imported?FuelLife.PRICE:0);
            set(t,p,item,"day",s.day);set(t,p,item,"units",units);set(t,p,item,"price",price);
            set(t,p,item,"status",t.employerFunds.getOrDefault(a,0)>=required||TradeCreditLife.eligible(t,p,item,price)?1:3);
        }
    }
    static boolean ordered(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int day){
        return get(t,p,item,"day")==day&&get(t,p,item,"status")==1;
    }
    static int quote(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int units,int day){
        return day>0&&ordered(t,p,item,day)&&get(t,p,item,"units")==units?get(t,p,item,"price"):2*units;
    }
    static void settled(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int units,int amount,int day){
        if(day<=0)return;
        if(get(t,p,item,"day")==day&&get(t,p,item,"status")==2)
            throw new IllegalStateException("Purchase order already settled.");
        set(t,p,item,"day",day);set(t,p,item,"units",units);set(t,p,item,"price",amount);set(t,p,item,"status",2);
        set(t,p,item,"invoice-day",day);set(t,p,item,"invoice-units",units);set(t,p,item,"invoice-amount",amount);
    }
    static void deferred(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int day){
        if(!ordered(t,p,item,day))throw new IllegalStateException("Purchase order unavailable.");
        set(t,p,item,"status",5);
    }
    static void finish(TownLife.Town t,FactoryNetworkLife.Plant p,int day){
        for(String item:p.inputs().keySet())if(ordered(t,p,item,day))
            set(t,p,item,"status",4);
    }
    record Order(String item,String supplier,int day,int quantity,int amount,String status,
                 int invoiceDay,int invoiceQuantity,int invoiceAmount){}
    static List<Order> view(TownLife.Town t,String a){
        var p=FactoryNetworkLife.PLANTS.stream().filter(x->FactoryNetworkLife.account(x).equals(a)).findFirst().orElse(null);
        if(p==null)return List.of();
        return p.inputs().keySet().stream().sorted().filter(item->get(t,p,item,"day")>0).map(item->{
            String status=switch(get(t,p,item,"status")){
                case 1->"ORDERED";case 2->"PAID";case 3->"NO_FUNDS";case 4->"UNFULFILLED";case 5->"ON_CREDIT";default->"NONE";};
            return new Order(item,FactoryNetworkLife.supplier(item),get(t,p,item,"day"),get(t,p,item,"units"),
                get(t,p,item,"price"),status,get(t,p,item,"invoice-day"),get(t,p,item,"invoice-units"),get(t,p,item,"invoice-amount"));
        }).toList();
    }
}
