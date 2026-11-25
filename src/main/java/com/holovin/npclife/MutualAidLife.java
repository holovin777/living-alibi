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
    record Notice(String residentId,String name,String item,int day) {}

    static java.util.List<Notice> notices(TownLife.Town town,int day) {
        var result=new java.util.ArrayList<Notice>();
        for(var resident:town.people.values()){
            for(var item:java.util.List.of("bread","water")){
                var kind=item.equals("bread")?SocialMemoryLife.Kind.AUTO_BREAD:SocialMemoryLife.Kind.AUTO_WATER;
                if(SocialMemoryLife.last(town,resident.id,kind)==day)
                    result.add(new Notice(resident.id,resident.name,item,day));
            }
        }
        return java.util.List.copyOf(result);
    }

    static void tick(TownLife.Town town,WorldState clock) {
        if(clock.minute%15!=0||RebirthLife.active(town)||!town.player.alive||town.player.travelRemaining>0)return;
        // A public encounter, with a real awake adult donor; no remote gifts.
        if(town.player.hunger<80&&town.player.thirst<80)return;
        for(var donor:PlayerLife.visible(town)){
            if(donor.generationIndex<=0||donor.age<18||donor.sociability<55||
                    java.util.Set.of("SLEEPING","TRAVELING","SICK_LEAVE").contains(donor.activity))continue;
            // One unsolicited transfer per tick, with the same donor/day limit as requested aid.
            for(String item:java.util.List.of("water","bread")){
                int need=item.equals("water")?town.player.thirst:town.player.hunger;
                if(need<80||!receive(town,donor,item,clock.day))continue;
                SocialMemoryLife.remember(town,donor.id,item.equals("water")?SocialMemoryLife.Kind.AUTO_WATER:SocialMemoryLife.Kind.AUTO_BREAD,clock.day);
                return;
            }
        }
    }

    static boolean receive(TownLife.Town town,TownLife.Citizen resident,String item,int day) {
        boolean water=item.equals("water");
        var contact=town.player.contacts.get(resident.id);
        if(contact==null||contact.trust<60||contact.affinity<12)return false;
        if(SocialMemoryLife.last(town,resident.id,SocialMemoryLife.Kind.RECEIVED_AID)==day)return false;
        int stock=water?resident.water:resident.food;
        int donorNeed=water?resident.thirst:resident.hunger;
        int playerNeed=water?town.player.thirst:town.player.hunger;
        if(stock<3||donorNeed>=65||playerNeed<65||FoodLife.quantity(town.player,item,false)>0||FoodLife.total(town.player,false)>=12)return false;
        FoodLife.change(town.player,item,false,1);
        if(water)resident.water--;else resident.food--;
        SocialMemoryLife.remember(town,resident.id,SocialMemoryLife.Kind.RECEIVED_AID,day);
        return true;
    }
}
