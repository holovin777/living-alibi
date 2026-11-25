package com.holovin.npclife;

/** One-unit, voluntary transfers between a visitor and an awake nearby resident.
 * Supplies are moved, never created. Autonomous aid uses the same accounting.
 */
final class MutualAidLife {
    private MutualAidLife() {}
    static boolean give(TownLife.Town town,TownLife.Citizen resident,String item,int day) {
        boolean water=item.equals("water");
        int stock=water?resident.water:resident.food;
        int need=water?resident.thirst:resident.hunger;
        if(stock>=3 || (stock>=1&&need<65))return false;
        if(FoodLife.quantity(town.player,item,false)<1)return false;
        FoodLife.change(town.player,item,false,-1);
        if(water)resident.water++;else resident.food++;
        SocialMemoryLife.remember(town,resident.id,SocialMemoryLife.Kind.SUPPORT,day);
        return true;
    }
    static boolean receive(TownLife.Town town,TownLife.Citizen resident,String item,int day) {
        boolean water=item.equals("water");
        var contact=town.player.contacts.get(resident.id);
        if(contact==null||contact.trust<60||contact.affinity<12)return false;
        if(SocialMemoryLife.last(town,resident.id,SocialMemoryLife.Kind.RECEIVED_AID)==day)return false;
        int stock=water?resident.water:resident.food;
        int donorNeed=water?resident.thirst:resident.hunger;
        int playerNeed=water?town.player.thirst:town.player.hunger;
        if(stock<3||donorNeed>=65||playerNeed<65||FoodLife.quantity(town.player,item,false)>0)return false;
        FoodLife.change(town.player,item,false,1);
        if(water)resident.water--;else resident.food--;
        SocialMemoryLife.remember(town,resident.id,SocialMemoryLife.Kind.RECEIVED_AID,day);
        return true;
    }
}
