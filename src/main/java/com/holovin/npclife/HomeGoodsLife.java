package com.holovin.npclife;
import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** Installed household goods, delivered to the visitor's owned home. No duplicate purchases. */
final class HomeGoodsLife {
    private HomeGoodsLife(){}
    record Product(String id,String label,int price,TownLife.Place shop){}
    static final List<Product> PRODUCTS=List.of(
        new Product("television","Television",65,PORTICI),
        new Product("fridge","Compact refrigerator",80,PORTICI),
        new Product("washer","Washing machine",95,PORTICI),
        new Product("boiler","Heating boiler",110,WORKSHOP),
        new Product("shelf","Wall bookshelf",25,WORKSHOP));
    static String key(TownLife.Town t,String field){return "home-goods:"+t.player.incarnation+":"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    static void set(TownLife.Town t,String field,int value){t.industryStocks.put(key(t,field),value);}
    static Product product(String id){return PRODUCTS.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown household item."));}
    static boolean open(int minute){return minute>=480&&minute<1080;}
    static boolean installed(TownLife.Town t,String id){return t.player.ownsHouse&&get(t,id)==1;}
    static String buy(TownLife.Town t,WorldState s,String id){
        PersonalGoodsLife.ready(t);var p=product(id);
        if(t.player.place!=p.shop()||!open(s.minute)||!t.player.ownsHouse)
            throw new IllegalArgumentException("Visit the open showroom and own a house before ordering delivery.");
        if(installed(t,id)||t.player.money<p.price())throw new IllegalArgumentException("Already installed or insufficient funds.");
        SupplyChainLife.take(t,id);
        t.player.money-=p.price();
        t.employerFunds.merge(p.shop()==WORKSHOP?"technician":"shop-assistant",p.price(),Math::addExact);
        set(t,id,1);HomeServiceLife.purchased(t,id,s.day);
        return "Household item delivered and installed.";
    }
    static int available(TownLife.Town t,String id){return SupplyChainLife.stock(t,id);}
    static boolean home(TownLife.Town t,WorldState s){
        return t.player.ownsHouse&&t.player.place==SOUTH_RESIDENCES
            ||!t.player.ownsHouse&&t.player.place==NORTH_RESIDENCES&&HousingLife.active(t.player,s.day,s.minute);
    }
    static boolean hasClothing(TownLife.Town t){
        return t.items.values().stream().anyMatch(i->i.type==PropertyLife.Type.CLOTHING&&!i.stolen
            &&PersonalGoodsLife.visitor(t).equals(i.ownerId)&&PersonalGoodsLife.visitor(t).equals(i.holderId));
    }
    static String wear(TownLife.Town t,WorldState s,boolean changed){
        PersonalGoodsLife.ready(t);
        if(!home(t,s)||IntercomLife.insideVisit(t,s.day,s.minute))throw new IllegalArgumentException("Change clothes at your own home.");
        if(changed&&!hasClothing(t))throw new IllegalArgumentException("Buy and keep an outfit first.");
        set(t,"outfit",changed?1:0);
        return "Changed outfit.";
    }
    record Offer(String id,String label,int price,boolean installed,boolean canBuy,int stock){}
    record View(List<Offer> offers,List<String> installed,boolean canChange,boolean hasClothing,boolean changedOutfit){}
    static View view(TownLife.Town t,WorldState s){
        if(RebirthLife.active(t)||!t.player.alive)return new View(List.of(),List.of(),false,false,false);
        boolean clothing=hasClothing(t);
        return new View(PRODUCTS.stream().filter(p->p.shop()==t.player.place)
            .map(p->new Offer(p.id(),p.label(),p.price(),installed(t,p.id()),t.player.ownsHouse&&open(s.minute)
                &&!installed(t,p.id())&&t.player.money>=p.price()&&t.player.travelRemaining==0&&available(t,p.id())>0,available(t,p.id()))).toList(),
            PRODUCTS.stream().filter(p->installed(t,p.id())).map(Product::id).toList(),
            home(t,s)&&!IntercomLife.insideVisit(t,s.day,s.minute),clothing,clothing&&get(t,"outfit")==1);
    }
}
