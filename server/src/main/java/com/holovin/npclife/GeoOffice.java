package com.holovin.npclife;
import java.util.*;

/** Short situational presentation lines for real, currently working office residents.
 * These are not persisted conversations, commitments, relationship changes or extra work. */
final class GeoOffice {
    record Remark(String id,String speakerId,String style,String italian,String english){}
    static String style(TownLife.Citizen c){
        if(c.sociability>=70&&c.curiosity>=60)return "JOKER";
        if(c.carefulness>=75)return "CAREFUL";
        if(c.sociability<=35)return "QUIET";
        if(c.sociability>=65)return "TALKATIVE";
        return "PRACTICAL";
    }
    static List<Remark> remarks(TownLife.Town t,WorldState s,List<GeoProjection.Actor> actors){
        // One brief line every 10 simulated minutes, only in the shared office.
        if(s.minute<480||s.minute>=1080||s.minute%10!=0)return List.of();
        var eligible=actors.stream().filter(a->"WORKING".equals(a.activity())&&"DESK".equals(a.pose()))
            .map(a->t.people.get(a.id())).filter(c->c!=null&&c.alive&&c.age>=18&&c.place==TownLife.Place.OFFICE
                &&c.travelRemaining==0&&c.sociability>35&&c.fatigue<85).sorted(Comparator.comparing(c->c.id)).toList();
        if(eligible.isEmpty())return List.of();
        var c=eligible.get(Math.floorMod(s.day+s.minute/10,eligible.size()));
        String type=style(c),it,en;
        boolean outage=DevOpsLife.unavailable(t);
        int deployed=(int)SoftwareLife.PROJECTS.stream().filter(p->SoftwareLife.deployed(t,p.id())).count();
        if(outage){it="Il catalogo non risponde. Serve una verifica.";en="The catalog is unavailable. It needs checking.";}
        else if(s.minute>=720&&s.minute<750){it="Una pausa al bar dopo il lavoro?";en="A coffee break after work?";}
        else if("JOKER".equals(type)){it="Il caffè è il mio framework preferito!";en="Coffee is my favorite framework!";}
        else if("CAREFUL".equals(type)){it="Controlliamo i test prima di rilasciare.";en="Check the tests before releasing.";}
        else if("TALKATIVE".equals(type)){it="Progetti rilasciati: "+deployed+". Come procede il resto?";en="Projects deployed: "+deployed+". How is the rest going?";}
        else{it="Torniamo al lavoro. Un passo alla volta.";en="Back to work. One step at a time.";}
        return List.of(new Remark(s.day+":"+s.minute+":"+c.id,c.id,type,it,en));
    }
}
