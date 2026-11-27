package com.holovin.npclife;

import java.util.List;

/** Finite retail counter; cash is transferred to the existing market payroll account. */
final class RetailLife {
    private RetailLife() { }
    record Product(String id,String label,int price,int capacity,int dailyDelivery) { }
    static final List<Product> PRODUCTS=List.of(
            new Product("bread","Bread",3,20,8),
            new Product("water","Bottled water",1,30,12),
            new Product("flour","Flour",2,18,6),
            new Product("eggs","Eggs",3,16,6),
            new Product("pasta","Pasta",2,20,8),
            new Product("vegetables","Vegetables",3,18,8),
            new Product("milk","Milk",2,16,6),
            new Product("rice","Rice",2,20,0),
            new Product("apples","Apples",3,12,0),
            new Product("pears","Pears",3,12,0),
            new Product("plums","Plums",3,12,0),
            new Product("grapes","Grapes",3,12,0),
            new Product("honey","Honey",5,8,0),
            new Product("grape-juice","Grape juice",7,8,0),
            new Product("wine","Wine",10,8,0),
            new Product("pizza","Pizza Margherita",7,16,12),
            new Product("gelato","Gelato",3,18,14));
    static void ensure(TownLife.Town t) {
        for(var p:PRODUCTS)t.retailStock.putIfAbsent(p.id(),(OrchardLife.isFruit(p.id())||p.id().equals("honey")||p.id().equals("grape-juice")||p.id().equals("wine"))?0:p.capacity());
    }
    static void tick(TownLife.Town t,int minute) {
        if(minute!=360||StaffingLife.enabled(t))return;
        for(var p:PRODUCTS)if(!FarmLife.local(p.id()))t.retailStock.compute(p.id(),
                (id,stock)->Math.min(p.capacity(),(stock==null?0:stock)+p.dailyDelivery()));
    }
    static Product product(String id) {
        return PRODUCTS.stream().filter(p->p.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown retail product."));
    }
    static boolean hasCounter(TownLife.Place place) {
        return place==TownLife.Place.MARKET || place==TownLife.Place.PORTICI || place==TownLife.Place.FUEL_STATION || place==TownLife.Place.BAKERY || place==TownLife.Place.CENTRO;
    }
    static boolean availableAt(TownLife.Place place,Product product) {
        if(place==TownLife.Place.CENTRO)return java.util.Set.of("pizza","gelato").contains(product.id());
        if(java.util.Set.of("pizza","gelato").contains(product.id()))return false;
        if(place==TownLife.Place.BAKERY)return java.util.Set.of("bread","flour","water").contains(product.id());
        return hasCounter(place) && (place!=TownLife.Place.FUEL_STATION || product.id().equals("bread") || product.id().equals("water"));
    }
    static int opens(TownLife.Place place){return place==TownLife.Place.CENTRO?660:place==TownLife.Place.BAKERY?420:480;}
    static int closes(TownLife.Place place){return place==TownLife.Place.CENTRO?1320:place==TownLife.Place.BAKERY?1080:1200;}
    static boolean open(TownLife.Place place,int minute){return hasCounter(place)&&minute>=opens(place)&&minute<closes(place);}
    static String buy(TownLife.Town t,String id,int minute) {
        var player=t.player;
        PlayerLife.requireAlive(player);
        var product=product(id);
        if(!availableAt(player.place,product) || player.travelRemaining>0)
            throw new IllegalArgumentException("This product is not available at your local counter.");
        if(!open(player.place,minute)||minute+2>closes(player.place))
            throw new IllegalArgumentException("The shop is closed or closing.");
        if(t.retailStock.getOrDefault(id,0)<1)
            throw new IllegalArgumentException("This product is sold out.");
        if(player.money<product.price())throw new IllegalArgumentException("Not enough money.");
        if(FoodLife.total(player,false)>=12)
            throw new IllegalArgumentException("Your bag is full (12 units).");
        player.money-=product.price();
        t.employerFunds.merge(salesAccount(id),product.price(),Math::addExact);
        t.retailStock.merge(id,-1,Integer::sum);
        FoodLife.change(player,id,false,1);
        return "Bought "+product.label()+" for "+product.price()+" EUR.";
    }
    static final int FOOD_WAGE=8;
    static boolean foodRole(String occupation){return "Pizzaiolo".equals(occupation)||"Gelataio".equals(occupation);}
    static String foodAccount(String occupation){return "Pizzaiolo".equals(occupation)?"pizzeria":"gelateria";}
    static String salesAccount(String id){return switch(id){case "pizza" -> "pizzeria";case "gelato" -> "gelateria";default -> "shop-assistant";};}
    static void foodShift(TownLife.Town t,TownLife.Citizen c,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!foodRole(c.occupation)||!c.employed||!c.alive||c.place!=TownLife.Place.CENTRO||c.travelRemaining>0)return;
        String account=foodAccount(c.occupation);
        int funds=t.employerFunds.getOrDefault(account,0);
        if(funds<FOOD_WAGE)return;
        t.employerFunds.put(account,funds-FOOD_WAGE);
        c.money=Math.addExact(c.money,FOOD_WAGE);
        String id="Pizzaiolo".equals(c.occupation)?"pizza":"gelato";
        int count=Math.min(2,product(id).capacity()-t.retailStock.getOrDefault(id,0));
        if(StaffingLife.enabled(t)){
            int made=FoodSupplyLife.prepare(t,id,count);
            if(made>0)TownLife.publish(t,clock,c.id,c.place,c.name+" prepared "+made+" portions from purchased ingredients.",events,observations);
            return;
        }
        int cost=count*(id.equals("pizza")?2:1);
        if(count>0&&funds-FOOD_WAGE>=cost){
            t.employerFunds.merge(account,-cost,Integer::sum);
            t.retailStock.merge(id,count,Integer::sum);
            TownLife.publish(t,clock,c.id,c.place,c.name+" prepared "+count+" portions of "+id+".",events,observations);
        }
    }
    static void foodCustomer(TownLife.Town t,TownLife.Citizen c,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(c.hunger<45||!open(TownLife.Place.CENTRO,clock.minute)||c.place!=TownLife.Place.CENTRO||c.travelRemaining>0)return;
        String id=c.hunger>=60?"pizza":"gelato";
        var p=product(id);
        if(c.money<p.price()+10||t.retailStock.getOrDefault(id,0)<1)return;
        c.money-=p.price();
        t.employerFunds.merge(salesAccount(id),p.price(),Math::addExact);
        t.retailStock.merge(id,-1,Integer::sum);
        c.hunger=Math.max(0,c.hunger-(id.equals("pizza")?65:20));
        TownLife.publish(t,clock,c.id,c.place,c.name+" bought and ate "+id+".",events,observations);
    }
    static final int PRODUCE_PRICE=2;
    static int produceSpace(TownLife.Town t) {
        return Math.max(0,product("vegetables").capacity()-t.retailStock.getOrDefault("vegetables",0)-FreightLife.reservedCrop(t,"vegetables"));
    }
    static String buyProduce(TownLife.Town t,int minute) {
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.MARKET)throw new IllegalArgumentException("Sell produce at the market.");
        if(minute<480||minute>=1200)throw new IllegalArgumentException("Market opens 08:00–20:00.");
        if(FoodLife.quantity(p,"vegetables",false)<1)throw new IllegalArgumentException("Bring vegetables in your bag.");
        if(produceSpace(t)<1)throw new IllegalArgumentException("The market has no unreserved vegetable shelf space.");
        if(t.employerFunds.getOrDefault("shop-assistant",0)<PRODUCE_PRICE)throw new IllegalArgumentException("Market cannot afford more produce.");
        FoodLife.change(p,"vegetables",false,-1);
        t.retailStock.merge("vegetables",1,Math::addExact);
        t.employerFunds.merge("shop-assistant",-PRODUCE_PRICE,Integer::sum);
        p.money=Math.addExact(p.money,PRODUCE_PRICE);
        return "Sold one vegetable unit for 2 EUR.";
    }
    static final int EGG_PRICE=2;
    static int eggSpace(TownLife.Town t) {
        return Math.max(0,product("eggs").capacity()-t.retailStock.getOrDefault("eggs",0));
    }
    static String buyEggs(TownLife.Town t,int minute) {
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.MARKET||minute<480||minute>=1200)
            throw new IllegalArgumentException("Sell eggs at Market 08:00–20:00.");
        if(FoodLife.quantity(p,"eggs",false)<1)throw new IllegalArgumentException("Bring eggs in your bag.");
        if(eggSpace(t)<1||t.employerFunds.getOrDefault("shop-assistant",0)<EGG_PRICE)
            throw new IllegalArgumentException("The market has insufficient storage or cash.");
        FoodLife.change(p,"eggs",false,-1);
        t.retailStock.merge("eggs",1,Math::addExact);
        t.employerFunds.merge("shop-assistant",-EGG_PRICE,Integer::sum);
        p.money=Math.addExact(p.money,EGG_PRICE);
        return "Sold one egg unit for 2 EUR.";
    }
    static int fruitSpace(TownLife.Town t,String id){
        if(!OrchardLife.isFruit(id))throw new IllegalArgumentException("Unknown fruit.");
        return Math.max(0,product(id).capacity()-t.retailStock.getOrDefault(id,0));
    }
    static String buyFruit(TownLife.Town t,String id,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.MARKET||minute<480||minute>=1200)throw new IllegalArgumentException("Sell fruit at Market 08:00–20:00.");
        if(fruitSpace(t,id)<1||t.employerFunds.getOrDefault("shop-assistant",0)<OrchardLife.SELL_PRICE)
            throw new IllegalArgumentException("Market cannot accept more fruit.");
        FoodLife.change(p,id,false,-1);
        t.retailStock.merge(id,1,Math::addExact);
        t.employerFunds.merge("shop-assistant",-OrchardLife.SELL_PRICE,Integer::sum);
        p.money=Math.addExact(p.money,OrchardLife.SELL_PRICE);
        return "Sold one "+id+" unit for 2 EUR.";
    }
    static int honeySpace(TownLife.Town t){
        return Math.max(0,product("honey").capacity()-t.retailStock.getOrDefault("honey",0));
    }
    static String buyHoney(TownLife.Town t,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.MARKET||minute<480||minute>=1200)
            throw new IllegalArgumentException("Sell honey at Market 08:00–20:00.");
        int funds=t.employerFunds.getOrDefault("shop-assistant",0);
        if(honeySpace(t)<1||funds<OrchardLife.HONEY_PRICE)
            throw new IllegalArgumentException("Market cannot accept more honey.");
        int newMoney=Math.addExact(p.money,OrchardLife.HONEY_PRICE);
        FoodLife.change(p,"honey",false,-1);t.retailStock.merge("honey",1,Math::addExact);
        t.employerFunds.put("shop-assistant",funds-OrchardLife.HONEY_PRICE);p.money=newMoney;
        return "Sold one honey unit for 3 EUR.";
    }
    static int cellarSpace(TownLife.Town t,String id) {
        if(!id.equals("grape-juice")&&!id.equals("wine"))throw new IllegalArgumentException("Unknown cellar product.");
        return Math.max(0,product(id).capacity()-t.retailStock.getOrDefault(id,0));
    }
    static String buyCellarProduct(TownLife.Town t,String id,int minute) {
        var p=t.player;TransportLife.ready(p);
        int space=cellarSpace(t,id);
        if(p.place!=TownLife.Place.MARKET||minute<480||minute>=1200)
            throw new IllegalArgumentException("Sell cellar products at Market 08:00–20:00.");
        int price=id.equals("wine")?OrchardLife.WINE_PRICE:OrchardLife.JUICE_PRICE;
        int funds=t.employerFunds.getOrDefault("shop-assistant",0);
        if(space<1||funds<price)throw new IllegalArgumentException("Market cannot accept more "+id+".");
        int newMoney=Math.addExact(p.money,price);
        FoodLife.change(p,id,false,-1);t.retailStock.merge(id,1,Math::addExact);
        t.employerFunds.put("shop-assistant",funds-price);p.money=newMoney;
        return "Sold one "+id+" unit for "+price+" EUR.";
    }
    static String consume(PlayerLife.Player p,String id) {
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0)throw new IllegalArgumentException("Finish traveling first.");
        product(id);
        if(id.equals("bread")) {
            if(p.bread<1)throw new IllegalArgumentException("No bread in your bag.");
            if(p.hunger==0)throw new IllegalArgumentException("You are not hungry.");
            p.bread--;p.hunger=Math.max(0,p.hunger-35);
            return "Ate bread.";
        }
        if(id.equals("pizza")||id.equals("gelato")) {
            if(p.hunger==0)throw new IllegalArgumentException("You are not hungry.");
            FoodLife.change(p,id,false,-1);
            p.hunger=Math.max(0,p.hunger-(id.equals("pizza")?65:20));
            return id.equals("pizza")?"Ate pizza.":"Ate gelato.";
        }
        if(id.equals("grape-juice")) {
            if(p.thirst==0&&p.hunger==0)throw new IllegalArgumentException("You do not need a drink.");
            FoodLife.change(p,id,false,-1);p.thirst=Math.max(0,p.thirst-30);p.hunger=Math.max(0,p.hunger-10);
            return "Drank grape juice.";
        }
        if(!id.equals("water"))throw new IllegalArgumentException("This item is not directly consumable.");
        if(p.bottledWater<1)throw new IllegalArgumentException("No bottled water in your bag.");
        if(p.thirst==0)throw new IllegalArgumentException("You are not thirsty.");
        p.bottledWater--;p.thirst=Math.max(0,p.thirst-40);
        return "Drank bottled water.";
    }
}
