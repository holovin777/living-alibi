package com.holovin.npclife;

/** A visitor exists physically, can remain anonymous, and reveals identity per resident. */
public final class PlayerPresenceSmokeTest {
    private PlayerPresenceSmokeTest() { }
    public static void main(String[] args) {
        WorldState clock=new WorldState();
        TownLife.Town town=TownLife.create(clock);
        var marco=town.get("marco");marco.place=TownLife.Place.PARK;
        var sara=new TownLife.Citizen("resident-0001","Sara Grassi","Librarian",TownLife.Place.PARK);
        sara.home=TownLife.Place.NORTH_RESIDENCES;
        town.people.put(sara.id,sara);
        check(town.player.privateName==null,"anonymous at migration");
        check(town.player.contacts.isEmpty(),"no fabricated social knowledge");
        check(PlayerLife.visible(town).size()==2,"both residents visible in public park");
        PlayerLife.setPrivateName(town.player,"Viktor Holovin");
        check(town.player.contacts.isEmpty(),"private name is not broadcast");
        String reply=PlayerLife.speak(town,"marco","Hello there",clock.day,clock.minute);
        check(reply.contains("stranger"),"greeting must not know private name");
        check(town.player.contacts.get("marco").knownName==null,"Marco still does not know name");
        PlayerLife.introduce(town,"resident-0001","Viktor",clock.day,clock.minute);
        check("Viktor".equals(town.player.contacts.get("resident-0001").knownName),"Sara learns disclosed name");
        check(town.player.contacts.get("marco").knownName==null,"Marco not informed by Sara's introduction");
        PlayerLife.speak(town,"marco","My name is Alex.",clock.day,clock.minute);
        check("Alex".equals(town.player.contacts.get("marco").knownName),"explicit spoken introduction is remembered");
        check("Viktor Holovin".equals(town.player.privateName),"other alias does not replace private identity");
        var snapshot=town.copy();
        snapshot.player.contacts.get("marco").knownName="Changed";
        check("Alex".equals(town.player.contacts.get("marco").knownName),"copy has independent identity memories");
        var first=PlayerLife.startTravel(town.player,TownLife.Place.NORTH_RESIDENCES);
        check(first==10,"travel takes simulated walking time");
        check(PlayerLife.visible(town).isEmpty(),"no public witnesses while traveling");
        for(int i=0;i<first;i++)PlayerLife.tick(town.player);
        check(town.player.place==TownLife.Place.NORTH_RESIDENCES,"visitor arrived");
        sara.place=TownLife.Place.NORTH_RESIDENCES;
        check(PlayerLife.visible(town).isEmpty(),"private apartment never visible from street");
        boolean denied=false;
        try {PlayerLife.nearby(town,"resident-0001");} catch(IllegalArgumentException ex){denied=true;}
        check(denied,"cannot remotely speak inside private apartment");
        denied=false;
        try {PlayerLife.destination("MARCO_HOME");} catch(IllegalArgumentException ex){denied=true;}
        check(denied,"cannot enter a private home by go");
        System.out.println("PlayerPresenceSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String label) {if(!ok)throw new AssertionError(label);}
}
