package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Unique personal goods with consensual custody transfers, using the existing property journal. */
final class PersonalGoodsLife {
    private PersonalGoodsLife(){}
    static final String MERCHANT="merchant:personal-goods";
    record Product(String id,String label,PropertyLife.Type type,int price){}
    static final List<Product> PRODUCTS=List.of(new Product("book","Book",PropertyLife.Type.BOOK,8),
            new Product("clothing","Clothing",PropertyLife.Type.CLOTHING,15),
            new Product("watch","Watch",PropertyLife.Type.WATCH,25),
            new Product("phone","Mobile phone",PropertyLife.Type.PHONE,45));
    static String visitor(TownLife.Town t){return "visitor:"+t.player.incarnation;}
    static boolean persistedIdentity(String id){return id!=null&&(id.equals(MERCHANT)||id.matches("visitor:[0-9]+"));}
    static String stockKey(Product p){return "personal-goods:stock:"+p.id();}
    static int stock(TownLife.Town t,Product p){return t.industryStocks.getOrDefault(stockKey(p),4);}
    static Product product(String id){return PRODUCTS.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown personal good."));}
    static Product product(PropertyLife.Item item){return PRODUCTS.stream().filter(p->p.type()==item.type).findFirst().orElseThrow(()->new IllegalArgumentException("This item is not tradeable."));}
    static void ready(TownLife.Town t){TransportLife.ready(t.player);JusticeLife.requireFree(t);if(RebirthLife.active(t))throw new IllegalArgumentException("Personal goods are currently available to adult visitors.");}
    static void market(TownLife.Town t,WorldState s){ready(t);if(t.player.place!=MARKET||!RetailLife.open(MARKET,s.minute)||IntercomLife.insideVisit(t,s.day,s.minute))throw new IllegalArgumentException("Visit the market during opening hours.");}
    static void journal(TownLife.Town t,WorldState s,PropertyLife.Action action,PropertyLife.Item item,String from,String to,String detail){
        t.pendingProperty.add(new PropertyLife.Change(s.day,s.minute,action,item.id,from,to,detail));
    }
    static String buy(TownLife.Town t,WorldState s,String id){
        market(t,s);var p=product(id);int stock=stock(t,p);
        if(stock<1||t.player.money<p.price())throw new IllegalArgumentException("Insufficient money or stock.");
        if(t.items.values().stream().filter(i->visitor(t).equals(i.ownerId)||visitor(t).equals(i.holderId)).count()>=12)throw new IllegalArgumentException("Personal inventory is full.");
        var item=t.items.values().stream().filter(i->MERCHANT.equals(i.ownerId)&&i.type==p.type()).findFirst().orElse(null);
        if(item==null){
            int seq=Math.addExact(t.industryStocks.getOrDefault("personal-goods:sequence",0),1);
            t.industryStocks.put("personal-goods:sequence",seq);
            item=new PropertyLife.Item("personal-good-"+seq,p.type(),visitor(t),visitor(t),null,false,p.price(),s.day);
            t.items.put(item.id,item);
        }else{item.ownerId=visitor(t);item.holderId=visitor(t);item.storedSpace=null;}
        t.player.money-=p.price();t.employerFunds.merge("shop-assistant",p.price(),Math::addExact);
        t.industryStocks.put(stockKey(p),stock-1);
        journal(t,s,PropertyLife.Action.PURCHASED,item,MERCHANT,visitor(t),"Personal good purchased");
        return "Personal item purchased.";
    }
    static PropertyLife.Item held(TownLife.Town t,String id){
        var item=t.items.get(id);
        if(item==null||item.stolen||!visitor(t).equals(item.ownerId)||!visitor(t).equals(item.holderId))
            throw new IllegalArgumentException("You must own and carry this item.");
        return item;
    }
    static String sell(TownLife.Town t,WorldState s,String id){
        market(t,s);var item=held(t,id);var p=product(item);int price=p.price()/2;
        if(t.employerFunds.getOrDefault("shop-assistant",0)<price)throw new IllegalArgumentException("The merchant cannot afford this item.");
        t.employerFunds.merge("shop-assistant",-price,Math::addExact);t.player.money=Math.addExact(t.player.money,price);
        item.ownerId=MERCHANT;item.holderId=null;item.storedSpace="merchant-stock";
        t.industryStocks.put(stockKey(p),Math.addExact(stock(t,p),1));
        journal(t,s,PropertyLife.Action.SOLD,item,visitor(t),MERCHANT,"Personal good resold");return "Personal item sold.";
    }
    static TownLife.Citizen contact(TownLife.Town t,WorldState s,String id){
        ready(t);var c=PlayerLife.nearby(t,id,s.day,s.minute);var contact=t.player.contacts.get(id);
        if(c.age<18||!PlayerLife.knowsResident(t,id)||contact==null||contact.knownName==null||contact.trust<70)
            throw new IllegalArgumentException("This resident has not agreed to exchange belongings.");
        return c;
    }
    static String exchange(TownLife.Town t,WorldState s,String action,String id,String resident){
        var c=contact(t,s,resident);
        if("return".equals(action)){
            var item=t.items.get(id);
            if(item==null||item.stolen||!visitor(t).equals(item.ownerId)||!c.id.equals(item.holderId))
                throw new IllegalArgumentException("No loan to return.");
            item.holderId=visitor(t);item.storedSpace=null;
            journal(t,s,PropertyLife.Action.RETURNED,item,c.id,visitor(t),"Borrowed item returned");return "Borrowed item returned.";
        }
        var item=held(t,id);product(item);
        var relation=t.player.contacts.get(c.id);
        if("lend".equals(action)&&relation.trust<85)throw new IllegalArgumentException("The resident declines the loan.");
        if(!"gift".equals(action)&&!"lend".equals(action))throw new IllegalArgumentException("Unknown exchange.");
        item.holderId=c.id;item.storedSpace=null;
        if("gift".equals(action))item.ownerId=c.id;
        journal(t,s,"gift".equals(action)?PropertyLife.Action.GIFTED:PropertyLife.Action.LENT,item,visitor(t),c.id,
                "gift".equals(action)?"Ownership transferred with consent":"Temporary custody; legal owner unchanged");
        return "gift".equals(action)?"Gift accepted.":"Item lent to resident.";
    }
    record Offer(String id,String label,int price,int stock){}
    record Possession(String id,String label,boolean carried,boolean lent,String borrowerId,int resalePrice){}
    record Contact(String id,String name,boolean canBorrow){}
    record View(List<Offer> offers,List<Possession> items,List<Contact> contacts){}
    static View view(TownLife.Town t,WorldState s){
        if(RebirthLife.active(t)||!t.player.alive)return new View(List.of(),List.of(),List.of());
        var offers=t.player.place==MARKET?PRODUCTS.stream().map(p->new Offer(p.id(),p.label(),p.price(),stock(t,p))).toList():List.<Offer>of();
        var items=t.items.values().stream().filter(i->visitor(t).equals(i.ownerId)&&!i.stolen)
            .map(i->new Possession(i.id,product(i).label(),visitor(t).equals(i.holderId),i.holderId!=null&&!visitor(t).equals(i.holderId),
                    i.holderId!=null&&!visitor(t).equals(i.holderId)?i.holderId:null,product(i).price()/2)).toList();
        var contacts=PlayerLife.visible(t).stream().filter(c->c.age>=18&&PlayerLife.knowsResident(t,c.id)&&t.player.contacts.containsKey(c.id))
            .filter(c->t.player.contacts.get(c.id).knownName!=null&&t.player.contacts.get(c.id).trust>=70)
            .map(c->new Contact(c.id,c.name,t.player.contacts.get(c.id).trust>=85)).toList();
        return new View(offers,items,contacts);
    }
}
