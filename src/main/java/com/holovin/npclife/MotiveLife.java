package com.holovin.npclife;

/** Time-local motive pressure, not a declaration that a resident is guilty. */
final class MotiveLife {
    private MotiveLife() { }
    record Indicators(int financial, int conflict, int impulse) {
        String publicSummary() {
            return "Financial strain: "+band(financial)+
                   " | Personal friction: "+band(conflict)+
                   " | Impulsivity: "+band(impulse)+
                   ". These are contextual leads, not evidence of guilt.";
        }
    }
    record Assessment(String reason, int strength) { }
    static int clamp(int n) { return Math.max(0, Math.min(100, n)); }
    private static String band(int n) { return n>=67?"elevated":n>=34?"moderate":"low"; }

    static Indicators indicators(TownLife.Town town,WorldState clock,
                                 TownLife.Citizen person,TownLife.Citizen victim) {
        // Current money and recorded personality are meaningful only at the incident time.
        int owed=person.unpaidBills;
        for(var loan:town.loans.values())
            if(loan.borrowerId.equals(person.id))owed+=loan.outstanding;
        int financial=clamp(110-person.money+person.hunger/4 + Math.min(65,owed/3) +
                (!person.employed?Math.min(35,person.unemploymentDays*2):0) +
                (person.id.equals("marco")?Math.min(60,clock.debt/6):0));
        var relation=person.id.equals(victim.id)?null:town.existingRelationship(person.id,victim.id);
        int conflict=relation==null?0:clamp(relation.tension*2-relation.trust/3);
        int impulse=clamp(82-person.carefulness+person.curiosity/3);
        return new Indicators(financial,conflict,impulse);
    }
    static Assessment assess(TownLife.Town town,WorldState clock,
                             TownLife.Citizen offender,TownLife.Citizen victim) {
        var v=indicators(town,clock,offender,victim);
        int envy=clamp((victim.money-offender.money)/6);
        // In rare circumstances a poorly inhibited citizen acts on temptation even
        // without immediate financial need or a history of interpersonal conflict.
        int temptation=v.impulse()*3/4;
        int strongest=Math.max(Math.max(v.financial(),v.conflict()),Math.max(envy,temptation));
        String reason=strongest==v.financial()?"FINANCIAL_PRESSURE":
                strongest==v.conflict()?"PERSONAL_GRIEVANCE":
                strongest==envy?"ENVY":"IMPULSIVE_TEMPTATION";
        int drive=clamp(strongest+v.impulse()/5);
        return new Assessment(reason,drive);
    }
}
