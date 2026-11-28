package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RecentEconomyPersistenceTest {
 @TempDir Path dir;
 @Test void dynamicLedgersSurviveActualDatabaseReload()throws Exception{
    var file=dir.resolve("economy.db");
    var keys=List.of(StaffingLife.ENABLED,"sim:resident-0001:until","business-owner:shop-assistant",
        "business-credit:shop-assistant:balance","trade-credit:cement:steel:balance","npc-enterprise:owner:shop-assistant",
        "commercial:via-roma-office:stage","residential-development:casa-verde:stage","industrial-expansion:cement-2:stage",
        "infrastructure:solar-extension:stage","transport-development:FACTORY:stage","bus-fleet:1:owned","regional-rail:pavia:cargo");
    var accounts=List.of(ConstructionCrewLife.ACCOUNT,MunicipalBusFleetLife.ACCOUNT,RegionalRailLife.ACCOUNT,
        "industrial-expansion:cement-2",EnergyLife.ACCOUNT,WaterNetworkLife.ACCOUNT);
    try(var db=new WorldDatabase(file)){
        var s=db.load();var t=db.loadTown(s);
        for(String k:keys)t.industryStocks.put(k,1);
        for(String a:accounts)t.employerFunds.put(a,37);
        db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
    }
    try(var db=new WorldDatabase(file)){
        var t=db.loadTown(db.load());
        for(String k:keys)assertEquals(1,t.industryStocks.get(k),k);
        for(String a:accounts)assertEquals(37,t.employerFunds.get(a),a);
    }
 }
}
