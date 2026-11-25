package com.holovin.npclife;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API adapter works on first launch and keeps authoritative world state on disk.
 * No old database or pre-generated player information is required.
 */
class GameSessionTest {
    @TempDir Path directory;

    @Test
    void housePurchaseEndsRentalAndKeepsPantryAcrossRestart() throws Exception {
        Path file=directory.resolve("owned-home.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,game::buyHouse);
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(2));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=400;
            town.player.roomUntil=HousingLife.now(state.day,state.minute)+10080;
            town.player.storedBread=2;town.player.storedWater=1;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.buyHouse();
            assertEquals(80,game.snapshot().player().money());
            assertTrue(game.snapshot().player().ownsHouse());
            assertFalse(game.snapshot().player().roomActive());
            assertEquals(2,game.snapshot().player().storedBread());
            assertThrows(IllegalArgumentException.class,game::buyHouse);
            game.sleepInRoom(2);
            game.transferRoomItem("bread",false);
            game.move("north");
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(2));
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().player().ownsHouse());
            assertEquals(1,game.snapshot().player().storedBread());
            game.move("south");
            game.cookRoomMeal();
            assertEquals(0,game.snapshot().player().storedBread());
            assertEquals(0,game.snapshot().player().storedWater());
        }
    }

    @Test
    void logisticsDepotIsPublicAndSupportsCompanyManagement() throws Exception {
        Path file=directory.resolve("depot.db");
        try(var game=new GameSession(file)) {
            game.move("logistics_depot");
            assertEquals("LOGISTICS_DEPOT",game.snapshot().player().placeId());
            game.freightAction("register",null);
            assertEquals(30,game.snapshot().player().money());
            assertTrue(game.snapshot().logistics().yours());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,500);
            LogisticsLife.manage(town,"buy-truck");
            SecondFleetLife.manage(town,"buy-truck-2",null,state.day);
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,LogisticsLife.truckPlace(town));
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,SecondFleetLife.place(town));
            assertEquals(60,LogisticsLife.get(town,LogisticsLife.CASH));
            var worker=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).findFirst().orElseThrow();
            worker.place=TownLife.Place.LOGISTICS_DEPOT;worker.destination=worker.place;worker.travelRemaining=0;worker.sickUntilDay=0;
            LogisticsLife.hire(town,worker.id,state.day);
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,worker.workplace);
            town.player.carPlace=TownLife.Place.LOGISTICS_DEPOT;
            town.player.carFuel=6;town.player.carCondition=80;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals("LOGISTICS_DEPOT",game.snapshot().logistics().truckPlace());
            assertEquals("LOGISTICS_DEPOT",game.snapshot().logistics().secondTruck().place());
            assertTrue(game.snapshot().player().carDestinations().contains("FUEL_STATION"));
            game.drive("fuel_station");game.drive("logistics_depot");
            assertEquals("LOGISTICS_DEPOT",game.snapshot().player().placeId());
            assertEquals(4,game.snapshot().player().carFuel());
        }
    }

    @Test
    void twoTrucksReserveDistinctCargoAndPersistIndependentDrivers() throws Exception {
        Path file=directory.resolve("two-trucks.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);game.freightAction("register",null);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,400);
            LogisticsLife.park(town,TownLife.Place.WORKSHOP);
            SecondFleetLife.manage(town,"buy-truck-2",null,state.day);
            var workers=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(2).toList();
            for(var worker:workers) {
                worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
                worker.sickUntilDay=0;worker.money=100;
            }
            LogisticsLife.hire(town,workers.get(0).id,state.day);
            assertThrows(IllegalArgumentException.class,()->SecondFleetLife.manage(town,"hire-driver-2",workers.get(0).id,state.day));
            SecondFleetLife.manage(town,"hire-driver-2",workers.get(1).id,state.day);
            FarmLife.set(town,FarmLife.barn("vegetables"),8);FarmLife.set(town,FarmLife.barn("rice"),8);
            town.retailStock.put("vegetables",14);town.retailStock.put("rice",18);
            FreightLife.dispatch(town,FreightLife.ROUTES.get(2),true,true);
            SecondFleetLife.dispatch(town,FreightLife.ROUTES.get(2));
            assertEquals(4,FreightLife.reservedCrop(town,"vegetables"));
            assertEquals(2,FreightLife.reservedCrop(town,"rice"));
            assertEquals(0,FarmLife.deliver(town));
            assertEquals(88,LogisticsLife.get(town,LogisticsLife.CASH));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(FreightLife.automated(town));assertTrue(SecondFleetLife.active(town));
            var a=LogisticsLife.driver(town);var b=SecondFleetLife.driver(town);
            assertNotEquals(a.id,b.id);
            assertEquals(TownLife.Place.FARM,LogisticsLife.activeDriverDestination(town,a));
            assertEquals(TownLife.Place.FARM,LogisticsLife.activeDriverDestination(town,b));
            for(int k=0;k<5;k++) {
                a.place=FreightLife.truckPlace(town);a.destination=a.place;a.travelRemaining=0;
                b.place=SecondFleetLife.place(town);b.destination=b.place;b.travelRemaining=0;
                state.minute=555+15*k;
                LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
                SecondFleetLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            }
            assertFalse(FreightLife.active(town));assertFalse(SecondFleetLife.active(town));
            assertEquals(18,town.retailStock.get("vegetables"));assertEquals(20,town.retailStock.get("rice"));
            assertEquals(128,a.money);assertEquals(128,b.money);
            assertEquals(85,LogisticsLife.get(town,LogisticsLife.CONDITION));
            assertEquals(85,LogisticsLife.get(town,SecondFleetLife.HEALTH));
            assertEquals(168,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(2,LogisticsLife.financials(town).deliveries());
            assertEquals(80,LogisticsLife.financials(town).revenue());
        }
    }

    @Test
    void logisticsMaintenanceAndLedgerPersistAndChargeRealResources() throws Exception {
        Path file=directory.resolve("carrier-service.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);game.freightAction("register",null);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,100);
            LogisticsLife.park(town,TownLife.Place.WORKSHOP);
            LogisticsLife.set(town,LogisticsLife.CONDITION,2);
            town.technicalMaterials.put("spare-parts",1);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            assertThrows(IllegalArgumentException.class,()->FreightLife.dispatch(town,FreightLife.ROUTES.get(1),true,false));
            assertEquals(100,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(2,IndustryLife.stock(town,IndustryLife.FACTORY_PARTS));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("service-truck",null);
            assertEquals(32,game.snapshot().logistics().condition());
            assertEquals(85,game.snapshot().logistics().cash());
            assertEquals(15,game.snapshot().logistics().financials().maintenance());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("service-truck",null));
            game.move("factory");game.freightAction("load-company",null);
            assertEquals(29,game.snapshot().logistics().condition());
            game.freightAction("drive","workshop");game.freightAction("unload",null);
            assertEquals(26,game.snapshot().logistics().condition());
            assertEquals(113,game.snapshot().logistics().cash());
        }
        try(var game=new GameSession(file)) {
            var report=game.snapshot().logistics();
            assertEquals(26,report.condition());
            assertEquals(40,report.financials().revenue());
            assertEquals(4,report.financials().fuel());
            assertEquals(8,report.financials().relocation());
            assertEquals(13,report.financials().operatingNet());
            assertEquals(1,report.financials().deliveries());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(2,town.technicalMaterials.get("spare-parts"));
        }
    }

    @Test
    void hiredDriverDeliversWhileOwnerIsElsewhereAndReceivesRealPay() throws Exception {
        Path file=directory.resolve("auto-carrier.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);
            game.freightAction("register",null);
        }
        String id;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,100);
            LogisticsLife.park(town,TownLife.Place.FACTORY);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            var worker=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).findFirst().orElseThrow();
            worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
            worker.sickUntilDay=0;worker.money=100;worker.food=8;worker.water=8;
            id=worker.id;
            LogisticsLife.hire(town,id,state.day);
            assertEquals(90,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(110,worker.money);
            LogisticsLife.set(town,LogisticsLife.AUTO,1);
            // Explicitly model the scheduled dispatch at 09:15, independent of incidental NPC actions.
            FreightLife.dispatch(town,FreightLife.ROUTES.get(1),true,true);
            LogisticsLife.set(town,LogisticsLife.LAST_DAY,state.day);
            assertEquals(70,LogisticsLife.get(town,LogisticsLife.CASH));
            town.player.place=TownLife.Place.PARK;town.player.destination=town.player.place;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(FreightLife.automated(town));
            var worker=LogisticsLife.driver(town);
            assertEquals(id,worker.id);
            worker.place=TownLife.Place.FACTORY;worker.destination=worker.place;worker.travelRemaining=0;
            state.minute=555;
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(TownLife.Place.WORKSHOP,FreightLife.truckPlace(town));
            worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
            state.minute=570;
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertFalse(FreightLife.active(town));
            assertEquals(110,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(128,worker.money);
            assertEquals(30,town.player.money);
            assertEquals(1,LogisticsLife.get(town,LogisticsLife.COMPLETED));
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(128,worker.money);
        }
    }

    @Test
    void logisticsCompanyOwnsTruckAndReceivesContractRevenueNotPersonalWages() throws Exception {
        Path file=directory.resolve("logistics.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);
            game.freightAction("register",null);
            assertEquals(30,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("buy-truck",null));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=400;
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            for(int k=0;k<3;k++)game.freightAction("invest-100",null);
            game.freightAction("buy-truck",null);
            assertEquals(80,game.snapshot().logistics().cash());
            assertEquals(100,game.snapshot().player().money());
            assertTrue(game.snapshot().logistics().truck());
            game.move("factory");game.freightAction("load-company",null);
            assertTrue(game.snapshot().freight().companyRun());
            assertEquals(68,game.snapshot().logistics().cash());
            assertEquals("FACTORY",game.snapshot().logistics().truckPlace());
            assertEquals(100,game.snapshot().player().money());
            game.freightAction("drive","workshop");
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().logistics().yours());
            assertTrue(game.snapshot().freight().companyRun());
            assertEquals("WORKSHOP",game.snapshot().logistics().truckPlace());
            game.freightAction("unload",null);
            assertEquals(108,game.snapshot().logistics().cash());
            assertEquals(100,game.snapshot().player().money());
            game.freightAction("withdraw-25",null);
            assertEquals(83,game.snapshot().logistics().cash());
            assertEquals(125,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
        }
    }

    @Test
    void farmFreightReservesMarketCapacityAndDeliversOnlyAtEnd() throws Exception {
        Path file=directory.resolve("farm-freight.db");
        try(var game=new GameSession(file)) {
            game.move("quarry");game.waitMinutes(60);game.applyJob("freight-driver");game.move("farm");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            FarmLife.set(town,FarmLife.barn("vegetables"),8);
            FarmLife.set(town,FarmLife.barn("rice"),8);
            town.retailStock.put("vegetables",16);town.retailStock.put("rice",19);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("load",null);
            assertEquals("Farm produce",game.snapshot().freight().cargoLabel());
            assertEquals(2,game.snapshot().freight().firstUnits());
            assertEquals(1,game.snapshot().freight().secondUnits());
            game.freightAction("drive","bakery");
            game.freightAction("drive","cafe");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertEquals(0,FarmLife.deliver(town),"reserved retail spaces cannot be filled by scheduled deliveries");
            assertEquals(16,town.retailStock.get("vegetables"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("SOUTH_RESIDENCES",game.snapshot().freight().nextDestination());
            game.freightAction("drive","south_residences");
            game.freightAction("drive","market");
            game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
            assertEquals(18,game.snapshot().farm().marketVegetables());
            assertEquals(20,game.snapshot().farm().marketRice());
        }
    }

    @Test
    void industrialFreightSuppliesWorkshopWithoutDuplicatingParts() throws Exception {
        Path file=directory.resolve("parts-freight.db");
        try(var game=new GameSession(file)) {
            game.move("quarry");game.waitMinutes(60);game.applyJob("freight-driver");game.move("factory");
        }
        int parts,steel;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            parts=town.technicalMaterials.getOrDefault("spare-parts",0);
            steel=town.technicalMaterials.getOrDefault("steel",0);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("load",null);
            assertEquals("Industrial materials",game.snapshot().freight().cargoLabel());
            game.freightAction("drive","workshop");game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(parts+2,town.technicalMaterials.get("spare-parts"));
            assertEquals(steel+1,town.technicalMaterials.get("steel"));
        }
    }

    @Test
    void freightCargoAndReservedPayPersistUntilActualDelivery() throws Exception {
        Path file=directory.resolve("freight.db");
        try(var game=new GameSession(file)) {
            // Seed the fixture after the scheduled 09:00 mining step, before 11:00 dispatch.
            game.move("quarry");game.waitMinutes(90);game.applyJob("freight-driver");
            assertThrows(IllegalArgumentException.class,game::work);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.industryStocks.put(IndustryLife.QUARRY_IRON,2);
            town.industryStocks.put(IndustryLife.QUARRY_COPPER,1);
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            int fuel=game.snapshot().fuel().stock();
            game.freightAction("load",null);
            assertEquals(50,game.snapshot().player().money());
            assertEquals(0,game.snapshot().freight().quarryOre());
            assertEquals(2,game.snapshot().freight().iron());
            assertEquals(fuel-2,game.snapshot().fuel().stock());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("drive","factory"));
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
            game.freightAction("drive","workshop");
            assertEquals("WORKSHOP",game.snapshot().freight().truckPlace());
        }
        try(var game=new GameSession(file)) {
            assertEquals("FACTORY",game.snapshot().freight().nextDestination());
            assertEquals(2,game.snapshot().freight().iron());
            game.freightAction("drive","factory");
            assertEquals(50,game.snapshot().player().money());
            game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
            assertFalse(game.snapshot().freight().active());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(2,IndustryLife.stock(town,IndustryLife.FACTORY_IRON));
            assertEquals(1,IndustryLife.stock(town,IndustryLife.FACTORY_COPPER_ORE));
            assertEquals(0,FreightLife.stock(town,FreightLife.WAGES));
        }
    }

    @Test
    void stationFuelConservesStockAndMoneyAndRestarts() throws Exception {
        Path file=directory.resolve("fuel.db");
        try(var game=new GameSession(file)) {
            game.move("fuel_station");game.waitMinutes(60);
            assertEquals(2,game.snapshot().shop().size());
            assertThrows(IllegalArgumentException.class,()->game.buy("milk"));
            game.buy("water");game.applyJob("fuel-attendant");game.work();
            assertEquals(74,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.carAction("refuel"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.carPlace=TownLife.Place.FUEL_STATION;
            town.player.carFuel=4;town.player.carCondition=80;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            int stock=game.snapshot().fuel().stock();
            int cash=game.snapshot().fuel().payroll();
            game.carAction("refuel");
            assertEquals(7,game.snapshot().player().carFuel());
            assertEquals(stock-3,game.snapshot().fuel().stock());
            assertEquals(cash+6,game.snapshot().fuel().payroll());
            assertEquals(68,game.snapshot().player().money());
            game.drive("quarry");
            assertEquals(6,game.snapshot().player().carFuel());
            assertThrows(IllegalArgumentException.class,()->game.carAction("refuel"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertEquals(45,FuelLife.stock(town));
            int funds=town.employerFunds.get(FuelLife.ACCOUNT);
            int supplier=town.employerFunds.get(FuelLife.SUPPLIER);
            FuelLife.tick(town,state.day+1,420);
            assertEquals(48,FuelLife.stock(town));
            assertEquals(funds-3,town.employerFunds.get(FuelLife.ACCOUNT));
            assertEquals(supplier+3,town.employerFunds.get(FuelLife.SUPPLIER));
            FuelLife.tick(town,state.day+1,420);
            assertEquals(funds-3,town.employerFunds.get(FuelLife.ACCOUNT));
        }
    }

    @Test
    void carTripsConserveMoneyFuelAndLocationAcrossRestart() throws Exception {
        Path file=directory.resolve("car.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(60);
            assertThrows(IllegalArgumentException.class,()->game.carAction("buy"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=250;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.carAction("buy");
            assertEquals(70,game.snapshot().player().money());
            game.carAction("refuel");game.carAction("repair");
            assertEquals(49,game.snapshot().player().money());
            assertEquals(9,game.snapshot().player().carFuel());
            assertEquals(100,game.snapshot().player().carCondition());
            assertThrows(IllegalArgumentException.class,()->game.drive("centro"));
            int minute=game.snapshot().minute();
            game.drive("library");
            assertEquals(minute+3,game.snapshot().minute());
            assertEquals("LIBRARY",game.snapshot().player().carPlace());
            assertEquals(8,game.snapshot().player().carFuel());
            assertEquals(97,game.snapshot().player().carCondition());
            game.move("park");
            assertThrows(IllegalArgumentException.class,()->game.drive("workshop"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("LIBRARY",game.snapshot().player().carPlace());
            assertEquals(8,game.snapshot().player().carFuel());
            game.move("library");game.drive("workshop");
            assertEquals("WORKSHOP",game.snapshot().player().carPlace());
            assertEquals(7,game.snapshot().player().carFuel());
        }
    }

    @Test
    void parkedBicycleCannotTeleportAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("parking.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(60);game.buyBicycle();
            game.transportMode("cycle");
            game.move("park");
            game.transportMode("park");
            assertEquals("PARK",game.snapshot().player().bicycleParked());
            assertFalse(game.snapshot().player().cycling());
            game.move("market");
            assertThrows(IllegalArgumentException.class,()->game.transportMode("cycle"));
            assertThrows(IllegalArgumentException.class,()->game.transportMode("retrieve"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("PARK",game.snapshot().player().bicycleParked());
            game.move("park");
            game.transportMode("retrieve");
            assertNull(game.snapshot().player().bicycleParked());
            game.transportMode("cycle");
            assertTrue(game.snapshot().player().cycling());
        }
    }

    @Test
    void bicycleOwnershipAndTravelModePersistAndCostRealMoney() throws Exception {
        Path file=directory.resolve("bicycle.db");
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.transportMode("cycle"));
            assertThrows(IllegalArgumentException.class,game::buyBicycle);
            game.move("workshop");game.waitMinutes(60);
            int before=game.jobs().stream().filter(x->x.id().equals("technician")).findFirst().orElseThrow().remainingPayroll();
            game.buyBicycle();
            assertEquals(5,game.snapshot().player().money());
            assertTrue(game.snapshot().player().ownsBicycle());
            assertEquals(before+45,game.jobs().stream().filter(x->x.id().equals("technician")).findFirst().orElseThrow().remainingPayroll());
            assertThrows(IllegalArgumentException.class,game::buyBicycle);
            assertEquals(10,game.destinations().stream().filter(x->x.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
            game.transportMode("cycle");
            assertEquals(5,game.destinations().stream().filter(x->x.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().player().ownsBicycle());
            assertTrue(game.snapshot().player().cycling());
            int minute=game.snapshot().minute();
            game.move("park");
            assertEquals(minute+5,game.snapshot().minute());
            game.transportMode("walk");
            assertFalse(game.snapshot().player().cycling());
        }
    }

    @Test
    void farmHarvestAndWholesaleAreConservedAndPersistent() throws Exception {
        Path file=directory.resolve("farm.db");
        int barn;
        try(var game=new GameSession(file)) {
            game.move("farm");
            game.waitMinutes(60);
            game.applyJob("farm-worker");
            var before=game.snapshot().farm();
            game.work();
            var after=game.snapshot().farm();
            assertEquals(76,game.snapshot().player().money());
            assertEquals(before.payroll()-26,after.payroll());
            assertEquals(before.vegetablesInField()-4,after.vegetablesInField());
            assertEquals(before.vegetablesInBarn()+4,after.vegetablesInBarn());
            barn=after.vegetablesInBarn();
        }
        try(var game=new GameSession(file)) {
            assertEquals(barn,game.snapshot().farm().vegetablesInBarn());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.retailStock.put("vegetables",0);
            town.retailStock.put("rice",0);
            int money=town.employerFunds.get("shop-assistant")+town.employerFunds.get("farm-worker");
            int stock=FarmLife.stock(town,FarmLife.barn("vegetables"))+FarmLife.stock(town,FarmLife.barn("rice"));
            assertEquals(6,FarmLife.deliver(town));
            assertEquals(stock-6,FarmLife.stock(town,FarmLife.barn("vegetables"))+FarmLife.stock(town,FarmLife.barn("rice")));
            assertEquals(6,town.retailStock.get("vegetables")+town.retailStock.get("rice"));
            assertEquals(money,town.employerFunds.get("shop-assistant")+town.employerFunds.get("farm-worker"));
            RetailLife.tick(town,360);
            assertEquals(6,town.retailStock.get("vegetables")+town.retailStock.get("rice"));
        }
    }

    @Test
    void newIngredientsPersistAndRecipesConsumeExactStock() throws Exception {
        Path file=directory.resolve("recipes.db");
        try(var game=new GameSession(file)) {
            game.move("market");game.waitMinutes(60);
            game.buy("pasta");game.buy("vegetables");game.buy("water");
            assertEquals(44,game.snapshot().player().money());
            assertEquals(1,game.snapshot().player().bag().get("pasta"));
            assertThrows(IllegalArgumentException.class,()->game.consume("pasta"));
            game.move("north");game.rentRoom();
            for(String id:java.util.List.of("pasta","vegetables","water"))
                game.transferRoomItem(id,true);
            game.waitMinutes(120);
            assertThrows(IllegalArgumentException.class,()->game.cookRoomMeal("frittata"));
            assertEquals(1,game.snapshot().player().pantry().get("vegetables"));
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().pantry().get("pasta"));
            int hunger=game.snapshot().player().hunger();
            game.cookRoomMeal("pasta-verdure");
            assertTrue(game.snapshot().player().hunger()<hunger);
            for(String id:java.util.List.of("pasta","vegetables","water"))
                assertEquals(0,game.snapshot().player().pantry().get(id));
        }
        var p=new PlayerLife.Player();
        FoodLife.change(p,"milk",false,12);
        assertEquals(12,FoodLife.total(p,false));
        p.roomUntil=600;FoodLife.change(p,"eggs",true,1);
        HousingLife.expire(p,1,600);
        assertEquals(0,FoodLife.total(p,true));
        assertEquals(12,FoodLife.total(p,false));
    }

    @Test
    void rentedKitchenConservesFoodAndPersistsHygiene() throws Exception {
        Path file=directory.resolve("kitchen.db");
        try(var game=new GameSession(file)) {
            game.move("market");
            game.waitMinutes(60);
            game.buy("bread");game.buy("water");
            assertThrows(IllegalArgumentException.class,()->game.transferRoomItem("bread",true));
            game.move("north");game.rentRoom();
            game.transferRoomItem("bread",true);
            game.transferRoomItem("water",true);
            assertEquals(0,game.snapshot().player().bread());
            assertEquals(1,game.snapshot().player().storedBread());
            game.transferRoomItem("bread",false);
            assertEquals(1,game.snapshot().player().bread());
            game.transferRoomItem("bread",true);
            game.waitMinutes(360);
            assertTrue(game.snapshot().player().hygiene()<95);
            game.washInRoom();
            assertTrue(game.snapshot().player().hygiene()>=99);
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().storedBread());
            assertEquals(1,game.snapshot().player().storedWater());
            assertTrue(game.snapshot().player().hygiene()>=99);
            int hunger=game.snapshot().player().hunger();
            game.cookRoomMeal();
            assertTrue(game.snapshot().player().hunger()<hunger);
            assertEquals(0,game.snapshot().player().storedBread());
            assertEquals(0,game.snapshot().player().storedWater());
            assertThrows(IllegalArgumentException.class,game::cookRoomMeal);
        }
        var player=new PlayerLife.Player();
        player.roomUntil=600;player.storedBread=2;
        HousingLife.expire(player,1,600);
        assertEquals(0,player.storedBread);
    }

    @Test
    void porticiGrocerySharesTheExistingMarketStock() throws Exception {
        Path file=directory.resolve("portici.db");
        try(var game=new GameSession(file)) {
            game.move("portici");
            game.waitMinutes(60);
            assertEquals("PORTICI",game.snapshot().player().placeId());
            int bread=game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock();
            game.buy("bread");
            assertEquals(47,game.snapshot().player().money());
            game.move("market");
            assertEquals(bread-1,game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock());
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().bread());
        }
    }

    @Test
    void historicCentreIsPublicAndParkRemainsFirstSpawn() throws Exception {
        Path file=directory.resolve("centro.db");
        try(var game=new GameSession(file)) {
            assertEquals("PARK",game.snapshot().player().placeId());
            assertTrue(game.destinations().stream().anyMatch(x->x.id().equals("CENTRO")));
            assertEquals("CENTRO",game.move("centro").world().player().placeId());
        }
        try(var game=new GameSession(file)) {
            assertEquals("CENTRO",game.snapshot().player().placeId());
            assertEquals("MARKET",game.move("market").world().player().placeId());
        }
    }

    @Test
    void prepaidRoomIsLocalAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("room.db");
        long expiry;
        try(var game=new GameSession(file)) {
            assertFalse(game.snapshot().player().roomActive());
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            game.move("north");
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(1));
            game.rentRoom();
            assertEquals(15,game.snapshot().player().money());
            assertTrue(game.snapshot().player().roomActive());
            expiry=game.snapshot().player().roomUntil();
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            int health=game.snapshot().player().health();
            game.sleepInRoom(2);
            assertTrue(game.snapshot().player().health()>=health);
        }
        try(var game=new GameSession(file)) {
            assertEquals(expiry,game.snapshot().player().roomUntil());
            game.move("park");
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(1));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            assertEquals(35,town.employerFunds.get("guesthouse"));
            town.player.place=HousingLife.DISTRICT;
            town.player.roomUntil=HousingLife.now(state.day,state.minute)+30;
            assertThrows(IllegalArgumentException.class,()->HousingLife.sleep(town.player,1,state.day,state.minute));
            assertFalse(HousingLife.active(town.player,state.day+1,state.minute));
        }
    }

    @Test
    void marketPurchaseConservesCashAndSuppliesAcrossRestart() throws Exception {
        Path file=directory.resolve("retail.db");
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.buy("bread"));
            game.move("market");
            game.waitMinutes(60);
            int stock=game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock();
            int cash=game.jobs().stream().filter(x->x.id().equals("shop-assistant")).findFirst().orElseThrow().remainingPayroll();
            game.buy("bread");
            assertEquals(47,game.snapshot().player().money());
            assertEquals(1,game.snapshot().player().bread());
            assertEquals(stock-1,game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock());
            assertEquals(cash+3,game.jobs().stream().filter(x->x.id().equals("shop-assistant")).findFirst().orElseThrow().remainingPayroll());
            game.buy("water");
            assertEquals(46,game.snapshot().player().money());
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().bread());
            assertEquals(1,game.snapshot().player().bottledWater());
            game.move("park");
            game.waitMinutes(120);
            int hunger=game.snapshot().player().hunger();
            game.consume("bread");
            assertEquals(0,game.snapshot().player().bread());
            assertTrue(game.snapshot().player().hunger()<hunger);
            assertThrows(IllegalArgumentException.class,()->game.consume("bread"));
            game.consume("water");
            assertEquals(0,game.snapshot().player().bottledWater());
        }
    }

    @Test
    void conversationIsLocalAndMemorySurvivesRestart() throws Exception {
        Path file=directory.resolve("conversation.db");
        try(var game=new GameSession(file)) { }
        String id;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var resident=town.people.values().iterator().next();
            id=resident.id;
            resident.place=TownLife.Place.PARK;
            resident.travelRemaining=0;
            resident.activity="FREE_TIME";
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.speak("missing","Hello"));
            int before=game.snapshot().minute();
            assertTrue(game.speak(id,"My name is Alex.").message().contains("Alex"));
            assertEquals((before+1)%1440,game.snapshot().minute());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals("Alex",town.player.contacts.get(id).knownName);
            assertEquals(1,town.player.contacts.get(id).conversations);
            assertEquals(53,town.player.contacts.get(id).trust);
            assertTrue(town.player.contacts.get(id).affinity>0);
        }
        try(var game=new GameSession(file)) {
            game.move("quarry");
            assertThrows(IllegalArgumentException.class,()->game.speak(id,"Hello"));
        }
    }

    @Test
    void publicVisitorNewsUsesActualSocialEncountersAndPersists() throws Exception {
        Path file=directory.resolve("visitor-news.db");
        try(var game=new GameSession(file)) { }
        String key;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var speaker=town.get("marco");
            var listener=town.get("elena");
            speaker.place=TownLife.Place.PARK;
            listener.place=TownLife.Place.CAFE;
            speaker.activity=listener.activity="FREE_TIME";
            speaker.travelRemaining=listener.travelRemaining=0;
            speaker.sociability=80;
            town.relationship.trust=70;
            PlayerLife.setPrivateName(town.player,"Private identity");
            PlayerLife.speak(town,speaker.id,"My name is Alex.",state.day,state.minute);
            assertFalse(town.beliefs.getOrDefault(speaker.id,new java.util.LinkedHashMap<>())
                    .values().stream().anyMatch(b->b.actorId().startsWith("visitor-")));
            PlayerLife.speak(town,speaker.id,"You can tell people my name.",state.day,state.minute);
            var claim=town.beliefs.get(speaker.id).values().stream()
                    .filter(b->b.actorId().startsWith("visitor-")).findFirst().orElseThrow();
            key=claim.eventKey();
            assertFalse(claim.statement().contains("Private identity"));
            assertTrue(claim.statement().contains("Alex"));
            KnowledgeLife.exchange(town,state,speaker.id,listener.id);
            assertFalse(town.beliefs.getOrDefault(listener.id,new java.util.LinkedHashMap<>()).containsKey(key));
            listener.place=TownLife.Place.PARK;
            KnowledgeLife.exchange(town,state,speaker.id,listener.id);
            var heard=town.beliefs.get(listener.id).get(key);
            assertEquals("HEARD",heard.origin());
            assertEquals(speaker.id,heard.learnedFrom());
            assertEquals(1,heard.hops());
            assertTrue(heard.confidence()<claim.confidence());
            assertFalse(town.player.contacts.containsKey(listener.id));
            assertTrue(KnowledgeLife.visitorNews(town,listener,state.day,state.minute).contains("Alex"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            assertEquals("HEARD",town.beliefs.get("elena").get(key).origin());
            assertTrue(KnowledgeLife.visitorNews(town,town.get("elena"),state.day,state.minute).contains("can't verify"));
        }
    }

    @Test
    void socialAttitudeIsPersonalAndCannotBeFarmedInOneDay() throws Exception {
        Path file=directory.resolve("attitude.db");
        try(var game=new GameSession(file)) { }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var resident=town.people.values().iterator().next();
            resident.place=TownLife.Place.PARK;
            resident.travelRemaining=0;
            resident.activity="FREE_TIME";
            PlayerLife.speak(town,resident.id,"Hello",1,450);
            var contact=town.player.contacts.get(resident.id);
            int initialAffinity=contact.affinity;
            for(int i=0;i<20;i++)PlayerLife.speak(town,resident.id,"Hello",1,450+i);
            assertEquals(initialAffinity,contact.affinity);
            for(int day=2;day<=12;day++)PlayerLife.speak(town,resident.id,"Hello",day,450);
            assertEquals("FRIENDLY",PlayerLife.relationship(contact));
            assertEquals("STRANGER",PlayerLife.relationship(null));
            for(int i=0;i<8;i++)PlayerLife.speak(town,resident.id,"Leave me alone!",12,460+i);
            assertEquals("WARY",PlayerLife.relationship(contact));
            assertEquals(1,town.player.contacts.size());
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            var contact=town.player.contacts.values().iterator().next();
            assertEquals("WARY",PlayerLife.relationship(contact));
            assertEquals(12,contact.lastPositiveDay);
        }
    }

    @Test
    void businessMachineServiceConsumesRealCityPartsAndSurvivesServerRestart() throws Exception {
        Path file=directory.resolve("worn-machine.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");
            game.openBusiness();
            game.investBusiness(25);
            game.buyBusinessMachine();
            assertEquals(100,game.business().machineCondition());
            assertThrows(IllegalArgumentException.class,game::serviceBusinessMachine,
                    "machine in perfect condition should not waste repair parts");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.business.machineCondition=40;
            town.business.cash=30;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(40,game.business().machineCondition());
            int initialCityParts=6;
            var repaired=game.serviceBusinessMachine();
            assertTrue(repaired.message().contains("100/100"));
            assertEquals(100,game.business().machineCondition());
            assertEquals(20,game.business().cash());
            // Repaired equipment is a material-consuming transaction.
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                assertEquals(initialCityParts-2,town.technicalMaterials.get("spare-parts"));
            }
        }
        try(var game=new GameSession(file)) {
            assertEquals(100,game.business().machineCondition());
            assertEquals(20,game.business().cash());
            assertThrows(IllegalArgumentException.class,game::serviceBusinessMachine);
        }
    }

    @Test
    void founderCanOpenCapitalFundedWorkshopWithoutCreatingMoney() throws Exception {
        Path file=directory.resolve("first-workshop.db");
        try(var game=new GameSession(file)) {
            assertFalse(game.business().registered());
            assertThrows(IllegalArgumentException.class,game::openBusiness,
                    "you cannot register a distant company by clicking from the park");
            game.move("workshop");
            var opened=game.openBusiness();
            assertTrue(opened.message().contains("Registered"));
            assertTrue(game.business().registered());
            assertTrue(game.business().yours());
            assertEquals(35,game.snapshot().player().money());
            game.investBusiness(25);
            assertEquals(25,game.business().cash());
            assertEquals(10,game.snapshot().player().money());
            game.buyBusinessMachine();
            assertTrue(game.business().machine());
            assertEquals(0,game.business().cash());
            assertThrows(IllegalArgumentException.class,
                    ()->game.hireBusinessWorker("resident-0001"),
                    "a company without enough money for wages cannot recruit people");
            assertThrows(IllegalArgumentException.class,
                    ()->game.withdrawBusiness(1));
            assertThrows(IllegalArgumentException.class,
                    ()->game.investBusiness(100));
        }
        try(var reopened=new GameSession(file)) {
            assertTrue(reopened.business().registered());
            assertTrue(reopened.business().machine());
            assertTrue(reopened.business().yours());
            assertEquals(0,reopened.business().cash());
            assertEquals(10,reopened.snapshot().player().money());
            assertTrue(reopened.businessCandidates().stream()
                    .allMatch(c->c.id().startsWith("resident-")));
        }
    }

    @Test
    void ordinaryVisitorCanBecomeMinerOrMachinistAndSurveyNewOre() throws Exception {
        Path mine=directory.resolve("visitor-miner.db");
        try(var game=new GameSession(mine)) {
            assertEquals(9,game.jobs().size());
            game.move("quarry");
            game.applyJob("miner");
            game.waitMinutes(30);
            var paid=game.work();
            assertEquals("miner",paid.world().player().jobId());
            assertEquals(80,paid.world().player().money(),
                    "the player earns real company cash and actually mines");
            assertTrue(game.industry().ironRemaining()<80);
            int beforeSurveyIron=game.industry().ironRemaining();
            var surveyed=game.prospect();
            assertTrue(surveyed.message().contains("Western"));
            assertTrue(game.industry().westDepositDiscovered());
            assertEquals(beforeSurveyIron+30,game.industry().ironRemaining(),
                    "geological survey reveals thirty additional finite iron units");
            assertThrows(IllegalArgumentException.class,game::prospect);
        }
        try(var restarted=new GameSession(mine)) {
            assertEquals("miner",restarted.snapshot().player().jobId());
            assertEquals(80,restarted.snapshot().player().money());
            assertTrue(restarted.industry().westDepositDiscovered(),
                    "geology and company finances survive server restarts");
        }
        Path factory=directory.resolve("visitor-machinist.db");
        try(var game=new GameSession(factory)) {
            game.move("factory");
            game.applyJob("machinist");
            game.waitMinutes(30);
            var worked=game.work();
            assertEquals("machinist",worked.world().player().jobId());
            assertEquals(84,worked.world().player().money());
            assertTrue(game.industry().scrapRemaining()<10,
                    "machinist genuinely converts stored waste into steel");
        }
    }

    @Test
    void freshWorldHasFiniteEnterprisesAndTheirStoresPersist() throws Exception {
        Path file=directory.resolve("factory-world.db");
        try(var game=new GameSession(file)) {
            assertEquals(80,game.industry().ironRemaining());
            assertEquals(40,game.industry().copperRemaining());
            assertEquals(6,game.industry().workshopParts());
            assertEquals(4,game.industry().workshopSteel());
            assertEquals(3,game.industry().workshopCopper());
            assertTrue(game.destinations().stream().anyMatch(d->d.id().equals("QUARRY")));
            assertTrue(game.destinations().stream().anyMatch(d->d.id().equals("FACTORY")));
            // Existing player freedoms still work; viewing a stock report
            // never grants free components or changes the world.
            assertEquals(80,game.industry().ironRemaining());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.industryStocks.put("seam:iron-ore",77);
            town.industryStocks.put("factory:spare-parts",2);
            db.save(state,java.util.List.of(),java.util.List.of(),
                    town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(77,game.industry().ironRemaining());
            assertEquals(2,game.industry().factoryParts());
            assertEquals(6,game.industry().workshopParts(),
                    "unfinished factory parts are not yet delivered to repairs");
        }
    }

    @Test
    void visitorChoosesCareerAndKeepsItAcrossRestart() throws Exception {
        Path file=directory.resolve("career-world.db");
        try(var game=new GameSession(file)) {
            var visitor=game.snapshot().player();
            assertEquals("Park",visitor.place());
            assertEquals("PARK",visitor.placeId());
            assertEquals(0,game.destinations().stream()
                    .filter(d->d.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
            assertEquals(10,game.destinations().stream()
                    .filter(d->d.id().equals("MARKET")).findFirst().orElseThrow().travelMinutes());
            assertEquals(50,visitor.money());
            assertNull(visitor.jobId());
            assertEquals(0,visitor.engineeringSkill());
            assertEquals(9,game.jobs().size());

            assertThrows(IllegalArgumentException.class,
                    ()->game.applyJob("shop-assistant"));
            assertEquals(50,game.snapshot().player().money());
            game.move("market");
            assertEquals("MARKET",game.snapshot().player().placeId());
            assertEquals(8,game.destinations().stream()
                    .filter(d->d.id().equals("SCHOOL")).findFirst().orElseThrow().travelMinutes());
            game.applyJob("shop-assistant");
            assertEquals("shop-assistant",game.snapshot().player().jobId());
            game.waitMinutes(5);
            var shift=game.work();
            assertEquals(74,shift.world().player().money());
            assertThrows(IllegalArgumentException.class,game::work);
            game.move("school");
            var lesson=game.study("engineering");
            assertEquals(64,lesson.world().player().money());
            assertEquals(1,lesson.world().player().engineeringSkill());
            assertEquals(456,game.jobs().stream()
                    .filter(j->j.id().equals("shop-assistant"))
                    .findFirst().orElseThrow().remainingPayroll());
        }
        try(var restarted=new GameSession(file)) {
            assertEquals(64,restarted.snapshot().player().money());
            assertEquals("SCHOOL",restarted.snapshot().player().placeId());
            assertEquals("shop-assistant",restarted.snapshot().player().jobId());
            assertEquals(1,restarted.snapshot().player().engineeringSkill());
            assertEquals(9,restarted.jobs().size(),"job board remains stable");
        }
    }

    @Test
    void parkFountainTransfersLimitedWaterAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("public-utilities.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.technicalOrders.get("water-pump").condition=100;
            town.cleanWater=3;
            db.save(state,java.util.List.of(),java.util.List.of(),
                    town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().services().fountainWorking());
            assertEquals(3,game.services().cleanWater());
            assertEquals(50,game.snapshot().services().lightingCondition());
            var result=game.drink("park-fountain");
            assertEquals(2,result.world().services().cleanWater());
            assertEquals(0,result.world().player().thirst());
        }
        try(var reopened=new GameSession(file)) {
            assertEquals(2,reopened.snapshot().services().cleanWater());
            reopened.drink("park-fountain");
            reopened.drink("park-fountain");
            assertEquals(0,reopened.snapshot().services().cleanWater());
            assertThrows(IllegalArgumentException.class,
                    ()->reopened.drink("park-fountain"));
            assertEquals(0,reopened.snapshot().services().cleanWater());
        }
    }

    @Test
    void paidTechnicalShiftRequiresRealRepairAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("technical-game.db");
        try(var game=new GameSession(file)) {
            assertEquals("OPEN",game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow().phase());
            assertFalse(game.services().fountainWorking());
            assertFalse(game.services().lightsWorking());
            game.move("school");
            game.waitMinutes(20); // Day 1 08:00
            for(int i=0;i<3;i++)game.study("engineering");
            game.move("workshop");
            game.applyJob("technician");
            assertThrows(IllegalArgumentException.class,game::work);
            game.diagnose("water-pump");
            var task=game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow();
            assertEquals("INSPECTED",task.phase());
            var finished=game.repair("water-pump");
            assertTrue(finished.message().contains("45 -> 100"));
            assertTrue(game.snapshot().services().fountainWorking());
            assertEquals(6,game.services().cleanWater());
            assertEquals(100,game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow().condition());
            assertEquals(20,game.snapshot().player().money());
            var paid=game.work();
            assertEquals(58,paid.world().player().money());
            assertThrows(IllegalArgumentException.class,()->game.repair("water-pump"));
        }
        try(var reopened=new GameSession(file)) {
            assertEquals(58,reopened.snapshot().player().money());
            var task=reopened.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow();
            assertEquals(100,task.condition());
            assertEquals("COMPLETED",task.phase());
            assertThrows(IllegalArgumentException.class,reopened::work);
        }
    }

    @Test
    void newWorldActionsAndRestart() throws Exception {
        Path file=directory.resolve("fresh-world.db");
        try(var game=new GameSession(file)) {
            var initial=game.snapshot();
            assertEquals(1,initial.day());
            assertEquals(450,initial.minute());
            assertEquals("SPRING",initial.season());
            assertEquals(1,initial.year());
            assertNotNull(initial.weather());
            assertEquals(WeatherLife.at(1,450).sky().name(),initial.weather().sky());
            assertEquals(WeatherLife.at(1,450).temperatureC(),
                    initial.weather().temperatureC());
            assertEquals(1,initial.player().incarnation());
            assertTrue(initial.player().alive());
            assertNull(initial.player().privateName());
            assertEquals("Park",initial.player().place());
            assertTrue(initial.sources().stream()
                    .anyMatch(s->s.id().equals("puddle-water")));
            var walk=game.move("river");
            assertEquals("River",walk.world().player().place());
            assertEquals(460,walk.world().minute());
            assertEquals(120,walk.world().sources().getFirst().stock());
            var drink=game.drink("river-water");
            assertEquals(119,drink.world().sources().getFirst().stock());
            var waited=game.waitMinutes(60);
            assertEquals(525,waited.world().minute());
            assertThrows(IllegalArgumentException.class,()->game.waitMinutes(1441));
            assertEquals(525,game.snapshot().minute());
        }
        // The brand-new city should start populated rather than requiring debug commands.
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(14,town.people.size(),"12 generated NPCs + Marco + Elena");
        }
        try(var reloaded=new GameSession(file)) {
            assertEquals(525,reloaded.snapshot().minute());
            assertEquals("River",reloaded.snapshot().player().place());
            assertEquals(119,reloaded.snapshot().sources().getFirst().stock());
            var next=reloaded.move("woods");
            assertEquals("Woods",next.world().player().place());
            assertTrue(next.world().sources().stream()
                    .anyMatch(s->s.id().equals("wild-berries")));
        }
    }
}
