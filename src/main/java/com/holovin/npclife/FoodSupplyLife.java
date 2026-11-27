package com.holovin.npclife;
import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** Wholesale groceries and shared counters in large worlds. All units are gameplay abstractions. */
final class FoodSupplyLife {
    private FoodSupplyLife(){}
    static final String SUPPLIER="regional-food-supplier";
    static final String SHOP="shop-assistant";
    static final List<String> IMPORTS=List.of("flour","water","milk","eggs","pasta");
    static int stock(TownLife.Town t,String id){return t.retailStock.getOrDefault(id,0);}
    static int capacity(TownLife.Town t,String id){
        int base=RetailLife.product(id).capacity();
        if(!StaffingLife.enabled(t))return base;
        return switch(id){
            case "bread","water","flour" -> Math.max(base,3*(int)DemographyLife.living(t));
            default -> base;
        };
    }
    static int importGoods(TownLife.Town t,String id,int requested){
        if(!IMPORTS.contains(id)||requested<=0)return 0;
        int n=Math.min(requested,Math.min(capacity(t,id)-stock(t,id),t.employerFunds.getOrDefault(SHOP,0)));
        if(n<=0)return 0;
        // A paid import crosses the simulated town boundary; regional production is not simulated.
        if(!SupplyChainLife.pay(t,SHOP,SUPPLIER,n))return 0;
        t.retailStock.merge(id,n,Math::addExact);return n;
    }
    static int bake(TownLife.Town t,int limit){
        int n=Math.min(limit,Math.min(capacity(t,"bread")-stock(t,"bread"),stock(t,"flour")*2));
        n=Math.min(n,stock(t,"water")*8);
        if(n<=0)return 0;
        t.retailStock.merge("flour",-(n+1)/2,Math::addExact);
        t.retailStock.merge("water",-(n+7)/8,Math::addExact);
        t.retailStock.merge("bread",n,Math::addExact);return n;
    }
    static int prepare(TownLife.Town t,String product,int desired){
        if(desired<=0)return 0;
        boolean pizza=product.equals("pizza");
        if(!pizza&&!product.equals("gelato"))throw new IllegalArgumentException("Unknown prepared food.");
        String ingredient=pizza?"flour":"milk";
        int n=Math.min(desired,Math.min(stock(t,ingredient),capacity(t,product)-stock(t,product)));
        if(pizza)n=Math.min(n,stock(t,"vegetables"));
        int cost=pizza?3:1;
        n=Math.min(n,t.employerFunds.getOrDefault(RetailLife.salesAccount(product),0)/cost);
        if(n<=0||!SupplyChainLife.pay(t,RetailLife.salesAccount(product),SHOP,n*cost))return 0;
        t.retailStock.merge(ingredient,-n,Math::addExact);
        if(pizza)t.retailStock.merge("vegetables",-n,Math::addExact);
        t.retailStock.merge(product,n,Math::addExact);return n;
    }
    static int buy(TownLife.Town t,TownLife.Citizen c,String id,int requested){
        if(!id.equals("bread")&&!id.equals("water"))throw new IllegalArgumentException("Unknown household grocery.");
        int price=RetailLife.product(id).price();
        int n=Math.min(requested,Math.min(stock(t,id),c.money/price));
        if(n<=0)return 0;
        c.money-=price*n;t.employerFunds.merge(SHOP,price*n,Math::addExact);
        t.retailStock.merge(id,-n,Math::addExact);
        if(id.equals("bread"))c.food+=n;else c.water+=n;
        return n;
    }
    static boolean once(TownLife.Town t,String stage,int day,int minute){
        String key="food-supply:"+stage;int now=(day-1)*1440+minute+1;
        if(t.industryStocks.getOrDefault(key,0)>=now)return false;
        t.industryStocks.put(key,now);return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if((s.minute==480||s.minute==720||s.minute==960)&&once(t,"import",s.day,s.minute)
            &&SupplyChainLife.attending(t,LOGISTICS_DEPOT,"Warehouse worker")){
            int residents=(int)DemographyLife.living(t);
            // Spread each wholesale budget between drinking water and baking flour.
            int funds=t.employerFunds.getOrDefault(SHOP,0);
            importGoods(t,"water",Math.min(residents, funds/3));
            importGoods(t,"flour",Math.min(residents, funds/3));
            for(String id:List.of("milk","eggs","pasta"))importGoods(t,id,4);
        }
        if(s.minute%60==0&&s.minute>=480&&s.minute<1080&&once(t,"bake",s.day,s.minute)){
            long workers=t.people.values().stream().filter(c->c.alive&&c.employed&&"Baker".equals(c.occupation)
                &&c.presentAt(BAKERY)&&"WORKING".equals(c.activity)).count();
            bake(t,(int)workers*12);
        }
    }
}
