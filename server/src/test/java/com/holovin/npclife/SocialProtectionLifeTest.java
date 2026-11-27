package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SocialProtectionLifeTest {
    @TempDir Path folder;
    @Test void supportUsesFiniteFundsAndSurvivesReload() throws Exception{
        var file=folder.resolve("social.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            var jobless=new TownLife.Citizen("aid-jobless","Test Worker","Mechanic",TownLife.Place.WORKSHOP);
            var ill=new TownLife.Citizen("aid-sick","Test Sick","Office clerk",TownLife.Place.OFFICE);
            jobless.generationIndex=9001;ill.generationIndex=9002;
            for(var c:List.of(jobless,ill)){c.age=35;c.money=20;c.alive=true;c.employed=true;t.people.put(c.id,c);}
            s.day=2;s.minute=390;
            EmploymentLife.loseJob(t,s,jobless);
            ill.sickUntilDay=4;
            t.employerFunds.put(MunicipalLife.CITY,MunicipalLife.RESERVE+25);
            SocialProtectionLife.fund(t);
            assertEquals(25,SocialProtectionLife.report(t,s.day).fund());
            assertEquals(MunicipalLife.RESERVE,MunicipalLife.balance(t,MunicipalLife.CITY));
            SocialProtectionLife.tick(t,s);
            assertEquals(30,ill.money);assertEquals(20,jobless.money);
            SocialProtectionLife.tick(t,s);
            assertEquals(30,ill.money);
            s.day=4;SocialProtectionLife.tick(t,s);
            assertEquals(40,ill.money);assertEquals(25,jobless.money);
            assertEquals(0,SocialProtectionLife.report(t,s.day).fund());
            assertEquals(20,SocialProtectionLife.report(t,s.day).sicknessPaid());
            assertEquals(5,SocialProtectionLife.report(t,s.day).unemploymentPaid());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(5,SocialProtectionLife.report(t,s.day).unemploymentPaid());
            assertEquals(20,SocialProtectionLife.report(t,s.day).sicknessPaid());
            var c=t.get("aid-jobless");
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,30);
            EmploymentLife.hire(t,s,c,2);
            s.day=5;s.minute=390;SocialProtectionLife.tick(t,s);
            assertEquals(25,c.money);
            assertEquals(30,SocialProtectionLife.report(t,s.day).fund());
        }
    }
}
