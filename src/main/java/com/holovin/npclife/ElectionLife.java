package com.holovin.npclife;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/** A local election with a secret simulated ballot and finite policy allocations. */
final class ElectionLife {
    private ElectionLife(){}
    static final int TERM=5*365, CAMPAIGN=30, BALANCED=0, EDUCATION=1, HEALTH=2, INFRASTRUCTURE=3;
    static final String NEXT="election:next", MAYOR="election:mayor", OWNER="election:mayor-incarnation",
            POLICY="election:policy", PLAYER="election:player-candidate", PLAYER_PLAN="election:player-plan",
            VOTE="election:vote", VOTER="election:voter", LAST="election:last", WINNER="election:winner",
            RESIDENT_CANDIDATE="election:resident-candidate", RESIDENT_VOTER="election:resident-voter";
    static String candidate(int slot){return "election:candidate:"+slot;}
    static String votes(int slot){return "election:votes:"+slot;}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int value){t.industryStocks.put(k,value);}
    static void ensure(TownLife.Town t){
        for(String k:List.of(NEXT,MAYOR,OWNER,POLICY,PLAYER,PLAYER_PLAN,VOTE,VOTER,LAST,WINNER,RESIDENT_CANDIDATE,RESIDENT_VOTER))t.industryStocks.putIfAbsent(k,0);
        for(int i=1;i<=4;i++){t.industryStocks.putIfAbsent(candidate(i),0);t.industryStocks.putIfAbsent(votes(i),0);}
    }
    static boolean playerMayor(TownLife.Town t){
        var c=RebirthLife.child(t);
        if(RebirthLife.linkedAdult(t,c))return get(t,MAYOR)==c.generationIndex;
        return !RebirthLife.active(t)&&t.player.alive&&get(t,OWNER)==t.player.incarnation;
    }
    static boolean npcMayor(TownLife.Town t){
        var c=DemographyLife.byIndex(t,get(t,MAYOR));
        return c!=null&&OpinionLife.eligible(c);
    }
    static int policy(TownLife.Town t){return playerMayor(t)||npcMayor(t)?get(t,POLICY):BALANCED;}
    static String label(int plan){return switch(plan){case EDUCATION -> "Education";case HEALTH -> "Healthcare";case INFRASTRUCTURE -> "Infrastructure";default -> "Balanced";};}
    static boolean campaign(TownLife.Town t,int day){
        int next=get(t,NEXT);
        return next>day&&next-day<=CAMPAIGN;
    }
    static boolean valid(TownLife.Town t,int slot){
        if(slot==4){
            int resident=get(t,RESIDENT_CANDIDATE);
            if(resident>0){var c=DemographyLife.byIndex(t,resident);return c!=null&&OpinionLife.eligible(c);}
            return !RebirthLife.active(t)&&get(t,PLAYER)==t.player.incarnation&&t.player.alive;
        }
        if(slot<1||slot>3)return false;
        var c=DemographyLife.byIndex(t,get(t,candidate(slot)));
        return c!=null&&OpinionLife.eligible(c)&&!RelationshipBoundaryLife.controlled(t,c);
    }
    static int plan(TownLife.Town t,int slot){return slot==4?get(t,PLAYER_PLAN):slot;}
    static void nominations(TownLife.Town t){
        var pool=t.people.values().stream().filter(c->OpinionLife.eligible(c)&&!RelationshipBoundaryLife.controlled(t,c))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->EducationLife.get(t,c,"social")).reversed()
                        .thenComparingInt(c->c.generationIndex)).toList();
        for(int i=1;i<=3;i++)set(t,candidate(i),i<=pool.size()?pool.get(i-1).generationIndex:0);
        set(t,PLAYER,0);set(t,PLAYER_PLAN,0);set(t,VOTE,0);set(t,VOTER,0);set(t,RESIDENT_CANDIDATE,0);set(t,RESIDENT_VOTER,0);
    }
    static int preference(TownLife.Town t,TownLife.Citizen voter,int slot){
        int p=plan(t,slot);
        int score=switch(p){
            case EDUCATION -> EducationLife.get(t,voter,"academic")/3+(DemographyLife.get(t,voter,"children")>0?18:0);
            case HEALTH -> (100-voter.health)/2+(voter.age>=67?25:0);
            case INFRASTRUCTURE -> (100-OpinionLife.score(t,voter,"city"))/2+EducationLife.get(t,voter,"technical")/4;
            default -> OpinionLife.score(t,voter,"city")/3;
        };
        if(slot==4&&get(t,RESIDENT_CANDIDATE)==0)score+=OpinionLife.score(t,voter,"stall")/5-10;
        else {
            var c=DemographyLife.byIndex(t,slot==4?get(t,RESIDENT_CANDIDATE):get(t,candidate(slot)));
            if(c!=null&&c.generationIndex==get(t,MAYOR))score+=(OpinionLife.score(t,voter,"city")-50)/2;
            if(c==voter)score+=100;
            else if(c!=null){
                var relation=t.existingRelationship(voter.id,c.id);
                if(relation!=null)score+=(relation.trust-45)/3;
            }
        }
        return score+Math.floorMod(voter.personalSeed+slot*37L,11);
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=360)return;
        ensure(t);
        if(get(t,NEXT)==0){set(t,NEXT,day+CAMPAIGN);nominations(t);}
        if(day==get(t,NEXT)-CAMPAIGN&&get(t,LAST)>0)nominations(t);
        if(day<get(t,NEXT)||get(t,LAST)>=day)return;
        int scheduled=get(t,NEXT);
        for(int i=1;i<=4;i++)set(t,votes(i),0);
        for(var voter:t.people.values())if(OpinionLife.eligible(voter)&&!RelationshipBoundaryLife.controlled(t,voter)&&voter.generationIndex!=get(t,RESIDENT_VOTER)){
            int best=0,score=Integer.MIN_VALUE;
            for(int i=1;i<=4;i++)if(valid(t,i)){
                int s=preference(t,voter,i);if(s>score){score=s;best=i;}
            }
            if(best>0)set(t,votes(best),get(t,votes(best))+1);
        }
        int vote=get(t,VOTE);
        var residentVoter=DemographyLife.byIndex(t,get(t,RESIDENT_VOTER));
        boolean explicit=residentVoter!=null?OpinionLife.eligible(residentVoter):!RebirthLife.active(t)&&get(t,VOTER)==t.player.incarnation&&t.player.alive;
        if(explicit&&valid(t,vote))set(t,votes(vote),get(t,votes(vote))+1);
        int winner=0,best=0;
        for(int i=1;i<=4;i++)if(valid(t,i)&&get(t,votes(i))>best){winner=i;best=get(t,votes(i));}
        set(t,MAYOR,winner>=1&&winner<=3?get(t,candidate(winner)):winner==4?get(t,RESIDENT_CANDIDATE):0);
        set(t,OWNER,winner==4&&get(t,RESIDENT_CANDIDATE)==0?t.player.incarnation:0);set(t,POLICY,winner==0?BALANCED:plan(t,winner));
        set(t,LAST,day);set(t,WINNER,winner);set(t,NEXT,Math.addExact(scheduled,TERM));
        String name=winner==4?playerCandidateName(t):winner==0?"Caretaker administration":DemographyLife.byIndex(t,get(t,MAYOR)).name;
        ArchiveLife.record(t,"election:"+scheduled,day,minute,"Municipality",winner==0?"No candidate elected; caretaker administration continues.":name+" won the municipal election with "+best+" votes. Programme: "+label(policy(t))+".");
    }
    static String playerCandidateName(TownLife.Town t){
        var c=DemographyLife.byIndex(t,get(t,RESIDENT_CANDIDATE));
        return c==null?"Visitor #"+get(t,PLAYER):c.name;
    }
    static boolean registered(TownLife.Town t){
        var c=RebirthLife.child(t);
        return RebirthLife.linkedAdult(t,c)?get(t,RESIDENT_CANDIDATE)==c.generationIndex:get(t,PLAYER)==t.player.incarnation;
    }
    static boolean voted(TownLife.Town t){
        var c=RebirthLife.child(t);
        return RebirthLife.linkedAdult(t,c)?get(t,RESIDENT_VOTER)==c.generationIndex:get(t,VOTER)==t.player.incarnation;
    }
    static boolean atComune(TownLife.Town.Citizen c,int minute){return RebirthLife.awakePresent(c)&&c.place==TownLife.Place.OFFICE&&minute>=540&&minute<1020;}
    static String act(TownLife.Town t,String action,int day,int minute){
        var c=RebirthLife.child(t);boolean linked=RebirthLife.linkedAdult(t,c);
        if(linked){if(!atComune(c,minute))throw new IllegalArgumentException("Visit Comune at Offices, 09:00–17:00.");}
        else {if(RebirthLife.active(t))throw new IllegalArgumentException("Only adults can participate.");
            TransportLife.ready(t.player);
            if(t.player.place!=TownLife.Place.OFFICE||minute<540||minute>=1020)throw new IllegalArgumentException("Visit Comune at Offices, 09:00–17:00.");}
        int split=action.lastIndexOf('-');
        int value=Integer.parseInt(action.substring(split+1));
        if(action.startsWith("election-policy-")){
            if(!playerMayor(t)||value<0||value>3)throw new IllegalArgumentException("Only the elected mayor may choose a valid programme.");
            set(t,POLICY,value);
            ArchiveLife.record(t,"policy:"+day+":"+minute,day,minute,"Municipality","The mayor selected the "+label(value)+" budget programme.");
            return "Municipal programme changed to "+label(value)+".";
        }
        if(!campaign(t,day))throw new IllegalArgumentException("The election campaign is not open.");
        if(action.startsWith("election-register-")){
            if(value<1||value>3||valid(t,4)||registered(t))throw new IllegalArgumentException("Choose one programme per campaign.");
            set(t,PLAYER,t.player.incarnation);set(t,PLAYER_PLAN,value);set(t,RESIDENT_CANDIDATE,linked?c.generationIndex:0);
            return "Registered as a candidate for "+label(value)+".";
        }
        if(action.startsWith("election-vote-")){
            if(!valid(t,value)||voted(t)||get(t,RESIDENT_VOTER)>0)throw new IllegalArgumentException("Choose a valid candidate; only one ballot per visitor.");
            set(t,VOTER,t.player.incarnation);set(t,RESIDENT_VOTER,linked?c.generationIndex:0);set(t,VOTE,value);return "Your ballot was recorded.";
        }
        throw new IllegalArgumentException("Unknown municipal election action.");
    }
    record Candidate(int slot,String name,String programme,int votes){}
    record Report(int nextDay,int lastDay,boolean campaigning,boolean registered,boolean voted,boolean playerMayor,String mayor,String programme,List<Candidate> candidates){}
    static Report report(TownLife.Town t,int day){
        var entries=new ArrayList<Candidate>();
        boolean open=campaign(t,day);
        for(int i=1;i<=4;i++){
            if(open&&!valid(t,i))continue;
            if(!open&&(get(t,LAST)==0||get(t,votes(i))==0))continue;
            var person=i==4?null:DemographyLife.byIndex(t,get(t,candidate(i)));
            if(i!=4&&person==null)continue;
            String name=i==4?playerCandidateName(t):person.name;
            entries.add(new Candidate(i,name,label(plan(t,i)),open?0:get(t,votes(i))));
        }
        var mayor=DemographyLife.byIndex(t,get(t,MAYOR));
        return new Report(get(t,NEXT),get(t,LAST),open,registered(t),voted(t),
                playerMayor(t),npcMayor(t)?mayor.name:playerMayor(t)?"Visitor #"+t.player.incarnation:"Caretaker administration",label(policy(t)),List.copyOf(entries));
    }
}
