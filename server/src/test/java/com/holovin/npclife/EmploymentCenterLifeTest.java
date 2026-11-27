package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EmploymentCenterLifeTest {
    @TempDir Path folder;
    @Test void registrationTrainingAndBudgetPersist() throws Exception{
        var file=folder.resolve("job-centre.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);var p=t.player;
            s.day=2;s.minute=540;p.money=5;p.jobId=null;
            p.place=TownLife.Place.PARK;p.destination=p.place;
            assertFalse(EmploymentCenterLife.canRegister(t,s));
            p.place=TownLife.Place.OFFICE;p.destination=p.place;
            assertTrue(EmploymentCenterLife.canRegister(t,s));
            EmploymentCenterLife.register(t,s);
            assertFalse(EmploymentCenterLife.canRegister(t,s));
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,30);
            int school=t.employerFunds.getOrDefault("school",0),skill=p.engineeringSkill;
            assertThrows(IllegalArgumentException.class,()->EmploymentCenterLife.study(t,s,"engineering"));
            assertEquals(5,p.money);
            assertEquals(30,t.employerFunds.get(SocialProtectionLife.ACCOUNT));
            p.place=TownLife.Place.SCHOOL;p.destination=p.place;
            for(int i=0;i<3;i++){
                assertTrue(EmploymentCenterLife.fundedLesson(t,s));
                EmploymentCenterLife.study(t,s,"engineering");
                assertFalse(EmploymentCenterLife.fundedLesson(t,s));
                s.day++;
            }
            assertEquals(skill+3,p.engineeringSkill);
            assertEquals(5,p.money);
            assertEquals(school+30,t.employerFunds.get("school"));
            assertEquals(0,t.employerFunds.get(SocialProtectionLife.ACCOUNT));
            assertEquals(0,EmploymentCenterLife.report(t,s).lessonsRemaining());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertTrue(EmploymentCenterLife.registered(t));
            assertEquals(0,EmploymentCenterLife.report(t,s).lessonsRemaining());
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,100);
            assertFalse(EmploymentCenterLife.fundedLesson(t,s));
        }
    }
}
