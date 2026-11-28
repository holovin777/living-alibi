package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class GeoDriveTest {
    @TempDir Path dir;
    @Test void accelerationCoastBrakeAndPauseAreBounded(){
        var drive=new GeoDrive(false,"1");
        assertEquals(0.6,drive.controlBudget("step",Math.sqrt(0.4)),.001);
        assertTrue(drive.speed>0);
        for(int i=0;i<20;i++)drive.controlBudget("step",0.65);
        assertEquals(GeoDrive.MAX_SPEED,drive.speed,.001);
        double before=drive.speed;
        drive.controlBudget("coast",0.65);
        assertTrue(drive.speed<before);
        drive.controlBudget("brake",0.8);
        drive.controlBudget("brake",0.8);
        assertEquals(0,drive.speed,.001);
        assertEquals(0,drive.controlBudget("coast",0.65),.001);
        assertTrue(drive.controlBudget("step",60)<1);
        assertEquals(drive.speed,drive.copy().speed,.001);
    }
    @Test void roadSpeedTargetsAreBounded()throws Exception{
        var graph=dir.resolve("speeds.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\nM\t1\t0\t0\nM\t2\t100\t0\nD\t1\t2\t72\nD\t2\t1\t20\n");
        var map=new GeoProjection(graph);
        assertEquals(72,map.speedLimit("1","2"));
        assertEquals(20,map.speedLimit("2","1"));
        var drive=new GeoDrive(false,"1");
        for(int i=0;i<20;i++)drive.controlBudget("step",0.65,map.speedLimit("1","2")/3.6);
        assertEquals(20,drive.speed,.001);
        drive.controlBudget("step",0.65,map.speedLimit("2","1")/3.6);
        assertEquals(20/3.6,drive.speed,.001);
    }
    @Test void signalStopsCarBeforeJunction()throws Exception{
        var graph=dir.resolve("signal.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\nM\t1\t0\t0\nM\t2\t100\t0\nD\t1\t2\nS\t2\t0\n");
        var map=new GeoProjection(graph);
        var drive=new GeoDrive(false,"1");
        assertEquals(97,drive.advance(map,0,100,0),.001);
        assertTrue(drive.waitingForSignal);
        assertEquals(0,drive.advance(map,0,5,0),.001);
        assertEquals(3,drive.advance(map,0,5,17),.001);
        assertFalse(drive.waitingForSignal);
    }
    @Test void ownedTwoWheelersHaveSeparateWalletParkingAndCondition(){
        var t=TownLife.create(new WorldState());
        t.player.place=TownLife.Place.WORKSHOP;t.player.destination=t.player.place;t.player.money=300;
        int dealer=t.employerFunds.getOrDefault("technician",0);
        LightMotorLife.action(t,"scooter","buy",600);
        LightMotorLife.action(t,"motorcycle","buy",600);
        assertEquals(85,t.player.money);
        assertEquals(dealer+215,t.employerFunds.get("technician").intValue());
        assertEquals(3,LightMotorLife.get(t,"scooter","fuel"));
        assertEquals(4,LightMotorLife.get(t,"motorcycle","fuel"));
        LightMotorLife.park(t,"motorcycle",TownLife.Place.PARK);
        assertThrows(IllegalArgumentException.class,()->LightMotorLife.action(t,"motorcycle","sell",600));
        LightMotorLife.action(t,"scooter","sell",600);
        assertEquals(115,t.player.money);
        assertNull(LightMotorLife.parked(t,"scooter"));
        assertEquals(TownLife.Place.PARK,LightMotorLife.parked(t,"motorcycle"));
        assertEquals(0,JusticeLife.thefts(t));
    }
    @Test void lightVehicleLimitsAndCustody(){
        var s=new WorldState();var t=TownLife.create(s);
        t.player.place=TownLife.Place.MARKET;t.player.destination=t.player.place;
        VehicleTheftLife.takeLight(t,s,"scooter",new ArrayList<>(),new ArrayList<>());
        assertEquals("scooter",VehicleTheftLife.lightKind(t));
        assertEquals(6,VehicleTheftLife.get(t,"fuel"));
        VehicleTheftLife.set(t,"fuel",2);
        VehicleTheftLife.recover(t);
        assertEquals(2,VehicleTheftLife.reserveFuel(t,"scooter"));
        assertEquals(9,VehicleTheftLife.reserveFuel(t,"motorcycle"));
        var scooter=new GeoDrive(true,false,false,"scooter","1");
        var motorcycle=new GeoDrive(true,false,false,"motorcycle","1");
        assertEquals(45,scooter.vehicleLimit(72),.001);
        assertEquals(72,motorcycle.vehicleLimit(72),.001);
        scooter.controlBudget("step",.5,20);motorcycle.controlBudget("step",.5,20);
        assertTrue(motorcycle.speed>scooter.speed);
        assertEquals("motorcycle",motorcycle.copy().lightKind);
    }
    @Test void reserveMaintenanceConsumesFundsAndSuppliesOnlyOncePerDay(){
        var s=new WorldState();var t=TownLife.create(s);
        s.day=2;s.minute=480;
        t.industryStocks.put("reserve:bus:fuel",2);
        t.industryStocks.put("reserve:bus:condition",40);
        t.industryStocks.put(FuelLife.STOCK,4);
        t.technicalMaterials.put("spare-parts",1);
        t.employerFunds.put("city-maintenance",30);
        int garage=t.employerFunds.getOrDefault("technician",0);
        int station=t.employerFunds.getOrDefault(FuelLife.ACCOUNT,0);
        ReserveFleetLife.tick(t,s);
        assertEquals(70,VehicleTheftLife.reserveCondition(t,"bus"));
        assertEquals(6,VehicleTheftLife.reserveFuel(t,"bus"));
        assertEquals(0,FuelLife.stock(t));
        assertEquals(0,t.technicalMaterials.get("spare-parts").intValue());
        assertEquals(7,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(garage+15,t.employerFunds.get("technician").intValue());
        assertEquals(station+8,t.employerFunds.get(FuelLife.ACCOUNT).intValue());
        t.industryStocks.put(FuelLife.STOCK,3);
        ReserveFleetLife.tick(t,s);
        assertEquals(6,VehicleTheftLife.reserveFuel(t,"bus"));
        s.day++;
        t.employerFunds.put("city-maintenance",0);
        ReserveFleetLife.tick(t,s);
        assertEquals(6,VehicleTheftLife.reserveFuel(t,"bus"));
        assertEquals(3,FuelLife.stock(t));
    }
    @Test void truckHasIndependentPersistentReserveCondition(){
        var t=TownLife.create(new WorldState());
        t.player.place=TownLife.Place.WORKSHOP;t.player.destination=t.player.place;
        VehicleTheftLife.takeTruck(t,new WorldState(),new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.truck(t));
        assertFalse(VehicleTheftLife.bus(t));
        VehicleTheftLife.set(t,"fuel",9);VehicleTheftLife.set(t,"condition",70);
        VehicleTheftLife.recover(t);
        assertEquals(9,VehicleTheftLife.reserveFuel(t,"truck"));
        assertEquals(70,VehicleTheftLife.reserveCondition(t,"truck"));
        assertEquals(12,VehicleTheftLife.reserveFuel(t,"bus"));
        var drive=new GeoDrive(true,false,true,"1");
        drive.controlBudget("step",0.5,20);
        assertEquals(0.6,drive.speed,.001);
        assertEquals(60,drive.vehicleLimit(72),.001);
        assertTrue(drive.copy().truck);
    }
    @Test void reserveBusCustodyAndRecoveryAreSeparate()throws Exception{
        var s=new WorldState();var t=TownLife.create(s);
        t.player.place=TownLife.Place.STAZIONE;t.player.destination=t.player.place;
        t.player.carPlace=TownLife.Place.WORKSHOP;t.player.carFuel=4;t.player.carCondition=80;
        VehicleTheftLife.takeBus(t,s,new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.bus(t));
        assertEquals(1,JusticeLife.thefts(t));
        assertThrows(IllegalArgumentException.class,()->VehicleTheftLife.takeBus(t,s,new ArrayList<>(),new ArrayList<>()));
        var drive=new GeoDrive(true,true,"1");
        drive.controlBudget("step",0.65,20);
        assertEquals(1.04,drive.speed,.001);
        assertTrue(drive.copy().bus);
        for(int i=0;i<30;i++)drive.controlBudget("step",0.65,20);
        assertEquals(15,drive.speed,.001);
        VehicleTheftLife.recover(t);
        assertFalse(VehicleTheftLife.bus(t));
        assertFalse(VehicleTheftLife.active(t));
        assertEquals(4,t.player.carFuel);
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
    }
    @Test void directedRoadAndTheftAreSeparateFromLegalCar()throws Exception{
        var graph=dir.resolve("graph.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\nM\t1\t0\t0\nM\t2\t100\t0\nD\t1\t2\n");
        var map=new GeoProjection(graph);
        assertEquals(36,map.speedLimit("1","2"));
        var drive=new GeoDrive(false,"1");
        assertEquals(8,drive.advance(map,0,8),.01);
        assertEquals(8,drive.position(map).x(),.01);
        drive.advance(map,0,200);
        assertEquals(100,drive.position(map).x(),.01);
        assertEquals(0,drive.advance(map,0,10),.01);
        var s=new WorldState();var t=TownLife.create(s);
        t.player.carPlace=TownLife.Place.WORKSHOP;t.player.carFuel=4;t.player.carCondition=80;
        var owner=new TownLife.Citizen("resident-test","Test Resident","Mechanic",t.player.place);
        owner.generationIndex=3;t.people.put(owner.id,owner);
        VehicleTheftLife.take(t,s,owner,new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.missing(t,owner));
        assertEquals(3,VehicleTheftLife.get(t,"owner"));
        assertTrue(VehicleTheftLife.active(t));
        assertEquals(1,JusticeLife.thefts(t));
        assertEquals(4,t.player.carFuel);
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
        s.minute+=61;
        JusticeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertTrue(JusticeLife.confined(t));assertFalse(VehicleTheftLife.active(t));
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
    }
}
