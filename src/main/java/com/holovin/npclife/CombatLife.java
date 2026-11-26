package com.holovin.npclife;

import java.util.List;

/** Fictional, nonlethal first-pass combat in the visitor life. */
final class CombatLife {
    private CombatLife(){}
    private static String key(TownLife.Town t,String field){
        return "combat:"+t.player.incarnation+":"+field;
    }
    static boolean ownsBaton(TownLife.Town t){
        return t.industryStocks.getOrDefault(key(t,"baton"),0)>0;
    }
    static int reports(TownLife.Town t){
        return t.industryStocks.getOrDefault(key(t,"police-reports"),0);
    }
    private static int now(WorldState clock){
        return (clock.day-1)*1440+clock.minute+1;
    }
    private static void ready(TownLife.Town t,WorldState clock){
        PlayerLife.requireAlive(t.player);
        if(RebirthLife.active(t)||t.player.travelRemaining>0||t.player.cycling||
                IntercomLife.insideVisit(t,clock.day,clock.minute))
            throw new IllegalArgumentException("Combat is available to adult visitors on foot in public areas.");
    }
    static String buyBaton(TownLife.Town t,WorldState clock){
        ready(t,clock);
        if(t.player.place!=TownLife.Place.MARKET||clock.minute<480||clock.minute>=1200)
            throw new IllegalArgumentException("Visit the market during opening hours.");
        WeaponLicenseLife.requireLicense(t,clock);
        if(ownsBaton(t))throw new IllegalArgumentException("You already own a training baton.");
        if(t.player.money<12)throw new IllegalArgumentException("Need 12 EUR.");
        t.player.money-=12;
        t.employerFunds.merge("shop-assistant",12,Math::addExact);
        t.industryStocks.put(key(t,"baton"),1);
        return "Bought a training baton for 12 EUR.";
    }
    record Result(String message,int minutes){}
    static Result fire(TownLife.Town t,WorldState clock,String id,List<TownLife.Event> truth,List<TownLife.Observation> observations){
        ready(t,clock);WeaponLicenseLife.requireSport(t,clock);
        if(!id.isEmpty()){
            var target=PlayerLife.nearby(t,id);
            if(target.age>=18&&target.health>20)return strike(t,clock,id,"sport",truth,observations);
        }
        WeaponLicenseLife.spendRound(t);
        TownLife.publish(t,clock,"visitor:"+t.player.incarnation,t.player.place,
                "A shot was fired in a public space.",truth,observations);
        boolean officer=PlayerLife.visible(t).stream().anyMatch(c->PoliceLife.scheduled(c,clock.day,clock.minute));
        t.industryStocks.merge(key(t,"police-reports"),1,Math::addExact);
        JusticeLife.reportDischarge(t,clock,officer,truth,observations);
        return new Result(officer?"Shot fired. Police intervened.":"Shot fired. Police report recorded.",officer?30:2);
    }
    static Result strike(TownLife.Town t,WorldState clock,String id,String weapon,
                         List<TownLife.Event> truth,List<TownLife.Observation> observations){
        ready(t,clock);
        if(!List.of("fists","baton","sport").contains(weapon))throw new IllegalArgumentException("Unknown weapon.");
        if("baton".equals(weapon)&&!ownsBaton(t))throw new IllegalArgumentException("You do not own a training baton.");
        if("sport".equals(weapon))WeaponLicenseLife.requireSport(t,clock);
        var victim=PlayerLife.nearby(t,id);
        if(victim.age<18||victim.health<=20)throw new IllegalArgumentException("This person cannot be attacked.");
        int last=t.industryStocks.getOrDefault(key(t,"last-strike"),0);
        if(last>0&&now(clock)-last<2)throw new IllegalArgumentException("You need a moment to recover.");
        if("sport".equals(weapon))WeaponLicenseLife.spendRound(t);
        int damage="sport".equals(weapon)?18:"baton".equals(weapon)?12:6;
        victim.health=Math.max(20,victim.health-damage);
        if("marco".equals(id))clock.health=victim.health;
        t.player.fatigue=Math.min(100,t.player.fatigue+5);
        t.industryStocks.put(key(t,"last-strike"),now(clock));
        // All testimony comes from the actual shared scene; no automatic player-name disclosure.
        TownLife.publish(t,clock,"visitor:"+t.player.incarnation,t.player.place,
                ("sport".equals(weapon)?"A visitor fired a shot that injured ":"A visitor struck ")+victim.name+".",truth,observations);
        boolean officer=false;
        for(var witness:PlayerLife.visible(t)){
            var contact=t.player.contacts.computeIfAbsent(witness.id,PlayerLife.Contact::new);
            boolean direct=witness.id.equals(id);
            contact.trust=Math.max(0,contact.trust-(direct?35:12));
            contact.affinity=Math.max(-100,contact.affinity-(direct?40:10));
            SocialMemoryLife.remember(t,witness.id,SocialMemoryLife.Kind.BROKEN_TRUST,clock.day);
            if(PoliceLife.scheduled(witness,clock.day,clock.minute))officer=true;
        }
        t.industryStocks.merge(key(t,"police-reports"),1,Math::addExact);
        JusticeLife.report(t,clock,officer,!"fists".equals(weapon),truth,observations);
        if(officer){
            if("baton".equals(weapon))t.industryStocks.put(key(t,"baton"),0);
            return new Result("sport".equals(weapon)?"Shot fired. Police intervened.":"Police intervened and recorded the assault.",30);
        }
        return new Result("sport".equals(weapon)?"Shot fired. Police report recorded.":"You struck a resident. Witnesses remember the assault.",2);
    }
}
