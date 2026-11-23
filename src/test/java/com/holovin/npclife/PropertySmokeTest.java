package com.holovin.npclife;

import java.util.ArrayList;

/** Genuine movable wallets: the legal owner is never replaced by the thief. */
public final class PropertySmokeTest {
    private PropertySmokeTest() { }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
    public static void main(String[] args) {
        var clock=new WorldState();clock.day=12;clock.minute=1050;
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,4);
        PropertyLife.ensureStarterItems(town,clock);
        int size=town.items.size();
        PropertyLife.ensureStarterItems(town,clock);
        check(town.items.size()==size,"initial inventory must be idempotent");
        var offender=town.get("marco");var victim=town.get("elena");
        for(var p:town.people.values()) {
            p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
            p.activity="FREE_TIME";p.money=80;
        }
        var original=PropertyLife.carriedWallet(town,victim);
        check(original!=null,"Elena begins with a uniquely identified wallet");
        var realId=original.id;
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var incident=InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,
                offender,victim,truth,seen);
        check(realId.equals(incident.physicalItemId),"case references a specific physical wallet");
        var stolen=town.items.get(realId);
        check(stolen.ownerId.equals("elena"),"the theft does not transfer legal title");
        check(stolen.holderId.equals("marco") && stolen.stolen,"custody moved to the offender");
        check(PropertyLife.carriedWallet(town,victim)==null,"victim no longer carries the stolen wallet");
        check(town.pendingProperty.stream().anyMatch(e->e.action()==PropertyLife.Action.STOLEN &&
                e.itemId().equals(realId)),"audit records the actual item transfer");
        check(!truth.getFirst().text().contains(realId),"public crime report hides item identifier");
        check(!truth.getFirst().text().contains(offender.name),"public crime report hides offender");
        var copy=town.copy();
        copy.items.get(realId).holderId="resident-0001";
        check(town.items.get(realId).holderId.equals("marco"),"candidate copy does not mutate saved truth");
        offender.place=TownLife.Place.CAFE;
        check(stolen.currentSpace(town).equals(TownLife.publicSpace(TownLife.Place.CAFE)),
                "a carried stolen wallet follows its actual holder");
        victim.place=TownLife.Place.MARKET;victim.travelRemaining=0;
        victim.money=85;clock.minute=600;
        int before=victim.money;
        PropertyLife.tick(town,clock);
        var replacement=PropertyLife.carriedWallet(town,victim);
        check(replacement!=null && !replacement.id.equals(realId),"replacement has another unique ID");
        check(victim.money==before-12,"replacement purchase costs actual money");
        PropertyLife.tick(town,clock);
        check(victim.money==before-12,"replacement purchase is not repeated");
        check(town.items.get(realId).stolen,"the old stolen wallet cannot be erased by replacement");
        System.out.println("PropertySmokeTest: ALL CHECKS PASSED");
    }
}
