package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CondominiumLifeTest {
    @TempDir Path folder;
    @Test void contributionIsPartOfHousingBillAndPersists() throws Exception{
        Path file=folder.resolve("condo.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            var c=t.get("marco");var other=t.get("elena");
            c.generationIndex=1000001;c.age=35;c.alive=true;c.money=200;
            other.generationIndex=1000002;other.age=35;other.alive=true;
            c.home=TownLife.Place.MARCO_HOME;
            other.home=c.home;other.homeUnit=c.homeUnit;
            s.day=2;s.minute=480;
            int share=CondominiumLife.allocate(t,s,c,155);
            assertEquals(12,share);
            assertEquals(0,CondominiumLife.allocate(t,s,other,155));
            EconomyLife.addBill(t,s,c,155-share,"housing and utilities",new ArrayList<>(),new ArrayList<>());
            assertEquals(12,CondominiumLife.collect(t,s,c));
            assertEquals(45,c.money);
            assertEquals(12,CondominiumLife.report(t).reserve());
            assertEquals(0,CondominiumLife.report(t).arrears());
            assertEquals(0,CondominiumLife.collect(t,s,c));
            s.day=31;c.money=20;s.money=20;
            assertEquals(12,CondominiumLife.allocate(t,s,c,155));
            assertEquals(0,CondominiumLife.collect(t,s,c));
            assertEquals(12,CondominiumLife.report(t).arrears());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(12,CondominiumLife.report(t).reserve());
            assertEquals(12,CondominiumLife.report(t).arrears());
            var c=t.get("marco");c.money=25;
            assertEquals(5,CondominiumLife.collect(t,s,c));
            assertEquals(20,c.money);
            assertEquals(17,CondominiumLife.report(t).reserve());
            assertEquals(7,CondominiumLife.report(t).arrears());
        }
    }
}
