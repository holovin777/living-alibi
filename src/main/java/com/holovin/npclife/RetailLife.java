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
            new Product("rice","Rice",2,20,0));
    static void ensure(TownLife.Town t) {
        for(var p:PRODUCTS)t.retailStock.putIfAbsent(p.id(),p.capacity());
    }
    static void tick(TownLife.Town t,int minute) {
        if(minute!=360)return;
        for(var p:PRODUCTS)if(!FarmLife.local(p.id()))t.retailStock.compute(p.id(),
                (id,stock)->Math.min(p.capacity(),(stock==null?0:stock)+p.dailyDelivery()));
    }
    static Product product(String id) {
        return PRODUCTS.stream().filter(p->p.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown retail product."));
    }
    static boolean hasCounter(TownLife.Place place) {
        return place==TownLife.Place.MARKET || place==TownLife.Place.PORTICI || place==TownLife.Place.FUEL_STATION;
    }
    static boolean availableAt(TownLife.Place place,Product product) {
        return hasCounter(place) && (place!=TownLife.Place.FUEL_STATION || product.id().equals("bread") || product.id().equals("water"));
    }
    static String buy(TownLife.Town t,String id,int minute) {
        var player=t.player;
        PlayerLife.requireAlive(player);
        var product=product(id);
        if(!availableAt(player.place,product) || player.travelRemaining>0)
            throw new IllegalArgumentException("This product is not available at your local counter.");
        if(minute<480 || minute>=1200)
            throw new IllegalArgumentException("Market opens 08:00–20:00.");
        if(t.retailStock.getOrDefault(id,0)<1)
            throw new IllegalArgumentException("This product is sold out.");
        if(player.money<product.price())throw new IllegalArgumentException("Not enough money.");
        if(FoodLife.total(player,false)>=12)
            throw new IllegalArgumentException("Your bag is full (12 units).");
        player.money-=product.price();
        t.employerFunds.merge("shop-assistant",product.price(),Math::addExact);
        t.retailStock.merge(id,-1,Integer::sum);
        FoodLife.change(player,id,false,1);
        return "Bought "+product.label()+" for "+product.price()+" EUR.";
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
        if(!id.equals("water"))throw new IllegalArgumentException("Use this ingredient in your rented kitchen.");
        if(p.bottledWater<1)throw new IllegalArgumentException("No bottled water in your bag.");
        if(p.thirst==0)throw new IllegalArgumentException("You are not thirsty.");
        p.bottledWater--;p.thirst=Math.max(0,p.thirst-40);
        return "Drank bottled water.";
    }
}
