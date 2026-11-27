package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** Finite sorted waste, paid collection, recycling and biogas. All material
 * conversion quantities are abstract game units, not real process yields. */
final class WasteLife {
    private WasteLife(){}
    static final String ACCOUNT="waste-utility", COLLECTOR="Waste collector", OPERATOR="Recycling operator";
    static final int BIN_CAP=24, SORT_CAP=24, WAGE=4;
    static final List<String> TYPES=List.of("organic","paper","metal","plastic","glass","residual");
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault("waste:"+f,0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put("waste:"+f,n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean active(TownLife.Town t){return get(t,"enabled")>0;}
    static boolean role(String job){return COLLECTOR.equals(job)||OPERATOR.equals(job);}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static void ensure(TownLife.Town t){
        if(active(t)||!StaffingLife.enabled(t))return;
        set(t,"enabled",1);
        SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,Math.min(40,t.employerFunds.getOrDefault("city-maintenance",0)));
    }
    static void discard(TownLife.Town t,String type,int amount){
        if(!active(t)||!TYPES.contains(type)||amount<=0)return;
        int n=Math.min(amount,BIN_CAP-get(t,"bin:"+type));
        add(t,"bin:"+type,n);
        add(t,"litter",amount-n);
    }
    static void rejected(TownLife.Town t,FactoryNetworkLife.Plant p,int n){
        String type=switch(p.output()){
            case "paper","book" -> "paper";
            case "board","phone","watch" -> "metal";
            case "planks","shelf","fabric","clothing" -> "residual";
            default -> "residual";
        };
        discard(t,type,n);
    }
    static TownLife.Citizen worker(TownLife.Town t,WorldState s,String role,TownLife.Place site){
        return t.people.values().stream().filter(c->role.equals(c.occupation)&&c.workplace==site
            &&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c))
            .min(Comparator.comparing(c->c.id)).orElse(null);
    }
    static void collect(TownLife.Town t,WorldState s){
        var c=worker(t,s,COLLECTOR,TownLife.Place.LOGISTICS_DEPOT);
        int remaining=12;
        int possible=0;
        for(String type:TYPES)possible+=Math.min(get(t,"bin:"+type),SORT_CAP-get(t,"sorted:"+type));
        if(c==null||possible==0||FuelLife.stock(t)<1||cash(t)<WAGE+FuelLife.PRICE)return;
        SupplyChainLife.pay(t,ACCOUNT,FuelLife.ACCOUNT,FuelLife.PRICE);
        t.industryStocks.merge(FuelLife.STOCK,-1,Math::addExact);
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);
        // Round-robin starting stream avoids permanently starving later materials.
        for(int i=0;i<TYPES.size()&&remaining>0;i++){
            String type=TYPES.get(Math.floorMod(s.day+i,TYPES.size()));
            int n=Math.min(remaining,Math.min(get(t,"bin:"+type),SORT_CAP-get(t,"sorted:"+type)));
            add(t,"bin:"+type,-n);add(t,"sorted:"+type,n);add(t,"collected",n);remaining-=n;
        }
    }
    static int condition(TownLife.Town t){return t.industryStocks.getOrDefault("waste:condition",100);}
    static void process(TownLife.Town t,WorldState s){
        var c=worker(t,s,OPERATOR,TownLife.Place.FACTORY);
        if(c==null||!GridDistributionLife.power(t,TownLife.Place.FACTORY))return;
        if(condition(t)<=55&&cash(t)>=5+WAGE+EnergyLife.PRICE&&t.technicalMaterials.getOrDefault("spare-parts",0)>0){
            SupplyChainLife.pay(t,ACCOUNT,"city-maintenance",5);
            t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
            set(t,"condition",Math.min(100,condition(t)+40));
        }
        if(condition(t)<=25)return;
        int organic=Math.min(2,Math.min(get(t,"sorted:organic")/2,12-get(t,"recovered:biogas")));
        int paper=Math.min(2,Math.min(get(t,"sorted:paper")/2,12-get(t,"recovered:paper")));
        int metal=Math.min(2,Math.min(get(t,"sorted:metal")/2,12-get(t,"recovered:steel")));
        int plastic=Math.min(2,Math.min(get(t,"sorted:plastic")/2,12-get(t,"recovered:plastic")));
        int glass=Math.min(2,Math.min(get(t,"sorted:glass")/2,12-get(t,"recovered:glass")));
        int residual=Math.min(2,get(t,"sorted:residual"));
        if(organic+paper+metal+plastic+glass+residual==0||cash(t)<WAGE+(EnergyLife.enabled(t)?EnergyLife.PRICE:0)||!EnergyLife.buy(t,ACCOUNT))return;
        t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);c.money=Math.addExact(c.money,WAGE);
        int[] counts={organic,paper,metal,plastic,glass};String[] products={"biogas","paper","steel","plastic","glass"};
        for(int i=0;i<counts.length;i++){
            add(t,"sorted:"+TYPES.get(i),-2*counts[i]);add(t,"recovered:"+products[i],counts[i]);
            add(t,"recycled",2*counts[i]);
        }
        add(t,"sorted:residual",-residual);add(t,"landfill",residual);
        set(t,"condition",Math.max(0,condition(t)-2));
    }
    static void sell(TownLife.Town t){
        if(GasNetworkLife.active(t)&&GasNetworkLife.condition(t)>25&&GasNetworkLife.stock(t)<GasNetworkLife.CAPACITY
            &&get(t,"recovered:biogas")>0&&SupplyChainLife.pay(t,GasNetworkLife.ACCOUNT,ACCOUNT,1)){
            add(t,"recovered:biogas",-1);GasNetworkLife.add(t,"stock",1);GasNetworkLife.add(t,"biogas",1);add(t,"biogas-sold",1);
        }
        // Pay existing suppliers' accounts for finite recycled input stock.
        for(String item:List.of("paper","steel")){
            if(get(t,"recovered:"+item)<=0||FactoryNetworkLife.stock(t,item)>=12)continue;
            String buyer=FactoryNetworkLife.supplier(item);
            if(!SupplyChainLife.pay(t,buyer,ACCOUNT,1))continue;
            add(t,"recovered:"+item,-1);FactoryNetworkLife.change(t,item,1);add(t,"materials-sold",1);
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==710&&get(t,"day")<s.day){
            ensure(t);set(t,"day",s.day);
            int households=(int)Math.max(1,DemographyLife.living(t)/40);
            // A small aggregate stream represents routine packaging and household refuse.
            discard(t,"organic",households);discard(t,"paper",Math.max(1,households/2));
            discard(t,"plastic",1);discard(t,"glass",1);discard(t,"metal",1);discard(t,"residual",1);
            // Municipal collection contract is a conserved payment, capped by treasury cash.
            int subsidy=Math.min(8,t.employerFunds.getOrDefault("city-maintenance",0));
            SupplyChainLife.pay(t,"city-maintenance",ACCOUNT,subsidy);
            collect(t,s);process(t,s);sell(t);
        }
    }
    record Stream(String type,int bin,int sorted){}
    record Material(String item,int units){}
    record View(boolean active,int condition,int collected,int recycled,int landfill,int litter,int biogasSold,int materialsSold,int funds,List<Stream> streams,List<Material> materials){}
    static View view(TownLife.Town t){
        return new View(active(t),condition(t),get(t,"collected"),get(t,"recycled"),get(t,"landfill"),get(t,"litter"),get(t,"biogas-sold"),get(t,"materials-sold"),cash(t),
            TYPES.stream().map(x->new Stream(x,get(t,"bin:"+x),get(t,"sorted:"+x))).toList(),
            List.of("biogas","paper","steel","plastic","glass").stream().map(x->new Material(x,get(t,"recovered:"+x))).toList());
    }
}
