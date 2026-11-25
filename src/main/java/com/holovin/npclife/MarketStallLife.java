package com.holovin.npclife;

import java.util.List;

/** A small market stall with persistent stock and voluntary, present customers. */
final class MarketStallLife {
    private MarketStallLife() {}
    static final String LICENSE="stall:license", OPEN="stall:open", SALES="stall:sales", REVENUE="stall:revenue";
    static final int LICENSE_PRICE=20, CAPACITY=12, MAX_PRICE=6;
    static final String WORKER="stall:worker", RESERVE="stall:reserve", PAID_DAY="stall:paid-day", PAID_HOUR="stall:paid-hour", WAGES="stall:wages";
    static final String OCCUPATION="Private stall seller";
    static final int WAGE=4;
    static final List<String> ITEMS=List.of("bread","vegetables","eggs","apples","pears","plums","grapes");
    static String stock(String id){return "stall:stock:"+id;}
    static String price(String id){return "stall:price:"+id;}
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static void ensure(TownLife.Town t){
        StallSupplyLife.ensure(t);
        for(String k:List.of(LICENSE,OPEN,SALES,REVENUE,WORKER,RESERVE,PAID_DAY,PAID_HOUR,WAGES))t.industryStocks.putIfAbsent(k,0);
        for(String id:ITEMS){t.industryStocks.putIfAbsent(stock(id),0);t.industryStocks.putIfAbsent(price(id),3);}
        for(var c:t.people.values())if(c.generationIndex>0)
            for(String field:MEMORY_FIELDS)t.industryStocks.putIfAbsent(memory(c.id,field),0);
    }
    static void clear(TownLife.Town t){
        StallSupplyLife.clear(t);
        release(t);
        t.employerFunds.merge("city-maintenance",get(t,RESERVE),Math::addExact);
        for(String k:List.of(LICENSE,OPEN,SALES,REVENUE,WORKER,RESERVE,PAID_DAY,PAID_HOUR,WAGES))set(t,k,0);
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
    static TownLife.Citizen worker(TownLife.Town t){
        int index=get(t,WORKER);
        if(index<=0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==index&&c.alive&&c.employed&&OCCUPATION.equals(c.occupation)).findFirst().orElse(null);
    }
    static boolean isEmployee(TownLife.Town t,TownLife.Citizen c){
        return c.generationIndex>0&&c.generationIndex==get(t,WORKER)&&c.alive&&c.employed&&OCCUPATION.equals(c.occupation);
    }
    static void release(TownLife.Town t){
        var c=worker(t);
        if(c!=null){c.employed=false;c.unemploymentDays=0;}
        set(t,WORKER,0);set(t,PAID_DAY,0);set(t,PAID_HOUR,0);
    }
    static boolean scheduled(TownLife.Town t,int day,int minute){
        return get(t,LICENSE)>0&&get(t,OPEN)>0&&(day-1)%7<5&&minute>=660&&minute<840;
    }
    static boolean staffed(TownLife.Town t,int day,int minute){
        if(!scheduled(t,day,minute)||minute<720)return false;
        var c=worker(t);
        return c!=null&&c.presentAt(TownLife.Place.MARKET)&&!EmploymentLife.sick(c,day)
                &&get(t,PAID_DAY)==day&&get(t,PAID_HOUR)==minute/60;
    }
    static void pay(TownLife.Town t,TownLife.Citizen c,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations){
        int day=clock.day,minute=clock.minute,hour=minute/60;
        if(!isEmployee(t,c)||!scheduled(t,day,minute)||minute<720||!c.presentAt(TownLife.Place.MARKET)
                ||EmploymentLife.sick(c,day)||(get(t,PAID_DAY)==day&&get(t,PAID_HOUR)>=hour))return;
        if(get(t,RESERVE)<WAGE)return;
        int wallet=Math.addExact(c.money,WAGE),wages=Math.addExact(get(t,WAGES),WAGE);
        set(t,RESERVE,get(t,RESERVE)-WAGE);c.money=wallet;set(t,WAGES,wages);
        set(t,PAID_DAY,day);set(t,PAID_HOUR,hour);
        TownLife.publish(t,clock,c.id,c.place,c.name+" received EUR "+WAGE+" for a stall shift hour.",events,observations);
    }
    static List<TownLife.Citizen> candidates(TownLife.Town t,int day){
        if(!t.player.alive||t.player.place!=TownLife.Place.MARKET||t.player.travelRemaining>0)return List.of();
        return t.people.values().stream().filter(c->c.generationIndex>0&&!c.employed&&c.presentAt(TownLife.Place.MARKET)&&!EmploymentLife.sick(c,day)).toList();
    }
    static boolean active(TownLife.Town t,int day,int minute){
        var p=t.player;
        return get(t,LICENSE)>0&&get(t,OPEN)>0&&p.alive&&minute>=480&&minute<1200
                &&((p.travelRemaining==0&&p.place==TownLife.Place.MARKET)||staffed(t,day,minute));
    }
    static String offer(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!active(t,day,minute)||!c.alive||c.food>=2||isEmployee(t,c))return null;
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
        String id=offer(t,c,day,minute);
        if(id==null)return null;
        int amount=get(t,price(id));
        int wallet=Math.addExact(t.player.money,amount);
        int sales=Math.addExact(get(t,SALES),1),revenue=Math.addExact(get(t,REVENUE),amount);
        c.money-=amount;c.food++;t.player.money=wallet;
        set(t,stock(id),get(t,stock(id))-1);set(t,SALES,sales);set(t,REVENUE,revenue);
        rememberPurchase(t,c,day);
        return c.name+" bought "+id+" from the visitor's stall for EUR "+amount+".";
    }
    static String act(TownLife.Town t,String action,int day,int minute){
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
        if(action.startsWith("stall-supply-"))return StallSupplyLife.act(t,action);
        if(action.equals("stall-fund")){
            if(p.money<10)throw new IllegalArgumentException("Need 10 EUR to fund the payroll reserve.");
            int reserve=Math.addExact(get(t,RESERVE),10);p.money-=10;set(t,RESERVE,reserve);
            return "Transferred 10 EUR into payroll reserve.";
        }
        if(action.equals("stall-withdraw")){
            if(get(t,RESERVE)<10)throw new IllegalArgumentException("Need 10 EUR in payroll reserve.");
            int money=Math.addExact(p.money,10);set(t,RESERVE,get(t,RESERVE)-10);p.money=money;
            return "Withdrew 10 EUR from payroll reserve.";
        }
        if(action.startsWith("stall-hire-")){
            if(worker(t)!=null||get(t,RESERVE)<2*WAGE)throw new IllegalArgumentException("Need a vacant seller job and 8 EUR in payroll reserve.");
            var c=candidates(t,day).stream().filter(x->x.id.equals(action.substring("stall-hire-".length()))).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Choose an available job seeker at Market."));
            c.employed=true;c.occupation=OCCUPATION;c.workplace=TownLife.Place.MARKET;
            c.unemploymentDays=0;c.lastHiredDay=day;c.jobChanges++;
            set(t,WORKER,c.generationIndex);set(t,PAID_DAY,0);set(t,PAID_HOUR,0);
            return c.name+" signed a seller contract.";
        }
        if(action.equals("stall-dismiss")){
            if(worker(t)==null)throw new IllegalArgumentException("No employed seller.");
            release(t);return "Ended the seller contract.";
        }
        if(action.equals("stall-open")||action.equals("stall-close")){
            boolean open=action.equals("stall-open");
            set(t,OPEN,open?1:0);return open?"Opened the stall. Customers need the owner or a paid seller.":"Closed the stall.";
        }
        for(String id:ITEMS){
            if(action.equals("stall-stock-"+id)){
                if(total(t)+StallSupplyLife.reserved(t)>=CAPACITY)throw new IllegalArgumentException("Stall shelves are full.");
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
    record Candidate(String id,String name) {}
    record Report(boolean registered,boolean open,boolean active,int licensePrice,int capacity,int stocked,int sales,int revenue,List<Item> items,int customers,int regularCustomers,int recommendedCustomers,String sellerName,boolean staffed,int reserve,int wages,int hourlyWage,List<Candidate> candidates,StallSupplyLife.Report supply) {}
    static Report report(TownLife.Town t,int day,int minute){
        return new Report(get(t,LICENSE)>0,get(t,OPEN)>0,active(t,day,minute),LICENSE_PRICE,CAPACITY,total(t),get(t,SALES),get(t,REVENUE),
                ITEMS.stream().map(id->new Item(id,get(t,stock(id)),get(t,price(id)))).toList(),
                (int)t.people.values().stream().filter(c->c.alive&&remembered(t,c,"purchases")>0).count(),
                (int)t.people.values().stream().filter(c->c.alive&&regular(t,c)).count(),
                (int)t.people.values().stream().filter(c->c.alive&&remembered(t,c,"referred")>0).count(),worker(t)==null?null:worker(t).name,staffed(t,day,minute),get(t,RESERVE),get(t,WAGES),WAGE,candidates(t,day).stream().map(c->new Candidate(c.id,c.name)).toList(),StallSupplyLife.report(t));
    }
}
