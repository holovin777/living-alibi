package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generic ingredient accounting while preserving the existing bread/water save columns. */
final class FoodLife {
    private FoodLife() { }
    record Recipe(String id,String label,int minutes,int hunger,int thirst,Map<String,Integer> ingredients) { }
    static final List<Recipe> RECIPES=List.of(
            new Recipe("bread-soup","Bread soup",15,45,20,Map.of("bread",1,"water",1)),
            new Recipe("pasta-verdure","Pasta with vegetables",25,65,5,Map.of("pasta",1,"vegetables",1,"water",1)),
            new Recipe("frittata","Vegetable frittata",20,60,0,Map.of("eggs",1,"vegetables",1)),
            new Recipe("pancakes","Pancakes",25,60,5,Map.of("flour",1,"eggs",1,"milk",1)));
    static Recipe recipe(String id) {
        return RECIPES.stream().filter(r->r.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown recipe."));
    }
    static int quantity(PlayerLife.Player p,String id,boolean home) {
        return switch(id) {
            case "bread" -> home?p.storedBread:p.bread;
            case "water" -> home?p.storedWater:p.bottledWater;
            default -> (home?p.pantryItems:p.bagItems).getOrDefault(id,0);
        };
    }
    static Map<String,Integer> inventory(PlayerLife.Player p,boolean home) {
        var result=new LinkedHashMap<String,Integer>();
        for(var item:RetailLife.PRODUCTS)result.put(item.id(),quantity(p,item.id(),home));
        return result;
    }
    static int total(PlayerLife.Player p,boolean home) {
        return inventory(p,home).values().stream().mapToInt(Integer::intValue).sum();
    }
    static void change(PlayerLife.Player p,String id,boolean home,int delta) {
        RetailLife.product(id);
        int next=Math.addExact(quantity(p,id,home),delta);
        if(next<0)throw new IllegalArgumentException("Not enough "+id+".");
        switch(id) {
            case "bread" -> {if(home)p.storedBread=next;else p.bread=next;}
            case "water" -> {if(home)p.storedWater=next;else p.bottledWater=next;}
            default -> {
                var items=home?p.pantryItems:p.bagItems;
                if(next==0)items.remove(id);else items.put(id,next);
            }
        }
    }
    static String cook(PlayerLife.Player p,Recipe recipe,int day,int minute) {
        HousingLife.requireStay(p,day,minute,recipe.minutes());
        if(p.hunger==0)throw new IllegalArgumentException("You are not hungry.");
        for(var ingredient:recipe.ingredients().entrySet())
            if(quantity(p,ingredient.getKey(),true)<ingredient.getValue())
                throw new IllegalArgumentException("Missing ingredient: "+ingredient.getKey()+".");
        for(var ingredient:recipe.ingredients().entrySet())
            change(p,ingredient.getKey(),true,-ingredient.getValue());
        p.hunger=Math.max(0,p.hunger-recipe.hunger());
        p.thirst=Math.max(0,p.thirst-recipe.thirst());
        return "Prepared and ate "+recipe.label()+".";
    }
}
