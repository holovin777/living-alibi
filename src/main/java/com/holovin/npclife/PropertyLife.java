package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** Physical possessions: legal ownership is distinct from current custody.
 *  These records are world truth, never automatically detective-visible.
 */
final class PropertyLife {
    private PropertyLife() { }

    enum Type { WALLET, PHONE, BICYCLE }
    enum Action { ACQUIRED_AT_MIGRATION, PURCHASED, STOLEN, RETURNED }

    static final class Item {
        final String id;
        final Type type;
        String ownerId;
        String holderId; // null: kept in storedSpace rather than carried
        String storedSpace;
        boolean stolen;
        final int value;
        final int createdDay;

        Item(String id,Type type,String ownerId,String holderId,String storedSpace,
             boolean stolen,int value,int createdDay) {
            if(id==null || id.isBlank() || ownerId==null || ownerId.isBlank())
                throw new IllegalArgumentException("An item needs an identity and an owner.");
            this.id=id;this.type=type;this.ownerId=ownerId;this.holderId=holderId;
            this.storedSpace=storedSpace;this.stolen=stolen;this.value=value;this.createdDay=createdDay;
        }
        Item copy() {return new Item(id,type,ownerId,holderId,storedSpace,stolen,value,createdDay);}
        String currentSpace(TownLife.Town town) {
            if(holderId==null)return storedSpace;
            var holder=town.people.get(holderId);
            if(holder==null)return "unknown";
            return holder.travelRemaining>0?"in transit with "+holderId:holder.currentSpace();
        }
    }

    record Change(int day,int minute,Action action,String itemId,String fromId,
                  String toId,String details) { }

    /** Explicit migration-time endowment; no fictitious pre-migration history is created. */
    static void ensureStarterItems(TownLife.Town town,WorldState clock) {
        for(var resident:town.people.values()) {
            if(!town.items.containsKey("wallet-"+resident.id))
                addStarter(town,clock,"wallet-"+resident.id,Type.WALLET,resident,35,true);
            if(!town.items.containsKey("phone-"+resident.id))
                addStarter(town,clock,"phone-"+resident.id,Type.PHONE,resident,180,true);
            if(Math.floorMod(resident.id.hashCode(),3)==0 &&
                    !town.items.containsKey("bicycle-"+resident.id))
                addStarter(town,clock,"bicycle-"+resident.id,Type.BICYCLE,resident,240,false);
        }
    }

    private static void addStarter(TownLife.Town town,WorldState clock,String id,Type type,
                                   TownLife.Citizen owner,int value,boolean carried) {
        var item=new Item(id,type,owner.id,carried?owner.id:null,owner.homeUnit,false,value,clock.day);
        town.items.put(id,item);
        town.pendingProperty.add(new Change(clock.day,clock.minute,
                Action.ACQUIRED_AT_MIGRATION,id,"system",owner.id,"Inventory initialized; not a historical purchase"));
    }

    static Item carriedWallet(TownLife.Town town,TownLife.Citizen owner) {
        for(var item:town.items.values())
            if(item.type==Type.WALLET && item.ownerId.equals(owner.id) &&
                    owner.id.equals(item.holderId) && !item.stolen)return item;
        return null;
    }

    /** Transfer custody, not legal title. This operation is authoritative and persistent. */
    static String stealWallet(TownLife.Town town,WorldState clock,
                              TownLife.Citizen offender,TownLife.Citizen victim) {
        var wallet=carriedWallet(town,victim);
        if(wallet==null)throw new IllegalArgumentException("The victim has no carried wallet to steal.");
        if(offender.id.equals(victim.id) || !offender.alive || !victim.alive ||
                offender.travelRemaining>0 || victim.travelRemaining>0 ||
                !offender.currentSpace().equals(victim.currentSpace()) ||
                offender.currentSpace().startsWith("unit:"))
            throw new IllegalArgumentException("A physical theft requires public co-presence.");
        wallet.holderId=offender.id;
        wallet.storedSpace=null;
        wallet.stolen=true;
        town.pendingProperty.add(new Change(clock.day,clock.minute,Action.STOLEN,
                wallet.id,victim.id,offender.id,"Custody changed; legal ownership retained"));
        return wallet.id;
    }

    /** A wallet can be replaced only by purchasing a NEW item while at the market. */
    static void tick(TownLife.Town town,WorldState clock) {
        if(clock.minute<420 || clock.minute>=1200 || clock.minute%30!=0)return;
        for(var resident:town.people.values()) {
            if(!resident.alive || resident.travelRemaining>0 ||
                    resident.place!=TownLife.Place.MARKET || resident.money<40 ||
                    carriedWallet(town,resident)!=null)continue;
            int next=1;
            String id;
            do{id="wallet-"+resident.id+"-replacement-%03d".formatted(next++);}
            while(town.items.containsKey(id));
            resident.money-=12;
            if(resident.id.equals("marco"))clock.money=resident.money;
            var item=new Item(id,Type.WALLET,resident.id,resident.id,null,false,35,clock.day);
            town.items.put(id,item);
            town.pendingProperty.add(new Change(clock.day,clock.minute,Action.PURCHASED,
                    id,"market",resident.id,"Replacement wallet purchased for EUR 12"));
        }
    }

    static List<Item> relevantItems(TownLife.Town town,String residentId) {
        List<Item> result=new ArrayList<>();
        for(var item:town.items.values())
            if(item.ownerId.equals(residentId) || residentId.equals(item.holderId))result.add(item);
        return result;
    }
}
