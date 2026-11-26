package com.holovin.npclife;

/** Run with assertions enabled. Checks privacy, deterministic membership and consequences. */
public final class MafiaSmokeTest {
    public static void main(String[] args) {
        var t=new TownLife.Town();
        for(int i=1;i<=24;i++){
            var c=new TownLife.Citizen("resident-"+i,"Citizen "+i,"Baker",TownLife.Place.MARKET);
            c.generationIndex=i;c.personalSeed=i;c.age=30;
            t.people.put(c.id,c);
        }
        t.people.get("resident-1").occupation=PoliceLife.OFFICER;
        var clock=new WorldState();clock.day=2;clock.minute=600;
        MafiaLife.tick(t,clock);
        assert t.people.values().stream().filter(c->MafiaLife.active(t,c)).count()==3;
        assert !MafiaLife.active(t,t.people.get("resident-1"));
        assert t.archive.isEmpty();
        var member=t.people.get("resident-2");
        assert MafiaLife.active(t,member);
        assert member.occupation.equals("Baker");
        var ordinary=new MotiveLife.Assessment("ENVY",50);
        assert MafiaLife.assess(t,member,ordinary).strength()==62;
        assert MafiaLife.assess(t,t.people.get("resident-24"),ordinary).equals(ordinary);
        var c=new InvestigationLife.CaseFile("case-0001",2,650,TownLife.Place.MARKET,
                "resident-24",member.id,"wallet",15);
        t.cases.put(c.id,c);MafiaLife.noteCase(t,c);
        assert t.archive.size()==1;
        assert t.archive.values().stream().noneMatch(e->e.text().contains(member.name));
        var copy=t.copy();
        assert MafiaLife.active(copy,copy.people.get(member.id));
        c.solved=true;clock.day=3;
        MafiaLife.tick(t,clock);MafiaLife.tick(t,clock);
        assert !MafiaLife.active(t,member);
        assert t.people.values().stream().filter(p->MafiaLife.active(t,p)).count()==2;
        assert t.archive.size()==2;
        System.out.println("MafiaSmokeTest: ALL CHECKS PASSED");
    }
}
