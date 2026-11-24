package com.holovin.npclife;

import java.util.ArrayList;

/** An employed NPC crew must move real ore through a finite production chain. */
public final class IndustrySmokeTest {
    private IndustrySmokeTest() { }

    public static void main(String[] args) {
        var world=new WorldState();
        var town=TownLife.create(world);
        check(IndustryLife.report(town).ironRemaining()==80 &&
                IndustryLife.report(town).copperRemaining()==40,
                "fresh world starts with finite iron and copper seams");
        var generated=PopulationGenerator.add(town,12);
        check(generated.stream().anyMatch(c->c.occupation.equals("Miner") &&
                        c.workplace==TownLife.Place.QUARRY),
                "default first-launch population includes real miners");
        check(generated.stream().anyMatch(c->c.occupation.equals("Factory machinist") &&
                        c.workplace==TownLife.Place.FACTORY),
                "default first-launch population includes factory workers");

        // Control worker attendance explicitly to isolate industry logic from
        // holiday, illness, lunch and individual commute schedule decisions.
        var miner=generated.stream().filter(c->c.occupation.equals("Miner")).findFirst().orElseThrow();
        var factory=generated.stream().filter(c->c.occupation.equals("Factory machinist"))
                .findFirst().orElseThrow();
        for(var worker:new TownLife.Citizen[]{miner,factory}) {
            worker.place=worker.workplace;
            worker.destination=worker.workplace;
            worker.travelRemaining=0;
            worker.employed=true;
            worker.activity="WORKING";
        }
        var events=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        long ironBefore=ironEquivalent(town);
        long copperBefore=copperEquivalent(town);

        // No extraction is possible when a real employee doesn't attend.
        miner.activity="SICK_LEAVE";
        world.minute=IndustryLife.EXTRACT_MINUTE;
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).ironRemaining()==80 &&
                        IndustryLife.report(town).oreAtQuarry()==0,
                "sick miner cannot operate a quarry remotely");

        // On a different day the miner returns, and the daily sequence works.
        world.day=2;
        miner.activity="WORKING";
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).ironRemaining()==78 &&
                        IndustryLife.report(town).copperRemaining()==39 &&
                        IndustryLife.report(town).oreAtQuarry()==3,
                "quarry removed exactly two iron and one copper ore units");
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).ironRemaining()==78,
                "same game minute cannot re-extract minerals");

        world.minute=IndustryLife.TRANSPORT_MINUTE;
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).oreAtQuarry()==0 &&
                        IndustryLife.report(town).oreAtFactory()==3,
                "transport moves actual cargo without duplication");
        world.minute=IndustryLife.REFINE_MINUTE;
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).oreAtFactory()==0 &&
                        IndustryLife.report(town).factorySteel()==2 &&
                        IndustryLife.report(town).factoryCopper()==1,
                "factory refines delivered ore only while staffed");
        world.minute=IndustryLife.FABRICATE_MINUTE;
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).factorySteel()==1 &&
                        IndustryLife.report(town).factoryParts()==2,
                "one steel unit is converted into two simplified spare-parts units");
        world.minute=IndustryLife.DELIVER_MINUTE;
        IndustryLife.tick(town,world,events,seen);
        var report=IndustryLife.report(town);
        check(report.factorySteel()==0 && report.factoryCopper()==0 &&
                        report.factoryParts()==0,
                "shipped goods leave factory shelves");
        check(report.workshopParts()==8 && report.workshopSteel()==5 &&
                        report.workshopCopper()==4,
                "existing technical repair materials receive actual deliveries");
        IndustryLife.tick(town,world,events,seen);
        check(IndustryLife.report(town).workshopParts()==8,
                "same-day transport or delivery cannot be repeated for free");
        check(ironEquivalent(town)==ironBefore && copperEquivalent(town)==copperBefore,
                "all industrial output can be traced to finite mineral deposits");
        check(events.stream().anyMatch(e->e.actorId().equals(miner.id) &&
                        e.text().contains("extracted")),
                "extraction has an actual worker and public world event");
        check(events.stream().anyMatch(e->e.actorId().equals(factory.id) &&
                        e.text().contains("dispatched")),
                "delivery has an actual worker and public world event");

        // A factory without staff cannot refine or ship, even if ore exists.
        var halted=town.copy();
        var haltedWorker=halted.get(factory.id);
        haltedWorker.employed=false;
        halted.industryStocks.put("factory:iron-ore",2);
        world.day=3;
        world.minute=IndustryLife.REFINE_MINUTE;
        IndustryLife.tick(halted,world,new ArrayList<>(),new ArrayList<>());
        check(halted.industryStocks.get("factory:iron-ore")==2,
                "unemployed factory worker cannot manufacture goods");

        // Minerals do not magically regenerate and zero remains zero.
        var depleted=town.copy();
        depleted.industryStocks.put("seam:iron-ore",0);
        depleted.industryStocks.put("seam:copper-ore",0);
        world.day=4;
        world.minute=IndustryLife.EXTRACT_MINUTE;
        IndustryLife.tick(depleted,world,new ArrayList<>(),new ArrayList<>());
        check(depleted.industryStocks.get("seam:iron-ore")==0 &&
                        depleted.industryStocks.get("seam:copper-ore")==0,
                "finite mineral seams stay exhausted");

        System.out.println("IndustrySmokeTest: ALL CHECKS PASSED");
    }

    private static long ironEquivalent(TownLife.Town town) {
        return (long)IndustryLife.stock(town,"seam:iron-ore")
                +IndustryLife.stock(town,"quarry:iron-ore")
                +IndustryLife.stock(town,"factory:iron-ore")
                +IndustryLife.stock(town,"factory:steel")
                +IndustryLife.stock(town,"factory:spare-parts")/2
                +town.technicalMaterials.getOrDefault("steel",0)
                +town.technicalMaterials.getOrDefault("spare-parts",0)/2;
    }

    private static long copperEquivalent(TownLife.Town town) {
        return (long)IndustryLife.stock(town,"seam:copper-ore")
                +IndustryLife.stock(town,"quarry:copper-ore")
                +IndustryLife.stock(town,"factory:copper-ore")
                +IndustryLife.stock(town,"factory:copper")
                +town.technicalMaterials.getOrDefault("copper",0);
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
