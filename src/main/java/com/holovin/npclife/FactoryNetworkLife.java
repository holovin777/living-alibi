package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Independent industrial firms sharing physical inventory and paid supply routes. */
final class FactoryNetworkLife {
    private FactoryNetworkLife(){}
    record Plant(String id,String occupation,TownLife.Place site,String output,int units,Map<String,Integer> inputs){}
    static final List<Plant> PLANTS=List.of(
        new Plant("sawmill","Sawmill worker",FACTORY,"planks",2,Map.of("logs",2)),
        new Plant("paper-mill","Paper mill worker",FACTORY,"paper",2,Map.of("logs",1)),
        new Plant("textile-mill","Textile worker",FACTORY,"fabric",2,Map.of("cotton",1)),
        new Plant("printing-house","Printer",FACTORY,"book",2,Map.of("paper",1)),
        new Plant("garment-factory","Garment worker",FACTORY,"clothing",2,Map.of("fabric",1)),
        new Plant("furniture-factory","Carpenter",WORKSHOP,"shelf",1,Map.of("planks",2)),
        new Plant("electronics-factory","Electronics assembler",FACTORY,"board",2,Map.of("components",1,"copper",1)),
        new Plant("phone-assembly","Device assembler",FACTORY,"phone",1,Map.of("board",1)),
        new Plant("watch-assembly","Watch assembler",FACTORY,"watch",1,Map.of("board",1,"steel",1)));
    static final String LUMBER="lumber-company", SUPPLIER="regional-materials";
    static String account(Plant p){return "factory-network:"+p.id();}
    static String key(String item){return switch(item){
        case "steel" -> IndustryLife.FACTORY_STEEL;
        case "copper" -> IndustryLife.FACTORY_COPPER;
        default -> "factory-network:stock:"+item;
    };}
    static int stock(TownLife.Town t,String item){return SupplyChainLife.get(t,key(item));}
    static void change(TownLife.Town t,String item,int amount){SupplyChainLife.set(t,key(item),Math.addExact(stock(t,item),amount));}
    static String supplier(String item){
        if(item.equals("logs"))return LUMBER;
        if(item.equals("steel")||item.equals("copper"))return "machinist";
        if(item.equals("cotton")||item.equals("components"))return SUPPLIER;
        return PLANTS.stream().filter(p->p.output().equals(item)).map(FactoryNetworkLife::account).findFirst().orElseThrow();
    }
    static boolean ensure(TownLife.Town t){
        if(!StaffingLife.enabled(t)||t.industryStocks.containsKey("factory-network:initialized"))return false;
        t.industryStocks.put("factory-network:initialized",1);
        t.industryStocks.putIfAbsent("factory-network:forest",80);
        t.employerFunds.putIfAbsent(LUMBER,160);
        t.employerFunds.putIfAbsent(SUPPLIER,0);
        for(var p:PLANTS)t.employerFunds.putIfAbsent(account(p),160);
        return true;
    }
    static String employer(String occupation){
        if("Forestry worker".equals(occupation))return LUMBER;
        return PLANTS.stream().filter(p->p.occupation().equals(occupation)).map(FactoryNetworkLife::account).findFirst().orElse(null);
    }
    static boolean payWorker(TownLife.Town t,TownLife.Citizen c){
        String account=employer(c.occupation);if(account==null)return false;
        int cash=t.employerFunds.getOrDefault(account,0);
        if(cash>=4){t.employerFunds.put(account,cash-4);c.money=Math.addExact(c.money,4);}
        return true;
    }
    static boolean produce(TownLife.Town t,Plant p){return produce(t,p,0);}
    static boolean produce(TownLife.Town t,Plant p,int day){
        if(!FactoryMaintenance.operational(t,p))return false;
        if(stock(t,p.output())+p.units()>12||p.inputs().entrySet().stream().anyMatch(e->stock(t,e.getKey())<e.getValue()))return false;
        int cost=p.inputs().entrySet().stream().filter(e->!supplier(e.getKey()).equals(SUPPLIER)).mapToInt(e->2*e.getValue()).sum();
        if(t.employerFunds.getOrDefault(account(p),0)<cost)return false;
        for(var e:p.inputs().entrySet()){
            if(!supplier(e.getKey()).equals(SUPPLIER))SupplyChainLife.pay(t,account(p),supplier(e.getKey()),2*e.getValue());
            change(t,e.getKey(),-e.getValue());
        }
        int accepted=FactoryQuality.inspect(t,p,day);
        change(t,p.output(),accepted);FactoryMaintenance.wear(t,p);return true;
    }
    static boolean procure(TownLife.Town t,String item,Plant p){
        if(!List.of("cotton","components").contains(item)||stock(t,item)>=8||FuelLife.stock(t)<1
            ||t.employerFunds.getOrDefault(account(p),0)<4+FuelLife.PRICE)return false;
        SupplyChainLife.pay(t,account(p),SUPPLIER,4);SupplyChainLife.pay(t,account(p),FuelLife.ACCOUNT,FuelLife.PRICE);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);change(t,item,2);return true;
    }
    static boolean harvest(TownLife.Town t){
        int trees=SupplyChainLife.get(t,"factory-network:forest");
        if(trees<2||stock(t,"logs")>=10||FuelLife.stock(t)<1||!SupplyChainLife.pay(t,LUMBER,FuelLife.ACCOUNT,FuelLife.PRICE))return false;
        SupplyChainLife.set(t,"factory-network:forest",trees-2);SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);
        change(t,"logs",2);return true;
    }
    static int retailStock(TownLife.Town t,String item){
        return item.equals("shelf")?SupplyChainLife.stock(t,item):PersonalGoodsLife.stock(t,PersonalGoodsLife.product(item));
    }
    static boolean deliver(TownLife.Town t,Plant p){
        String item=p.output();if(!List.of("book","clothing","phone","watch","shelf").contains(item)||stock(t,item)<1||retailStock(t,item)>=4||FuelLife.stock(t)<1)return false;
        String shop=item.equals("shelf")?"technician":"shop-assistant";
        int cost=FactoryQuality.wholesalePrice(t,p,item.equals("shelf")?12:PersonalGoodsLife.product(item).price()/2);
        if(t.employerFunds.getOrDefault(shop,0)<cost+FuelLife.PRICE)return false;
        SupplyChainLife.pay(t,shop,account(p),cost);SupplyChainLife.pay(t,shop,FuelLife.ACCOUNT,FuelLife.PRICE);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);change(t,item,-1);
        if(item.equals("shelf"))SupplyChainLife.set(t,SupplyChainLife.retail(item),retailStock(t,item)+1);
        else SupplyChainLife.set(t,PersonalGoodsLife.stockKey(PersonalGoodsLife.product(item)),retailStock(t,item)+1);
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        ensure(t);
        if(s.minute==360&&SupplyChainLife.once(t,"forest-growth",s.day)){
            String k="factory-network:forest";SupplyChainLife.set(t,k,Math.min(80,SupplyChainLife.get(t,k)+1));
        }
        boolean freight=SupplyChainLife.attending(t,LOGISTICS_DEPOT,"Warehouse worker")&&SupplyChainLife.attending(t,QUARRY,"Transport driver");
        if(s.minute==600&&SupplyChainLife.once(t,"materials-import",s.day)&&freight){
            for(var p:PLANTS)for(String item:p.inputs().keySet())if(List.of("cotton","components").contains(item))procure(t,item,p);
        }
        if(s.minute==660&&SupplyChainLife.once(t,"logging",s.day)&&freight&&SupplyChainLife.attending(t,WOODS,"Forestry worker"))harvest(t);
        if(s.minute==720){
            for(var p:PLANTS)FactoryMaintenance.service(t,s,p);
        }
        if(s.minute==780&&SupplyChainLife.once(t,"network-production",s.day)){
            for(var p:PLANTS)if(SupplyChainLife.attending(t,p.site(),p.occupation()))produce(t,p,s.day);
        }
        if(s.minute==900&&SupplyChainLife.once(t,"network-delivery",s.day)&&freight){
            for(var p:PLANTS)if(deliver(t,p))WorkplaceRewards.afterSale(t,s,p);
        }
    }
    record PlantView(String id,String district,String output,int stock,int employees,int funds,int condition,boolean operational,int accepted,int rejected,int reputation){}
    record View(boolean active,int forest,List<PlantView> plants){}
    static View view(TownLife.Town t){
        return new View(StaffingLife.enabled(t),SupplyChainLife.get(t,"factory-network:forest"),
            PLANTS.stream().map(p->new PlantView(p.id(),p.site().name(),p.output(),stock(t,p.output()),
                (int)t.people.values().stream().filter(c->c.alive&&c.employed&&p.occupation().equals(c.occupation)&&c.workplace==p.site()).count(),
                t.employerFunds.getOrDefault(account(p),0),FactoryMaintenance.condition(t,p),FactoryMaintenance.operational(t,p),FactoryQuality.accepted(t,p),FactoryQuality.rejected(t,p),FactoryQuality.reputation(t,p))).toList());
    }
}
