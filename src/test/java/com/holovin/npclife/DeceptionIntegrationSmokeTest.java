package com.holovin.npclife;

import java.util.ArrayList;

/** End-to-end rule simulation: deceptive claims emerge while world truth stays intact. */
public final class DeceptionIntegrationSmokeTest {
    private DeceptionIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        LifeSimulation.advance(clock,28L*1440L,
                c->TownLife.tick(town,c,truth,observations,interactions));
        long secretCount=town.secrets.values().stream().mapToInt(m->m.size()).sum();
        long lies=town.pendingDecisions.stream().filter(d->d.kind().equals("LIE")).count();
        if(secretCount<1)throw new AssertionError("Sensitive eyewitness experiences produced no secrets");
        // A fixed deadline for an emergent lie is not a world invariant:
        // schedules, witness opportunities and trust all change independently.
        // Keep the autonomous simulation check above and test an actual
        // low-trust encounter separately, with deterministic preconditions.
        deliberateEncounter();
        for(var d:town.pendingDecisions) {
            if(!d.kind().equals("LIE"))continue;
            var matching=truth.stream().filter(e->KnowledgeLife.eventKey(e).equals(d.eventKey())).findFirst();
            if(matching.isEmpty())throw new AssertionError("A deception has no underlying truthful event");
            if(matching.get().text().equals(d.statement()))throw new AssertionError("A lie is identical to truth");
            if(truth.stream().anyMatch(e->e.text().equals(d.statement())))
                throw new AssertionError("A deceptive statement leaked into authoritative event log");
        }
        System.out.println("DeceptionIntegrationSmokeTest: ALL CHECKS PASSED ("+
                secretCount+" secrets, "+lies+" lies in 28 days)");
    }

    private static void deliberateEncounter(){
        var clock=new WorldState();clock.day=3;clock.minute=600;
        var town=TownLife.create(clock);
        // Isolate two living residents in a real public scene so unrelated
        // conversations cannot change the seeded encounter.
        var a=new TownLife.Citizen("fixture-a","Test A","Job seeker",TownLife.Place.FARM);
        var b=new TownLife.Citizen("fixture-b","Test B","Job seeker",TownLife.Place.FARM);
        town.people.clear();
        a.home=TownLife.Place.SOUTH_RESIDENCES;b.home=TownLife.Place.NORTH_RESIDENCES;
        a.generationIndex=9001;b.generationIndex=9002;
        a.sociability=100;b.sociability=100;a.fatigue=0;b.fatigue=0;
        a.carefulness=90;a.activity="FREE_TIME";b.activity="FREE_TIME";
        town.people.put(a.id,a);town.people.put(b.id,b);
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var social=new ArrayList<SocialLife.Interaction>();
        String statement="Test A had an argument at Farm.";
        b.place=TownLife.Place.PARK;b.destination=b.place;
        TownLife.publish(town,clock,a.id,a.place,statement,truth,seen);
        String key=KnowledgeLife.eventKey(truth.getFirst());
        DeceptionLife.mark(town,a.id,key,clock.day,clock.minute,"PERSONAL_INCIDENT");
        b.place=a.place;b.destination=a.place;
        var bond=new SocialLife.Relationship(a.id,b.id);
        bond.trust=35;bond.tension=0;bond.rng=2;
        town.peerRelationships.put(SocialNetwork.key(a.id,b.id),bond);
        // Ten minutes together, then the social engine's normal approach.
        for(int m=600;m<=615;m++){
            clock.minute=m;
            SocialNetwork.tick(town,clock,truth,seen,social);
        }
        if(social.size()!=1)throw new AssertionError("Expected one actual conversation");
        var meeting=social.getFirst();
        KnowledgeLife.exchange(town,clock,meeting.firstId(),meeting.secondId());
        var claim=town.beliefs.get(b.id).get(key);
        if(claim==null||!claim.origin().equals("HEARD")||claim.statement().equals(statement))
            throw new AssertionError("Low-trust encounter failed to relay deceptive testimony");
        if(town.pendingDecisions.stream().noneMatch(d->d.kind().equals("LIE")&&d.eventKey().equals(key)))
            throw new AssertionError("Deliberate false claim was not recorded");
        if(truth.stream().noneMatch(e->e.text().equals(statement))||
                truth.stream().anyMatch(e->e.text().equals(claim.statement())))
            throw new AssertionError("False testimony changed authoritative truth");
    }
}
