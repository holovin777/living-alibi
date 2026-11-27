package com.holovin.npclife;
import java.util.*;

/** An adapter for the original linked life, not a second playable identity. */
final class GeoLife {
    record View(String phase,String name,int age,String activity,int savings,int maxWaitDays,
                int schoolDays,boolean graduated,String parentA,String parentB,boolean canWalk,int radius,List<GenealogyLife.Relative> familyTree,ChildhoodLife.View upbringing,ChildSearchLife.View childSearch,PassportLife.View passport,ConvictionLife.View conviction){}
    static View view(TownLife.Town t,WorldState s){
        int phase=RebirthLife.phase(t);var c=RebirthLife.child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        return new View(switch(phase){case RebirthLife.CHOICE->"CHOICE";case RebirthLife.CHILDHOOD->"CHILDHOOD";case RebirthLife.LINKED_ADULT->"LINKED_ADULT";default->"ADULT";},
            c==null?null:c.name,c==null?0:c.age,c==null?"":c.activity,c==null?t.player.money:c.money,RebirthLife.maxWaitDays(t),
            c==null?0:EducationLife.get(t,c,"school-days"),c!=null&&EducationLife.get(t,c,"graduate")>0,a==null?null:a.name,b==null?null:b.name,canWalk(t,s),24,GenealogyLife.relatives(t,c),ChildhoodLife.view(t,c),ChildSearchLife.view(t,c,s),PassportLife.view(t,s),ConvictionLife.view(t,c));
    }
    static List<GeoProjection.Choice> choices(TownLife.Town t,WorldState s,GeoProjection map){
        var result=new ArrayList<GeoProjection.Choice>();
        if(!RebirthLife.active(t)||JusticeLife.confined(t))return result;
        var life=view(t,s);var c=RebirthLife.child(t);
        if(life.phase().equals("CHOICE")){
            if(RebirthLife.returnPending(t)){
                add(result,"rebirth-return","Continue next life / Continua la prossima vita");
                return List.copyOf(result);
            }
            if(FirstLifeChoice.pending(t)){
                add(result,"rebirth-first-boy","Boy / Bambino");
                add(result,"rebirth-first-girl","Girl / Bambina");
                return List.copyOf(result);
            }
            if(RebirthLife.randomBirth(t)){
                add(result,"rebirth-wait","Await birth: 1 day / Attendi la nascita: 1 giorno");
                add(result,"rebirth-wait-fast","Await birth: up to 7 days / Attendi la nascita: fino a 7 giorni");
                return List.copyOf(result);
            }
            for(var f:RebirthLife.families(t,s.day))add(result,"rebirth-family-"+f.first()+"-"+f.second(),familyLabel(f));
            add(result,"rebirth-wait","Wait 1 day / Attendi un giorno");
            add(result,"rebirth-adult","Start as a new adult / Inizia adulto");
            return List.copyOf(result);
        }
        if(c==null||!c.alive)return List.of();
        if(NpcJusticeLife.confined(t,c,s))return List.of();
        if(c.age>=13&&RebirthLife.awakePresent(c)){
            for(var belief:ConvictionLife.Belief.values())if(belief!=ConvictionLife.Belief.UNDECIDED&&belief!=ConvictionLife.belief(t,c))
                add(result,"rebirth-conviction-"+belief.name(),"Belief / Convinzione: "+belief.label);
            if(ConvictionLife.canReflect(t,c,s))add(result,"rebirth-conviction-reflect","Quiet reflection / Riflessione · 15 min");
        }
        if(life.phase().equals("CHILDHOOD")){
            if(life.maxWaitDays()>0)add(result,"rebirth-wait","Advance 1 day / Avanza un giorno");
            if(life.maxWaitDays()>1)add(result,"rebirth-wait-fast","Advance up to "+life.maxWaitDays()+" days / Avanza");
            int extended=RebirthLife.extendedWaitDays(t);
            if(extended>0)add(result,"rebirth-wait-extended","Grow up: up to "+extended+" days / Cresci: fino a "+extended+" giorni");
            if(c.age>=18&&life.graduated())add(result,"rebirth-continue","Continue adult life / Vita adulta");
            var child=RebirthLife.childOptions(t,s.day,s.minute);
            if(child!=null){
                add(result,"rebirth-child-hour","Advance 1 hour / Avanza un'ora");
                for(String subject:EducationLife.SUBJECTS)add(result,"rebirth-focus-"+subject,"School focus / Studio: "+subject);
                add(result,"rebirth-focus-balanced","Balanced studies / Studio equilibrato");
                if(child.allowanceAvailable()>0)add(result,"rebirth-allowance","Ask allowance / Paghetta");
                for(var hobby:child.hobbies())add(result,"rebirth-hobby-"+hobby.id(),hobby.label());
                for(String id:child.destinations())add(result,"rebirth-child-travel-"+id,"Outing / Passeggiata → "+TownLife.Place.valueOf(id).label);
                if(child.outing())add(result,"rebirth-child-home","Return home / Torna a casa");
            }
        }else{
            add(result,"rebirth-wait-hour","Advance 1 hour / Avanza un'ora");
            add(result,"rebirth-wait-adult-day","Advance 1 day / Avanza un giorno");
            if(RebirthLife.partner(t,c)!=null)add(result,"rebirth-separate","End partnership / Termina la relazione");
            if(ElectionLife.atComune(c,s.minute)){
                if(CorruptionLife.canMisconduct(t,c,s))add(result,"rebirth-corruption-offense","Abuse public office (crime) / Abuso d’ufficio (reato)");
                if(CorruptionLife.canAudit(t,c,s))add(result,"rebirth-corruption-audit","Investigate budget discrepancy / Indaga sul bilancio");
                if(ElectionLife.campaign(t,s.day)){
                    if(!ElectionLife.valid(t,4)&&!ElectionLife.registered(t))for(int plan=1;plan<=3;plan++)
                        add(result,"rebirth-election-register-"+plan,"Run for mayor / Candidati · "+ElectionLife.label(plan));
                    if(!ElectionLife.voted(t)&&ElectionLife.get(t,ElectionLife.RESIDENT_VOTER)==0)for(var candidate:ElectionLife.report(t,s.day).candidates())
                        add(result,"rebirth-election-vote-"+candidate.slot(),"Vote / Vota "+candidate.name()+" · "+candidate.programme());
                }
                if(ElectionLife.playerMayor(t))for(int plan=0;plan<=3;plan++)
                    add(result,"rebirth-election-policy-"+plan,"Municipal budget / Bilancio · "+ElectionLife.label(plan));
            }
            var adult=RebirthLife.adultOptions(t,s.day);
            var home=RebirthLife.homeOptions(t,s.day,s.minute);
            if(adult!=null){
                add(result,"rebirth-routine","Resume daily routine / Riprendi routine");
                for(String role:adult.jobs())add(result,"rebirth-job-"+role,"Choose occupation / Professione: "+role);
                for(String id:adult.destinations()){
                    var p=TownLife.Place.valueOf(id);
                    if(p!=c.place&&(!id.startsWith("OUTER_")||map.point(p).distance(map.point(c.place))<100))
                        add(result,"rebirth-travel-"+id,"Travel / Vai → "+p.label);
                }
            }
            var cafe=RebirthLife.cafe(t,s.day,s.minute);
            if(cafe!=null&&cafe.open()){
                for(var order:cafe.orders())if(order.available())add(result,"rebirth-cafe-"+order.id(),"Bar Centrale · "+order.label()+" · €"+order.price());
                if(cafe.canRest())add(result,"rebirth-cafe-rest","Coffee break / Pausa · 20 min");
                for(var guest:cafe.guests())add(result,"rebirth-cafe-chat-"+guest.id(),"Chat at cafe / Conversa: "+guest.name());
            }
            for(var offer:HomeMarketLife.offers(t,s.minute)){
                if(offer.canBuy())add(result,"rebirth-property-buy-"+offer.id(),"Buy / Compra "+offer.name()+" · €"+offer.price());
                if(offer.canSell())add(result,"rebirth-property-sell-"+offer.id(),"Sell / Vendi "+offer.name()+" · €"+(offer.resale()-offer.arrears())+" net");
            }
            if(home!=null){
                for(String action:home.actions())add(result,"rebirth-home-"+action,"Home / Casa: "+action);
                if(home.atHome()){
                    add(result,"rebirth-home-exit","Leave home / Esci");
                    if(FamilyDoorLife.canManage(t,c)){
                        add(result,"rebirth-door-lock","Lock family door (1 hour) / Chiudi porta (1 ora)");
                        add(result,"rebirth-door-unlock","Unlock family door / Apri porta");
                    }
                }
            }
        }
        for(var other:RebirthLife.conversationPartners(t,s.day))add(result,"rebirth-chat-"+other.id,"Talk / Parla: "+other.name);
        return List.copyOf(result);
    }
    static String familyLabel(RebirthLife.Family f){
        return f.firstName()+" ("+f.firstAge()+") + "+f.secondName()+" ("+f.secondAge()+")"
                +" · "+f.firstJob()+" / "+f.secondJob()
                +" · "+(f.ownedHome()==null?f.district():f.ownedHome()+" / "+f.district())
                +" · "+f.children().size()+" children / figli"
                +" · €"+f.walletTotal()+" cash / contanti"
                +" · €"+f.familySavings()+" savings / risparmi"
                +" · €"+f.unpaidBills()+" bills / bollette"
                +" · "+(f.sharedHome()?"Shared home / Casa condivisa":"Separate homes / Case separate");
    }
    private static void add(List<GeoProjection.Choice> choices,String id,String label){choices.add(new GeoProjection.Choice(id,label));}
    static boolean canWalk(TownLife.Town t,WorldState s){
        var c=RebirthLife.child(t);
        return RebirthLife.active(t)&&c!=null&&c.age>=6&&RebirthLife.awakePresent(c)&&!JusticeLife.confined(t)&&!NpcJusticeLife.confined(t,c,s)
                &&!EmploymentLife.sick(c,s.day)&&!"IN_CUSTODY".equals(c.activity)
                &&(RebirthLife.linkedChild(t,c)||RebirthLife.linkedAdult(t,c));
    }
    static boolean canTalk(TownLife.Town t,TownLife.Citizen c){
        var self=RebirthLife.child(t);
        return self!=null&&self.age>=3&&RebirthLife.awakePresent(self)&&RebirthLife.awakePresent(c)
                &&RebirthLife.visible(t).contains(c)&&!JusticeLife.confined(t);
    }
    static boolean atHome(TownLife.Citizen c){return c!=null&&c.alive&&c.travelRemaining==0&&c.currentSpace().equals(c.homeUnit);}
    static List<GeoProjection.Actor> household(TownLife.Town t,WorldState s,GeoProjection map){
        var self=RebirthLife.child(t);if(!atHome(self))return List.of();
        var people=RebirthLife.visible(t).stream()
            .filter(c->!c.id.equals(self.id)&&HomeRoutineLife.atHome(c)&&c.homeUnit.equals(self.homeUnit)&&!"SLEEPING".equals(c.activity))
            .sorted(Comparator.comparing(c->c.id)).limit(8).toList();
        return GeoHome.actors(t,s,map.point(self.place),people,c->canTalk(t,c));
    }
}
