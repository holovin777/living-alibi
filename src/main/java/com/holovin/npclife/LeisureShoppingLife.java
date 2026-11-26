package com.holovin.npclife;

import java.util.*;

/** Discretionary purchases use the same stock and shop account as the player. */
final class LeisureShoppingLife {
    private LeisureShoppingLife(){}
    private static final List<String> GIFTS=List.of("toy-ball","teddy-bear","storybook","gift-box","scarf");
    private static String key(TownLife.Citizen c,String f){return "leisure-shopping:"+c.id+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    private static String holding(TownLife.Citizen c,String id){return "gift:resident:"+c.id+":"+id;}
    private static boolean autonomous(TownLife.Town t,TownLife.Citizen c){
        return c.alive&&c.generationIndex>0&&c.age>=18&&!RebirthLife.linkedAdult(t,c)&&!RebirthLife.linkedChild(t,c);
    }
    private static int reserve(TownLife.Citizen c){return 80+c.carefulness/2;}
    private static boolean ready(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        return autonomous(t,c)&&!EmploymentLife.sick(c,clock.day)&&c.food>=2&&c.water>=2&&
            c.hunger<65&&c.thirst<65&&c.fatigue<75&&c.health>=60&&c.unpaidBills==0;
    }
    private static int birthday(TownLife.Town t,TownLife.Citizen c,int day){
        int origin=Math.max(1,DemographyLife.get(t,c,"born"));
        int elapsed=day-origin;
        return origin+365*Math.max(1,(elapsed+364)/365);
    }
    private static boolean parent(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen child){
        return DemographyLife.get(t,child,"parent-a")==c.generationIndex||DemographyLife.get(t,child,"parent-b")==c.generationIndex;
    }
    private record Purchase(String item,WardrobeLife.Outfit outfit,TownLife.Citizen recipient,int due,int price){}
    private static Purchase gift(TownLife.Town t,TownLife.Citizen c,String id,TownLife.Citizen recipient,int due){
        var product=RetailLife.product(id);
        if(c.money-product.price()<reserve(c)||t.retailStock.getOrDefault(id,0)<=0)return null;
        return new Purchase(id,null,recipient,due,product.price());
    }
    private static Purchase choose(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!ready(t,c,clock)||clock.minute<960||clock.minute>=1140||(clock.day-1)%7<5||
            get(t,c,"week")==1+(clock.day-1)/7)return null;
        if(get(t,c,"pending-item")==0){
            var child=t.people.values().stream().filter(x->x.alive&&x.age>=3&&x.age<18&&parent(t,c,x)&&
                x.homeUnit.equals(c.homeUnit)&&birthday(t,x,clock.day)-clock.day<=6&&
                t.industryStocks.getOrDefault("birthday-gift:"+x.id,0)!=birthday(t,x,clock.day))
                .sorted(Comparator.comparing(x->x.id)).findFirst().orElse(null);
            if(child!=null){
                String item=child.age<7?"teddy-bear":child.curiosity>=60?"storybook":"toy-ball";
                var p=gift(t,c,item,child,birthday(t,child,clock.day));
                if(p!=null)return p;
            }
        }
        int temp=WeatherLife.at(clock.day,clock.minute).temperatureC();
        String ideal=temp<5?"winter":temp<13?"work":temp>=24?"summer":"casual";
        var outfit=WardrobeLife.OUTFITS.stream().filter(o->o.id().equals(ideal)).findFirst().orElseThrow();
        if((!WardrobeLife.owns(t,c,outfit)||WardrobeLife.condition(t,c,outfit)<40)&&
            WardrobeLife.stock(t,outfit)>0&&c.money-outfit.price()>=reserve(c))
            return new Purchase(null,outfit,null,0,outfit.price());
        if(c.curiosity>=65&&t.industryStocks.getOrDefault(holding(c,"storybook"),0)==0)
            return gift(t,c,"storybook",null,0);
        if(c.sociability>=65&&t.industryStocks.getOrDefault(holding(c,"scarf"),0)==0)
            return gift(t,c,"scarf",null,0);
        return null;
    }
    // Called only after essential supplies, work and social appointments are prioritized.
    static boolean wantsShop(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!ready(t,c,clock)||clock.minute>=1200)return false;
        if(get(t,c,"day")==clock.day&&clock.minute<get(t,c,"minute")+15)return true;
        return choose(t,c,clock)!=null;
    }
    static boolean purchase(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!c.presentAt(TownLife.Place.PORTICI)||!c.currentSpace().equals(TownLife.publicSpace(c.place)))return false;
        var p=choose(t,c,clock);
        if(p==null)return false;
        int balance=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),p.price);
        if(p.outfit!=null){
            var o=p.outfit;
            t.industryStocks.put("clothes:stock:"+o.id(),WardrobeLife.stock(t,o)-1);
            WardrobeLife.set(t,c,o.id()+"-owned",1);
            WardrobeLife.set(t,c,o.id()+"-wear",0);
        }else{
            String owned=holding(c,p.item);
            int quantity=Math.addExact(t.industryStocks.getOrDefault(owned,0),1);
            t.retailStock.merge(p.item,-1,Integer::sum);
            t.industryStocks.put(owned,quantity);
            if(p.recipient!=null){
                put(t,c,"pending-item",GIFTS.indexOf(p.item)+1);
                put(t,c,"pending-recipient",p.recipient.generationIndex);
                put(t,c,"pending-due",p.due);
                t.industryStocks.put("birthday-gift:"+p.recipient.id,p.due);
            }
        }
        c.money-=p.price;
        t.employerFunds.put("shop-assistant",balance);
        put(t,c,"week",1+(clock.day-1)/7);
        put(t,c,"day",clock.day);put(t,c,"minute",clock.minute);
        return true;
    }
    static boolean carryingBag(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        return c.alive&&c.travelRemaining==0&&get(t,c,"day")==day&&minute>=get(t,c,"minute")&&minute<get(t,c,"minute")+30;
    }
    static void home(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!autonomous(t,c)||!HomeRoutineLife.atHome(c)||clock.minute%30!=0)return;
        int item=get(t,c,"pending-item");
        if(item<1||item>GIFTS.size())return;
        var child=DemographyLife.byIndex(t,get(t,c,"pending-recipient"));
        int due=get(t,c,"pending-due");
        if(child==null||!child.alive||clock.day>due+30){
            put(t,c,"pending-item",0);return; // purchased item remains with buyer
        }
        if(clock.day<due||clock.minute<1080||clock.minute>=1260||
            !HomeRoutineLife.atHome(child)||!child.homeUnit.equals(c.homeUnit)||
            Set.of("SLEEPING","SICK_LEAVE","WORKING").contains(c.activity)||
            Set.of("SLEEPING","SICK_LEAVE","WORKING").contains(child.activity))return;
        String id=GIFTS.get(item-1),owned=holding(c,id),received=holding(child,id);
        int quantity=t.industryStocks.getOrDefault(owned,0);
        if(quantity<1){put(t,c,"pending-item",0);return;}
        int next=Math.addExact(t.industryStocks.getOrDefault(received,0),1);
        t.industryStocks.put(owned,quantity-1);t.industryStocks.put(received,next);
        put(t,c,"pending-item",0);
        var r=t.peerRelationships.computeIfAbsent(SocialNetwork.key(c.id,child.id),k->new SocialLife.Relationship(c.id,child.id));
        r.affinity=Math.min(100,r.affinity+2);r.trust=Math.min(100,r.trust+1);
        r.conversations++;if(r.encounters==0)r.encounters=1;
    }
}
