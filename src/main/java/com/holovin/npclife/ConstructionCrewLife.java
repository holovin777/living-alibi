package com.holovin.npclife;
import java.util.*;

/** Per-stage contracting: no payroll or fuel supplied by an imaginary crew. */
final class ConstructionCrewLife {
    private ConstructionCrewLife(){}
    static final String ARCHITECT="Architect", BUILDER="Construction worker", DRIVER="Construction truck driver", ACCOUNT="construction-contractors";
    static final int CONTRACT_FEE=4, DESIGN=4, WAGE=8, FREIGHT=4;
    static int labor(int stage){return CONTRACT_FEE+WAGE+FREIGHT+(stage==0?DESIGN:0);}
    static int cost(int stage){return labor(stage)+CommercialPropertyLife.STEEL+CommercialPropertyLife.MATERIAL+FuelLife.PRICE;}
    static int total(){return cost(0)+cost(1)+cost(2);}
    static boolean role(String occupation){return Set.of(ARCHITECT,BUILDER,DRIVER).contains(occupation);}
    static TownLife.Citizen worker(TownLife.Town t,WorldState s,String role,TownLife.Citizen owner){
        return t.people.values().stream().filter(c->c!=owner&&role.equals(c.occupation)&&WorkplaceHierarchy.onDuty(c,s.day)
            &&!RelationshipBoundaryLife.controlled(t,c)&&!NpcJusticeLife.confined(t,c,s)
            &&!TownLife.inPlayerConversation(t,c)&&c.fatigue<80)
            .min(Comparator.comparing(c->c.id)).orElse(null);
    }
    static boolean complete(TownLife.Town t,WorldState s,TownLife.Citizen owner,int stage){
        var builder=worker(t,s,BUILDER,owner);
        var driver=worker(t,s,DRIVER,owner);
        var architect=stage==0?worker(t,s,ARCHITECT,owner):null;
        if(builder==null||driver==null||stage==0&&architect==null||FuelLife.stock(t)<1
            ||owner.money<NpcEnterpriseLife.PERSONAL_RESERVE+cost(stage))return false;
        owner.money-=cost(stage);
        t.employerFunds.merge(ACCOUNT,CONTRACT_FEE,Math::addExact);
        builder.money=Math.addExact(builder.money,WAGE);driver.money=Math.addExact(driver.money,FREIGHT);
        builder.fatigue=Math.min(100,builder.fatigue+5);driver.fatigue=Math.min(100,driver.fatigue+3);
        if(architect!=null)architect.money=Math.addExact(architect.money,DESIGN);
        t.employerFunds.merge(FuelLife.ACCOUNT,FuelLife.PRICE,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-1,Integer::sum);
        t.employerFunds.merge("machinist",CommercialPropertyLife.STEEL,Math::addExact);
        t.employerFunds.merge(FactoryNetworkLife.supplier("building-material"),CommercialPropertyLife.MATERIAL,Math::addExact);
        t.industryStocks.merge(IndustryLife.FACTORY_STEEL,-1,Integer::sum);
        FactoryNetworkLife.change(t,"building-material",-1);
        return true;
    }
}
