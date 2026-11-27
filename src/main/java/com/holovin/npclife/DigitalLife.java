package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Private resident dashboard and public fictional websites. No external network,
 * fabricated accounts or access to another resident's private records. */
final class DigitalLife {
    private DigitalLife(){}
    record Site(String name,String status,String description){}
    record Notice(int day,String text){}
    record View(boolean online,int wallet,int bills,boolean canPay,List<Site> sites,List<String> jobs,List<Notice> news,TelecomLife.View telecom){}
    static boolean online(TownLife.Town t){return EnergyLife.publicPower(t);}
    static View view(TownLife.Town t,WorldState s){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c)||NpcJusticeLife.confined(t,c,s))return null;
        var telecom=TelecomLife.view(t,c,s);
        boolean online=online(t)&&TelecomLife.connected(t,c,s);
        if(!online)return new View(false,c.money,c.unpaidBills,false,List.of(),List.of(),List.of(),telecom);
        boolean shop=SoftwareLife.deployed(t,"retail-api")&&!DevOpsLife.unavailable(t);
        boolean utility=SoftwareLife.deployed(t,"water-control");
        var sites=List.of(
            new Site("Comune","ONLINE","Municipal programme: "+ElectionLife.label(ElectionLife.policy(t))+"; election day "+ElectionLife.get(t,ElectionLife.NEXT)),
            new Site("Bottega Alimentare",shop?"ONLINE":"UNAVAILABLE",shop?"Bread: "+t.retailStock.getOrDefault("bread",0)+" · Water: "+t.retailStock.getOrDefault("water",0):"Retail service not deployed or unavailable."),
            new Site("Acqua / Water",utility?"ONLINE":"UNAVAILABLE",utility?"Clean water stock: "+ITBusinessLife.utilityTotal(t):"Monitoring service not deployed."),
            new Site("Employment centre","ONLINE","Vacancies reflect qualifications and existing staffing."),
            new Site("Personal finance","ONLINE","Resident wallet and pooled outstanding bills. Payments use existing accounting."));
        var news=t.archive.values().stream().filter(n->List.of("Municipality","Public safety","Culture","Sport",NewspaperLife.CATEGORY).contains(n.category()))
            .sorted(Comparator.comparingInt(ArchiveLife.Entry::day).thenComparingInt(ArchiveLife.Entry::minute).reversed())
            .limit(8).map(n->new Notice(n.day(),n.text())).toList();
        return new View(true,c.money,c.unpaidBills,RebirthLife.awakePresent(c)&&c.unpaidBills>0&&c.money>20,
            sites,EmploymentLife.openRoles(t,c,s.day),news,telecom);
    }
    static String pay(TownLife.Town t,WorldState s){
        var view=view(t,s);
        if(view==null||!view.canPay())throw new IllegalArgumentException("No payable bills or digital service unavailable.");
        int paid=EconomyLife.payBills(t,s,RebirthLife.requireAdult(t),new java.util.ArrayList<>(),new java.util.ArrayList<>());
        return "Paid EUR "+paid+" towards outstanding bills.";
    }
}
