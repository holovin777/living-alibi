package com.holovin.npclife;

/** Ordinary visitors freely choose work and study. Every wage comes from finite funds. */
public final class CareerSmokeTest {
    private CareerSmokeTest() { }

    public static void main(String[] args) {
        var state=new WorldState();
        var town=TownLife.create(state);
        var p=town.player;
        check(p.place==TownLife.Place.PARK && p.money==50 && p.jobId==null,
                "visitor arrives at park with 50 EUR and no assigned job");
        check(p.engineeringSkill==0 && p.investigationSkill==0,
                "no professional training on entry");
        check(CareerLife.offers().size()==8 && CareerLife.offers().stream().anyMatch(o->o.id().equals("farm-worker")),"vacancies include multiple career paths");
        long initialMoney=totalMoney(town);

        boolean rejected=false;
        try {CareerLife.apply(town,"shop-assistant");}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected && p.jobId==null,"cannot apply remotely from park");

        p.place=TownLife.Place.MARKET;
        p.destination=p.place;
        CareerLife.apply(town,"shop-assistant");
        check("shop-assistant".equals(p.jobId),"first occupation is a voluntary choice");
        rejected=false;
        try {CareerLife.work(town,1,450);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"cannot work before opening hours");

        int marketBefore=town.employerFunds.get("shop-assistant");
        CareerLife.work(town,1,540);
        check(p.money==74 && town.employerFunds.get("shop-assistant")==marketBefore-24,
                "market wage transfers real funds to player");
        check(totalMoney(town)==initialMoney,"work does not mint money");
        rejected=false;
        try {CareerLife.work(town,1,780);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected && p.money==74,"no second shift on same day");

        p.place=TownLife.Place.SCHOOL;
        p.destination=p.place;
        int schoolBefore=town.employerFunds.get("school");
        for(int i=0;i<3;i++)CareerLife.study(town,"engineering",600+i*60);
        check(p.engineeringSkill==3 && p.money==44,"education costs money and builds skill");
        check(town.employerFunds.get("school")==schoolBefore+30,
                "tuition reaches institution rather than disappearing");
        check(totalMoney(town)==initialMoney,"classes conserve money");
        p.place=TownLife.Place.WORKSHOP;
        p.destination=p.place;
        CareerLife.apply(town,"technician");
        check("technician".equals(p.jobId),"technical apprenticeship unlocked through study");
        int techBefore=town.employerFunds.get("technician");
        boolean earlySalary=false;
        try {CareerLife.work(town,2,540);}
        catch(IllegalArgumentException expected) {earlySalary=true;}
        check(earlySalary,"technician needs actual completed work before wages");
        TechnicalWorkLife.inspect(town,"water-pump",540);
        TechnicalWorkLife.complete(town,"water-pump",2,570);
        CareerLife.work(town,2,720);
        check(p.money==82 && town.employerFunds.get("technician")==techBefore-38,
                "higher skilled job pays from its own employer's budget");
        check(totalMoney(town)==initialMoney,"career changes do not create new currency");

        rejected=false;
        try {CareerLife.apply(town,"engineer");}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"engineering profession requires further qualification");
        p.place=TownLife.Place.SCHOOL;
        p.destination=p.place;
        for(int i=0;i<5;i++)CareerLife.study(town,"engineering",600+i*60);
        check(p.engineeringSkill==8 && p.money==32,
                "university-like coursework grows skill without instant promotion");
        p.place=TownLife.Place.OFFICE;
        p.destination=p.place;
        CareerLife.apply(town,"engineer");
        check("engineer".equals(p.jobId),"qualified player can become junior engineer");

        rejected=false;
        try {CareerLife.apply(town,"investigator");}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected && "engineer".equals(p.jobId),
                "engineer does not automatically become investigator");
        p.place=TownLife.Place.LIBRARY;
        p.destination=p.place;
        for(int i=0;i<3;i++)CareerLife.study(town,"investigation",600+i*60);
        check(p.investigationSkill==3,"private investigation requires different education");
        check(totalMoney(town)==initialMoney,"all study and wages conserve cash");
        rejected=false;
        try {CareerLife.study(town,"investigation",900);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected && p.money==2,"no studying without tuition");

        p.health=0;
        p.alive=false;
        PlayerLife.beginNewLife(town,3,600);
        check(town.player.money==50 && town.player.jobId==null &&
                town.player.engineeringSkill==0 && town.player.investigationSkill==0,
                "successor starts a different life, not the prior person's career");
        check(town.pastLives.containsKey(1) && town.employerFunds.get("technician")==techBefore-38,
                "world economy and old life history remain after death");

        System.out.println("CareerSmokeTest: ALL CHECKS PASSED");
    }

    private static long totalMoney(TownLife.Town town) {
        long total=town.player.money;
        for(var value:town.employerFunds.values())total+=value;
        return total;
    }

    private static void check(boolean ok,String what) {
        if(!ok)throw new AssertionError(what);
    }
}
