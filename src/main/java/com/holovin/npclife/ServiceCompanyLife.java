package com.holovin.npclife;

/** Independent NPC service account and physical spare-part inventory.
 * No free capital: the company grows from completed paid orders. */
final class ServiceCompanyLife {
    private ServiceCompanyLife(){}
    static final String ACCOUNT="appliance-service-company";
    static final int LABOR=8, CAPACITY=4;
    static String key(String field){return "service-company:"+field;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(f),0);}
    static void set(TownLife.Town t,String f,int value){t.industryStocks.put(key(f),value);}
    static void count(TownLife.Town t,String f,int amount){set(t,f,Math.addExact(get(t,f),amount));}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static boolean available(TownLife.Town t){
        return get(t,"parts")>0||t.technicalMaterials.getOrDefault("spare-parts",0)>0;
    }
    static boolean purchasePart(TownLife.Town t){
        if(get(t,"parts")>=CAPACITY||cash(t)<HomeServiceLife.PART_PRICE
                ||t.technicalMaterials.getOrDefault("spare-parts",0)<1)return false;
        if(!SupplyChainLife.pay(t,ACCOUNT,"city-maintenance",HomeServiceLife.PART_PRICE))return false;
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        count(t,"parts",1);count(t,"parts-expense",HomeServiceLife.PART_PRICE);
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        ServiceFleetLife.tick(t,s);
        if(s.minute!=480||get(t,"restock-day")>=s.day)return;
        set(t,"restock-day",s.day);
        if(get(t,"parts")<2)purchasePart(t);
    }
    static void complete(TownLife.Town t,TownLife.Citizen mechanic){
        t.employerFunds.merge(ACCOUNT,HomeServiceLife.REPAIR_PRICE,Math::addExact);
        if(get(t,"parts")==0&&!purchasePart(t))throw new IllegalStateException("Repair part unavailable");
        count(t,"parts",-1);
        t.employerFunds.merge(ACCOUNT,-LABOR,Math::addExact);
        mechanic.money=Math.addExact(mechanic.money,LABOR);
        count(t,"revenue",HomeServiceLife.REPAIR_PRICE);count(t,"labor-expense",LABOR);
        count(t,"completed",1);
    }
    record View(int cash,int parts,int completed,int cancelled,int missed,int revenue,int laborExpense,int partsExpense,int queued,int active,ServiceFleetLife.View fleet){}
    static View view(TownLife.Town t){
        return new View(cash(t),get(t,"parts"),get(t,"completed"),get(t,"cancelled"),
            get(t,"missed"),get(t,"revenue"),get(t,"labor-expense"),get(t,"parts-expense"),ResidentServiceLife.queued(t),ResidentServiceLife.active(t),ServiceFleetLife.view(t));
    }
}
