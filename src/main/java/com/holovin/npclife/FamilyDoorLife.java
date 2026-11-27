package com.holovin.npclife;

/** Temporary supervised household restriction, not control of a child's life decisions.
 * Adults, school attendance, emergencies and unsupervised exits are never locked in.
 */
final class FamilyDoorLife {
    private FamilyDoorLife(){}
    private static String key(TownLife.Citizen c,String f){return "family-door:"+c.homeUnit+":"+f;}
    private static long time(int day,int minute){return (day-1L)*1440+minute;}
    static boolean canManage(TownLife.Town t,TownLife.Citizen parent){
        return RebirthLife.linkedAdult(t,parent)&&HomeRoutineLife.atHome(parent)&&t.people.values().stream()
                .anyMatch(c->c.alive&&c.age<18&&c.homeUnit.equals(parent.homeUnit)&&ParentingLife.parents(t,c).contains(parent));
    }
    static void act(TownLife.Town t,WorldState s,boolean lock){
        var parent=RebirthLife.child(t);
        if(!canManage(t,parent))throw new IllegalArgumentException("Only a parent at the family home can manage the door.");
        t.industryStocks.put(key(parent,"until"),lock?Math.toIntExact(time(s.day,s.minute)+60):0);
        t.industryStocks.put(key(parent,"parent"),lock?parent.generationIndex:0);
    }
    static boolean lockedFor(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(c==null||!c.alive||c.age>=18||c.health<60||!HomeRoutineLife.atHome(c)||FirstLifeChoice.accountOwned(t,c))return false;
        if(t.industryStocks.getOrDefault(key(c,"until"),0)<=time(day,minute))return false;
        var parent=DemographyLife.byIndex(t,t.industryStocks.getOrDefault(key(c,"parent"),0));
        return parent!=null&&parent.alive&&parent.health>=60&&HomeRoutineLife.atHome(parent)
                &&parent.homeUnit.equals(c.homeUnit)&&ParentingLife.parents(t,c).contains(parent);
    }
}
