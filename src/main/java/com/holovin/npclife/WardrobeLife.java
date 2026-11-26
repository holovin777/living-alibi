package com.holovin.npclife;

import java.util.List;

/** Small portable wardrobe with individual wear and season-dependent comfort. */
final class WardrobeLife {
    private WardrobeLife(){}
    record Outfit(String id,int code,int price,int warmth){}
    static final List<Outfit> OUTFITS=List.of(new Outfit("casual",0,15,1),new Outfit("summer",1,20,0),
            new Outfit("winter",2,45,3),new Outfit("work",3,35,2));
    static int get(TownLife.Town t,TownLife.Citizen c,String key){return EducationLife.get(t,c,"wardrobe-"+key);}
    static void set(TownLife.Town t,TownLife.Citizen c,String key,int value){EducationLife.set(t,c,"wardrobe-"+key,value);}
    static Outfit worn(TownLife.Town t,TownLife.Citizen c){return OUTFITS.stream().filter(o->o.code()==get(t,c,"worn")).findFirst().orElse(OUTFITS.getFirst());}
    static boolean owns(TownLife.Town t,TownLife.Citizen c,Outfit o){return o.code()==0||get(t,c,o.id()+"-owned")>0;}
    static int condition(TownLife.Town t,TownLife.Citizen c,Outfit o){return 100-Math.min(100,get(t,c,o.id()+"-wear"));}
    static boolean shop(TownLife.Citizen c,int minute){return RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==TownLife.Place.PORTICI&&minute>=480&&minute<1200;}
    static final int SHOP_CAPACITY=5;
    static int stock(TownLife.Town t,Outfit o){return t.industryStocks.getOrDefault("clothes:stock:"+o.id(),SHOP_CAPACITY);}
    record Item(String id,int price,int condition,boolean owned,boolean worn,boolean canBuy,boolean canWear,int stock){}
    record View(String worn,int comfort,List<Item> items){}
    static int comfort(TownLife.Town t,TownLife.Citizen c,int temperature){
        var o=worn(t,c);int warmth=condition(t,c,o)<25?Math.max(0,o.warmth()-1):o.warmth();
        int ideal=temperature<5?3:temperature<13?2:temperature<24?1:0;
        return Math.max(0,100-30*Math.abs(warmth-ideal));
    }
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        var worn=worn(t,c);
        var items=OUTFITS.stream().map(o->new Item(o.id(),o.price(),condition(t,c,o),owns(t,c,o),o==worn,
            shop(c,minute)&&stock(t,o)>0&&c.money>=o.price()+20&&(!owns(t,c,o)||condition(t,c,o)<100),
            RebirthLife.atHome(c)&&owns(t,c,o)&&o!=worn,stock(t,o))).toList();
        return new View(worn.id(),minute<0?100:comfort(t,c,WeatherLife.at(day,minute).temperatureC()),items);
    }
    static String act(TownLife.Town t,String action,int day,int minute){
        var c=RebirthLife.requireAdult(t);
        var item=view(t,c,day,minute).items().stream().filter(i->action.equals("buy-"+i.id())||action.equals("wear-"+i.id())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Unknown outfit."));
        var o=OUTFITS.stream().filter(x->x.id().equals(item.id())).findFirst().orElseThrow();
        if(action.startsWith("buy-")){
            if(!item.canBuy())throw new IllegalArgumentException("Visit the open clothing shop and keep 20 EUR in reserve.");
            int next=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),o.price());
            c.money-=o.price();t.employerFunds.put("shop-assistant",next);
            t.industryStocks.put("clothes:stock:"+o.id(),stock(t,o)-1);
            set(t,c,o.id()+"-owned",1);set(t,c,o.id()+"-wear",0);
            return "Purchased an outfit.";
        }
        if(!item.canWear())throw new IllegalArgumentException("Change into an owned outfit at home.");
        set(t,c,"worn",o.code());
        return "Changed outfit.";
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(clock.minute==360&&t.industryStocks.getOrDefault("clothes:delivery-day",0)<clock.day){
            for(var o:OUTFITS)t.industryStocks.put("clothes:stock:"+o.id(),Math.min(SHOP_CAPACITY,stock(t,o)+1));
            t.industryStocks.put("clothes:delivery-day",clock.day);
        }
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c))return;
        if(clock.minute==360&&get(t,c,"last-day")<clock.day){
            set(t,c,"last-day",clock.day);
            var o=worn(t,c);set(t,c,o.id()+"-wear",Math.min(100,get(t,c,o.id()+"-wear")+1));
        }
        if(clock.minute%60!=0||!RebirthLife.awakePresent(c)||RebirthLife.atHome(c)||PlayerLife.exposure(c.place)<2)return;
        if(comfort(t,c,WeatherLife.at(clock.day,clock.minute).temperatureC())<=40)c.fatigue=Math.min(100,c.fatigue+1);
    }
}
