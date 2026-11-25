package com.holovin.npclife;

import java.util.List;

/** A small market stall with persistent stock and voluntary, present customers. */
final class MarketStallLife {
    private MarketStallLife() {}
    static final String LICENSE="stall:license", OPEN="stall:open", SALES="stall:sales", REVENUE="stall:revenue";
    static final int LICENSE_PRICE=20, CAPACITY=12, MAX_PRICE=6;
    static final List<String> ITEMS=List.of("bread","vegetables","eggs","apples","pears","plums","grapes");
    static String stock(String id){return "stall:stock:"+id;}
    static String price(String id){return "stall:price:"+id;}
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static void ensure(TownLife.Town t){
        for(String k:List.of(LICENSE,OPEN,SALES,REVENUE))t.industryStocks.putIfAbsent(k,0);
        for(String id:ITEMS){t.industryStocks.putIfAbsent(stock(id),0);t.industryStocks.putIfAbsent(price(id),3);}
        for(var c:t.people.values())if(c.generationIndex>0)
            for(String field:MEMORY_FIELDS)t.industryStocks.putIfAbsent(memory(c.id,field),0);
    }
    static void clear(TownLife.Town t){
        for(String k:List.of(LICENSE,OPEN,SALES,REVENUE))set(t,k,0);
        for(String id:ITEMS){set(t,stock(id),0);set(t,price(id),3);}
        t.industryStocks.replaceAll((k,v)->k.startsWith("stall:customer:")?0:v);
    }
    private static String memory(String id,String field){return "stall:customer:"+id+":"+field;}
    private static final List<String> MEMORY_FIELDS=List.of("purchases","days","last-day","referred");
    private static int remembered(TownLife.Town t,TownLife.Citizen c,String field){
        return get(t,memory(c.id,field));
    }
    static boolean regular(TownLife.Town t,TownLife.Citizen c){return remembered(t,c,"days")>=2;}
    private static void rememberPurchase(TownLife.Town t,TownLife.Citizen c,int day){
        String count=memory(c.id,"purchases"),last=memory(c.id,"last-day"),days=memory(c.id,"days");
        set(t,count,Math.min(1000000,get(t,count)+1));
        if(day>get(t,last)){
            set(t,last,day);set(t,days,Math.min(1000000,get(t,days)+1));
        }
    }
    /** Only direct customers can recommend the stall to a trusted, co-present acquaintance. */
    static boolean recommend(TownLife.Town t,TownLife.Citizen source,TownLife.Citizen listener){
        if(get(t,LICENSE)==0||source==null||listener==null||source==listener||source.generationIndex<=0||
                listener.generationIndex<=0||!source.alive||!listener.alive||
                source.travelRemaining>0||listener.travelRemaining>0||
                source.place!=listener.place||!source.currentSpace().equals(listener.currentSpace())||
                !regular(t,source)||remembered(t,listener,"purchases")>0||
                remembered(t,listener,"referred")>0)return false;
        var relation=t.peerRelationships.get(SocialNetwork.key(source.id,listener.id));
        if(relation==null||relation.trust<58||relation.tension>=35)return false;
        set(t,memory(listener.id,"referred"),1);return true;
    }
    static int total(TownLife.Town t){return ITEMS.stream().mapToInt(id->get(t,stock(id))).sum();}
    static boolean active(TownLife.Town t,int minute){
        var p=t.player;
        return get(t,LICENSE)>0&&get(t,OPEN)>0&&p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.MARKET
                &&minute>=480&&minute<1200;
    }
    static String offer(TownLife.Town t,TownLife.Citizen c,int minute){
        if(!active(t,minute)||!c.alive||c.food>=2)return null;
        String chosen=null;
        int acceptable=regular(t,c)||(remembered(t,c,"referred")>0&&c.hunger>=65)?4:3;
        for(String id:ITEMS){
            int amount=get(t,price(id));
            // The normal market offers three food units for 9 EUR.
            if(get(t,stock(id))>0&&amount>=1&&amount<=acceptable&&amount<=c.money
                    &&(chosen==null||amount<get(t,price(chosen))))chosen=id;
        }
        return chosen;
    }
    static String purchase(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(c.generationIndex<=0||!c.presentAt(TownLife.Place.MARKET))return null;
        String id=offer(t,c,minute);
        if(id==null)return null;
        int amount=get(t,price(id));
        int wallet=Math.addExact(t.player.money,amount);
        int sales=Math.addExact(get(t,SALES),1),revenue=Math.addExact(get(t,REVENUE),amount);
        c.money-=amount;c.food++;t.player.money=wallet;
        set(t,stock(id),get(t,stock(id))-1);set(t,SALES,sales);set(t,REVENUE,revenue);
        rememberPurchase(t,c,day);
        return c.name+" bought "+id+" from the visitor's stall for EUR "+amount+".";
    }
    static String act(TownLife.Town t,String action,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.MARKET||minute<480||minute>=1200)
            throw new IllegalArgumentException("Manage the stall at Market 08:00–20:00.");
        if(action.equals("stall-register")){
            if(get(t,LICENSE)>0||p.money<LICENSE_PRICE)throw new IllegalArgumentException("Need 20 EUR and no existing stall.");
            int city=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),LICENSE_PRICE);
            p.money-=LICENSE_PRICE;t.employerFunds.put("city-maintenance",city);set(t,LICENSE,1);
            return "Registered a private market stall.";
        }
        if(get(t,LICENSE)==0)throw new IllegalArgumentException("Register the stall first.");
        if(action.equals("stall-open")||action.equals("stall-close")){
            boolean open=action.equals("stall-open");
            set(t,OPEN,open?1:0);return open?"Opened the stall. Stay at Market to serve customers.":"Closed the stall.";
        }
        for(String id:ITEMS){
            if(action.equals("stall-stock-"+id)){
                if(total(t)>=CAPACITY)throw new IllegalArgumentException("Stall shelves are full.");
                FoodLife.change(p,id,false,-1);set(t,stock(id),get(t,stock(id))+1);
                return "Placed one "+id+" on the stall shelf.";
            }
            if(action.equals("stall-take-"+id)){
                if(get(t,stock(id))<1||FoodLife.total(p,false)>=12)
                    throw new IllegalArgumentException("Need stock and space in your bag.");
                FoodLife.change(p,id,false,1);set(t,stock(id),get(t,stock(id))-1);
                return "Returned one "+id+" to your bag.";
            }
            if(action.equals("stall-price-up-"+id)||action.equals("stall-price-down-"+id)){
                int next=get(t,price(id))+(action.startsWith("stall-price-up-")?1:-1);
                if(next<1||next>MAX_PRICE)throw new IllegalArgumentException("Choose a price of 1–6 EUR.");
                set(t,price(id),next);return "Set "+id+" to EUR "+next+".";
            }
        }
        throw new IllegalArgumentException("Unknown stall action.");
    }
    record Item(String id,int stock,int price) {}
    record Report(boolean registered,boolean open,boolean active,int licensePrice,int capacity,int stocked,int sales,int revenue,List<Item> items,int customers,int regularCustomers,int recommendedCustomers) {}
    static Report report(TownLife.Town t,int minute){
        return new Report(get(t,LICENSE)>0,get(t,OPEN)>0,active(t,minute),LICENSE_PRICE,CAPACITY,total(t),get(t,SALES),get(t,REVENUE),
                ITEMS.stream().map(id->new Item(id,get(t,stock(id)),get(t,price(id)))).toList(),
                (int)t.people.values().stream().filter(c->c.alive&&remembered(t,c,"purchases")>0).count(),
                (int)t.people.values().stream().filter(c->c.alive&&regular(t,c)).count(),
                (int)t.people.values().stream().filter(c->c.alive&&remembered(t,c,"referred")>0).count());
    }
}
