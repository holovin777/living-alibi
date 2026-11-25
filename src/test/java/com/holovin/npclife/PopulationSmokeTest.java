package com.holovin.npclife;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Generation, reproducibility, movement, survival and witness scope. */
public final class PopulationSmokeTest {
    private PopulationSmokeTest() { }
    public static void main(String[] args) {
        var initial=new WorldState();
        var town=TownLife.create(initial);
        var citizens=PopulationGenerator.add(town,20);
        check(citizens.size()==20 && town.people.size()==22,"20 residents generated");
        check(new HashSet<>(citizens.stream().map(c->c.name).toList()).size()==20,"names unique");
        check(new HashSet<>(citizens.stream().map(c->c.id).toList()).size()==20,"IDs unique");
        check(citizens.stream().allMatch(c->c.age>=19 && c.age<=70),"age range");
        var same=TownLife.create(new WorldState());
        PopulationGenerator.add(same,20);
        for(var c:citizens) {
            var twin=same.get(c.id);
            check(c.name.equals(twin.name) && c.goal.equals(twin.goal) &&
                    c.personalSeed==twin.personalSeed,"stable generation across fresh worlds");
        }
        personalityPreferences();
        var first=town.get("resident-0001");
        check(first.home!=null && first.workplace!=null,"home and workplace assigned");
        var events=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(initial,30L*1440L,
                s->TownLife.tick(town,s,events,observations));
        check(town.people.size()==22,"population persists during simulation");
        check(town.get("resident-0001").alive,"generated resident survives 30 days");
        check(town.get("resident-0001").health>=70,"generated resident maintains health");
        check(citizens.stream().allMatch(c->c.alive && c.health>=60),
                "all solvent residents survive 30 days");
        check(events.stream().anyMatch(e->e.actorId().equals(first.id) && e.text().contains("arrived")),
                "generated resident travels");
        check(events.stream().anyMatch(e->e.actorId().equals(first.id) && e.text().contains("purchased")),
                "generated resident buys supplies");
        // An event is never witnessed remotely, even when known by the world engine.
        for(var observation:observations) {
            check(observation.witnessId()!=null && observation.place()!=null,"witness has identity/location");
        }
        var copy=town.copy();
        check(copy.people.size()==22 && copy.get(first.id).age==first.age,"copy retains generated residents");
        var more=PopulationGenerator.add(copy,10);
        check(more.getFirst().id.equals("resident-0021"),"IDs advance without collisions");
        check(town.people.size()==22,"copy modified separately");
        try { PopulationGenerator.add(copy,20); throw new AssertionError("limit not enforced"); }
        catch(IllegalArgumentException expected) { /* max is 40 */ }
        System.out.println("PopulationSmokeTest: ALL CHECKS PASSED (20 residents; 30 days)");
    }
    private static void personalityPreferences() {
        var social=new TownLife.Citizen("social-test","Social","Barista",TownLife.Place.CAFE);
        var quiet=new TownLife.Citizen("quiet-test","Quiet","Librarian",TownLife.Place.LIBRARY);
        social.personalSeed=quiet.personalSeed=1234L;
        social.sociability=90;social.curiosity=20;social.carefulness=20;
        quiet.sociability=20;quiet.curiosity=90;quiet.carefulness=90;
        int[] a=new int[4],b=new int[4];
        for(int day=1;day<=365;day++){
            int x=PersonalityLife.leisurePreference(social,day);
            int y=PersonalityLife.leisurePreference(quiet,day);
            check(x==PersonalityLife.leisurePreference(social,day),"leisure deterministic");
            a[x]++;b[y]++;
        }
        check(a[0]>b[0]&&b[1]>a[1],"social residents favour cafes, curious careful residents favour libraries");
        check(java.util.Arrays.stream(a).allMatch(n->n>0)&&java.util.Arrays.stream(b).allMatch(n->n>0),"preferences never prohibit destinations");
        check(PersonalityLife.conflictBias(social,social)>PersonalityLife.conflictBias(quiet,quiet),"carefulness lowers conflict pressure");
        check(PersonalityLife.affinityBonus(social,social)>PersonalityLife.affinityBonus(social,quiet),"shared interests improve affinity");
        check(PersonalityLife.careerBias(social,"social")>PersonalityLife.careerBias(quiet,"social"),"social career preference");
        check(PersonalityLife.careerBias(quiet,"academic")>PersonalityLife.careerBias(social,"academic"),"academic career preference");
    }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
}
