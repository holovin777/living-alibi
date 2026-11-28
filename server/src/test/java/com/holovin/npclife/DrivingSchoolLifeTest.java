package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DrivingSchoolLifeTest {
    @Test void heavyCategoriesRequireBAndAreSeparate(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        t.player.place=TownLife.Place.SCHOOL;t.player.destination=t.player.place;t.player.money=100;
        assertThrows(IllegalArgumentException.class,()->DrivingSchoolLife.action(t,s,"C","lesson"));
        DrivingSchoolLife.set(t,"B","issued",1);
        DrivingSchoolLife.action(t,s,"C","lesson");
        assertEquals(1,DrivingSchoolLife.get(t,"C","theory"));
        assertFalse(DrivingSchoolLife.allowed(t,"truck"));
        assertFalse(DrivingSchoolLife.allowed(t,"bus"));
        DrivingSchoolLife.set(t,"C","issued",1);
        assertTrue(DrivingSchoolLife.allowed(t,"truck"));
        assertFalse(DrivingSchoolLife.allowed(t,"bus"));
        DrivingSchoolLife.set(t,"D","issued",1);
        assertTrue(DrivingSchoolLife.allowed(t,"bus"));
    }
    @Test void registrationNumbersAreStableAndSeparate(){
        var t=TownLife.create(new WorldState());
        String car=VehicleRegistrationLife.personal(t,"car");
        assertEquals(car,VehicleRegistrationLife.personal(t.copy(),"car"));
        assertNotEquals(car,VehicleRegistrationLife.personal(t,"scooter"));
        assertNotEquals(car,VehicleRegistrationLife.resident(t.player.incarnation));
        assertNotEquals(VehicleRegistrationLife.reserve("municipal-reserve-bus"),VehicleRegistrationLife.reserve("freight-reserve-truck"));
        t.player.incarnation++;
        assertNotEquals(car,VehicleRegistrationLife.personal(t,"car"));
    }
    @Test void theoryPracticeIssueAndIncarnationScope(){
        var s=new WorldState();s.day=2;s.minute=600;
        var t=TownLife.create(s);t.player.money=100;
        t.player.place=TownLife.Place.SCHOOL;t.player.destination=t.player.place;
        assertThrows(IllegalArgumentException.class,()->DrivingSchoolLife.action(t,s,"B","issue"));
        for(int i=0;i<2;i++)DrivingSchoolLife.action(t,s,"B","lesson");
        for(int i=0;i<3;i++)DrivingSchoolLife.action(t,s,"B","answer0");
        assertEquals(0,DrivingSchoolLife.get(t,"B","passed"));
        assertEquals(0,DrivingSchoolLife.get(t,"B","question"));
        for(int i:new int[]{1,0,2})DrivingSchoolLife.action(t,s,"B","answer"+i);
        assertEquals(1,DrivingSchoolLife.get(t,"B","passed"));
        t.player.place=TownLife.Place.WORKSHOP;t.player.destination=t.player.place;
        for(int i=0;i<2;i++)DrivingSchoolLife.action(t,s,"B","practice");
        assertFalse(DrivingSchoolLife.allowed(t,""));
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
        DrivingSchoolLife.action(t,s,"B","issue");
        assertEquals(70,t.player.money);
        assertTrue(DrivingSchoolLife.allowed(t,""));
        assertTrue(DrivingSchoolLife.allowed(t,"scooter"));
        assertFalse(DrivingSchoolLife.allowed(t,"motorcycle"));
        assertThrows(IllegalArgumentException.class,()->DrivingSchoolLife.action(t,s,"B","issue"));
        t.player.incarnation++;
        assertFalse(DrivingSchoolLife.valid(t,"B"));
    }
}
