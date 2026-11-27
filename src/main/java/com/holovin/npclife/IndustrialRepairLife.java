package com.holovin.npclife;

import java.util.List;

/** Owner-operated emergency industrial contract. A stopped plant may also
 * be repaired normally by its own technicians; the outside order then expires. */
final class IndustrialRepairLife {
    private IndustrialRepairLife(){}
    static final int FEE=30, PARTS=2, PART_COST=5, TAX=2, RESTORE=60, RESERVE=16;
    static int get(TownLife.Town t,String f){return PlayerServiceBusiness.get(t,"industrial:"+f);}
    static void set(TownLife.Town t,String f,int n){PlayerServiceBusiness.set(t,"industrial:"+f,n);}
    static FactoryNetworkLife.Plant plant(TownLife.Town t){
        int n=get(t,"plant");return n>0&&n<=FactoryNetworkLife.PLANTS.size()?FactoryNetworkLife.PLANTS.get(n-1):null;
    }
    static boolean qualified(TownLife.Town t){
        return ApprenticeshipLife.qualified(t)&&ApprenticeshipLife.get(t,"sessions")>=3;
    }
    static boolean open(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p){
        return p!=null&&StaffingLife.enabled(t)&&PlayerServiceBusiness.operating(t,s.day)&&qualified(t)
            &&!FactoryMaintenance.operational(t,p)&&t.employerFunds.getOrDefault(FactoryNetworkLife.account(p),0)>=FEE+RESERVE;
    }
    static boolean present(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p,int duration){
        return open(t,s,p)&&t.player.place==p.site()&&t.player.travelRemaining==0&&t.player.fatigue<85
            &&s.minute>=480&&s.minute+duration<=1080;
    }
    static void clear(TownLife.Town t){
        for(String f:List.of("plant","day","phase","fault"))set(t,f,0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,"plant")>0&&(get(t,"day")!=s.day||s.minute>=1080||!open(t,s,plant(t))))clear(t);
    }
    static int parts(TownLife.Town t,FactoryNetworkLife.Plant p){
        return p.site()==TownLife.Place.FACTORY?SupplyChainLife.get(t,IndustryLife.FACTORY_PARTS):t.technicalMaterials.getOrDefault("spare-parts",0);
    }
    static String supplier(FactoryNetworkLife.Plant p){return p.site()==TownLife.Place.FACTORY?"machinist":"city-maintenance";}
    static String fault(int n){return switch(n){case 1->"drive";case 2->"sensor";default->"controller";};}
    static String reading(int n){return switch(n){case 1->"Simulator: drive module fault (D).";case 2->"Simulator: feedback sensor fault (S).";default->"Simulator: control module fault (C).";};}
    record Step(int minutes,String message){}
    static Step act(TownLife.Town t,WorldState s,String action){
        if(action.equals("cancel")){
            if(!PlayerServiceBusiness.yours(t)||plant(t)==null)throw new IllegalArgumentException("No active industrial order.");
            clear(t);return new Step(1,"Industrial order released.");
        }
        if(action.startsWith("inspect:")){
            var p=FactoryNetworkLife.PLANTS.stream().filter(x->x.id().equals(action.substring(8))).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown factory."));
            if(plant(t)!=null||!present(t,s,p,60))throw new IllegalArgumentException("Visit a stopped factory with an operating business and completed supervised practice.");
            set(t,"plant",FactoryNetworkLife.PLANTS.indexOf(p)+1);set(t,"day",s.day);set(t,"phase",1);
            set(t,"fault",1+Math.floorMod(p.id().hashCode()+s.day,3));
            return new Step(15,"Inspected stopped production equipment. "+reading(get(t,"fault")));
        }
        var p=plant(t);
        if(get(t,"day")!=s.day||!present(t,s,p,action.equals("test")?30:15))
            throw new IllegalArgumentException("The factory order is no longer available or you are not present.");
        if(action.startsWith("choose:")&&get(t,"phase")==1){
            if(!action.substring(7).equals(fault(get(t,"fault"))))return new Step(15,"Incorrect simulated diagnosis; review the fault code.");
            set(t,"phase",2);return new Step(15,"Diagnosis accepted. Ready for repair and functional test.");
        }
        if(action.equals("test")&&get(t,"phase")==2){
            if(parts(t,p)<PARTS)throw new IllegalArgumentException("Two industrial spare parts are required.");
            int cost=PARTS*PART_COST;
            t.employerFunds.merge(FactoryNetworkLife.account(p),-FEE,Math::addExact);
            t.employerFunds.merge(supplier(p),cost,Math::addExact);
            t.employerFunds.merge("city-maintenance",TAX,Math::addExact);
            if(p.site()==TownLife.Place.FACTORY)SupplyChainLife.set(t,IndustryLife.FACTORY_PARTS,parts(t,p)-PARTS);
            else t.technicalMaterials.merge("spare-parts",-PARTS,Math::addExact);
            int restored=Math.min(100,FactoryMaintenance.condition(t,p)+RESTORE);
            t.industryStocks.put(FactoryMaintenance.key(p,"condition"),restored);
            t.industryStocks.put(FactoryMaintenance.key(p,"last-repair"),s.day);
            PlayerServiceBusiness.add(t,"cash",FEE-cost-TAX);PlayerServiceBusiness.add(t,"revenue",FEE);
            PlayerServiceBusiness.add(t,"tax",TAX);PlayerServiceBusiness.add(t,"jobs",1);
            PlayerServiceBusiness.add(t,"manual-jobs",1);PlayerServiceBusiness.add(t,"industrial-jobs",1);
            t.player.engineeringSkill=Math.addExact(t.player.engineeringSkill,1);
            t.player.fatigue=Math.min(100,t.player.fatigue+8);clear(t);
            return new Step(30,"Production equipment restored to "+restored+"%. Factory paid EUR "+FEE+".");
        }
        throw new IllegalArgumentException("Complete the current industrial repair step first.");
    }
    static void lostBatch(TownLife.Town t,FactoryNetworkLife.Plant p){
        String key=FactoryMaintenance.key(p,"lost-batches");
        t.industryStocks.merge(key,1,Math::addExact);
    }
    record Offer(String id,String district,int condition,int fee,int parts,boolean canInspect){}
    record View(String active,int phase,String reading,boolean canAct,int completed,List<Offer> offers){}
    static View view(TownLife.Town t,WorldState s){
        if(!PlayerServiceBusiness.yours(t))return new View("",0,"",false,0,List.of());
        var p=plant(t);int phase=get(t,"phase");
        return new View(p==null?"":p.id(),phase,phase==1?reading(get(t,"fault")):"",present(t,s,p,phase==2?30:15),
            PlayerServiceBusiness.get(t,"industrial-jobs"),FactoryNetworkLife.PLANTS.stream().filter(x->open(t,s,x))
                .map(x->new Offer(x.id(),x.site().name(),FactoryMaintenance.condition(t,x),FEE,parts(t,x),p==null&&present(t,s,x,60))).toList());
    }
}
