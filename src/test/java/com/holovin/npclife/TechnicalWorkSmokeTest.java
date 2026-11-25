package com.holovin.npclife;

/** Technical careers improve actual persistent infrastructure, not just wallets. */
public final class TechnicalWorkSmokeTest {
    private TechnicalWorkSmokeTest() { }

    public static void main(String[] args) {
        var town=TownLife.create(new WorldState());
        var p=town.player;
        check(town.technicalMaterials.get("spare-parts")==6 &&
                town.technicalOrders.get("water-pump").condition==45,
                "starter workshop begins with physical stock and faulty infrastructure");
        p.place=TownLife.Place.WORKSHOP;
        p.destination=p.place;
        p.engineeringSkill=3;

        boolean denied=false;
        try {TechnicalWorkLife.inspect(town,"water-pump",540);}
        catch(IllegalArgumentException ex) {denied=true;}
        check(denied,"cannot repair without being employed");
        CareerLife.apply(town,"technician");
        denied=false;
        try {CareerLife.work(town,1,540);}
        catch(IllegalArgumentException ex) {denied=true;}
        check(denied,"technician does not receive wages for empty shift");

        String inspected=TechnicalWorkLife.inspect(town,"water-pump",540);
        check(inspected.contains("2 spare-parts") &&
                town.technicalOrders.get("water-pump").phase==TechnicalWorkLife.Phase.INSPECTED,
                "inspection reveals concrete material requirements");
        int before=town.technicalMaterials.get("spare-parts");
        TechnicalWorkLife.complete(town,"water-pump",1,570);
        check(town.technicalOrders.get("water-pump").condition==100 &&
                town.technicalMaterials.get("spare-parts")==before-2,
                "repair consumes two parts and restores pump condition");
        CareerLife.work(town,1,720);
        check(p.money==88 && town.employerFunds.get("technician")==722,
                "wage transfers from employer after actual completed repair");
        denied=false;
        try {TechnicalWorkLife.complete(town,"water-pump",1,850);}
        catch(IllegalArgumentException ex) {denied=true;}
        check(denied,"duplicate repair cannot consume parts or create credit");

        // Material depletion rejects execution without mutating the diagnosis.
        var copied=town.copy();
        copied.technicalOrders.get("water-pump").phase=TechnicalWorkLife.Phase.INSPECTED;
        copied.technicalMaterials.put("spare-parts",0);
        int untouched=copied.technicalOrders.get("water-pump").condition;
        denied=false;
        try {TechnicalWorkLife.complete(copied,"water-pump",2,570);}
        catch(IllegalArgumentException ex) {denied=true;}
        check(denied && copied.technicalOrders.get("water-pump").condition==untouched &&
                copied.technicalMaterials.get("spare-parts")==0,
                "not enough material means no phantom repair");

        var office=town.copy();
        office.player.engineeringSkill=8;
        office.player.place=TownLife.Place.OFFICE;
        office.player.destination=TownLife.Place.OFFICE;
        CareerLife.apply(office,"engineer");
        denied=false;
        try {CareerLife.work(office,2,540);}
        catch(IllegalArgumentException ex) {denied=true;}
        check(denied,"engineer cannot be paid before delivering project");
        TechnicalWorkLife.inspect(office,"street-lights",540);
        int steel=office.technicalMaterials.get("steel");
        int copper=office.technicalMaterials.get("copper");
        TechnicalWorkLife.complete(office,"street-lights",2,570);
        check(office.technicalOrders.get("street-lights").condition==90 &&
                office.technicalMaterials.get("steel")==steel-1 &&
                office.technicalMaterials.get("copper")==copper-1,
                "engineering upgrades city lighting using actual shared metal stock");
        CareerLife.work(office,2,720);
        check(office.player.money==150,
                "engineer earns a wage from existing company budget");
        check(town.technicalOrders.get("street-lights").condition==50,
                "speculative work cannot mutate the original town before commit");
        check(town.technicalMaterials.get("steel")==4,
                "copied city material stocks are isolated");

        var persisted=office.copy();
        TechnicalWorkLife.tick(persisted,3,0);
        check(persisted.technicalOrders.get("street-lights").condition==89,
                "infrastructure gradually wears after each midnight");
        for(int d=4;d<=30;d++)TechnicalWorkLife.tick(persisted,d,0);
        check(persisted.technicalOrders.get("street-lights").phase==TechnicalWorkLife.Phase.OPEN,
                "eventual wear reopens a real new task, but does not mint new stock");

        System.out.println("TechnicalWorkSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
