package com.holovin.npclife;
import java.util.*;

/** Reuses existing education, software, career, incident and IT business reports.
 * No source code or shell command is executed by the geographic adapter. */
final class GeoWork {
    record Input(String prefix,String label,String prompt,String starter,int maxLength){}
    record View(EducationLife.College college,JavaTicketLife.Workspace workspace,List<SoftwareLife.ProjectView> projects,
                List<GeoProjection.Choice> actions,List<Input> inputs){}
    static View view(TownLife.Town t,WorldState s){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c)||JusticeLife.confined(t))
            return new View(null,null,List.of(),List.of(),List.of());
        var college=EducationLife.college(t,s.day,s.minute);
        var workspace=JavaTicketLife.workspace(t,s.day,s.minute);
        var projects=SoftwareLife.projects(t,s.day,s.minute);
        var actions=new ArrayList<GeoProjection.Choice>();
        var inputs=new ArrayList<Input>();
        if(college!=null){
            for(var course:college.courses())if(course.canEnroll())
                add(actions,"rebirth-enroll-"+course.id(),"Enroll / Iscriviti: "+course.label());
            if(college.canStudy())add(actions,"rebirth-study","Attend lesson / Lezione · €"+college.fee());
        }
        if(workspace!=null){
            if(workspace.canOnboard())add(actions,"rebirth-java-onboard","Java onboarding / Formazione · 15 min");
            for(var project:projects)if(project.canAdvance()){
                switch(project.command()){
                    case "code" -> workspace.tickets().stream().filter(ticket->ticket.id().equals(project.id())).findFirst()
                        .ifPresent(ticket->inputs.add(new Input("rebirth-java-submit:"+ticket.id()+":",ticket.number(),
                            ticket.signature()+"\n"+ticket.requirement()+"\n"+ticket.examples(),ticket.starter(),180)));
                    case "branch","commit","pr","merge" -> add(actions,"rebirth-git:"+project.command()+":"+project.id(),project.title()+" · "+project.nextAction()+" · "+project.minutes()+" min");
                    case "review","deploy" -> add(actions,"rebirth-software-"+project.id(),project.title()+" · "+project.nextAction()+" · "+project.minutes()+" min");
                    default -> {}
                }
            }
            var career=workspace.career();
            if(career!=null){
                if(career.canPromote())add(actions,"rebirth-developer-promote","Promotion / Promozione → "+career.nextRank());
                if(career.audit().available())inputs.add(new Input("rebirth-developer-audit:",career.audit().title(),career.audit().prompt()+"\nReward / Compenso: €"+career.audit().wage(),"",10));
            }
            var devops=workspace.devops();
            if(devops!=null){
                if(devops.canTrain())add(actions,"rebirth-devops-train","DevOps onboarding · 30 min");
                if(devops.canRun())inputs.add(new Input("rebirth-devops-command:","DevOps console",devops.output()+"\n"+devops.guide(),"",120));
            }
            var business=workspace.companyStatus();
            if(business!=null){
                if(business.canRegister())add(actions,"rebirth-it:register","Register IT company / Registra azienda · €50");
                if(business.canInvest())add(actions,"rebirth-it:invest","Invest / Investi · €20");
                if(business.canWithdraw())add(actions,"rebirth-it:withdraw","Withdraw / Preleva · €20");
                if(business.canRent())add(actions,"rebirth-it:rent","Rent office / Affitta ufficio · €20 / 7 days");
                for(var offer:business.offers())if(offer.available())
                    inputs.add(new Input("rebirth-it:deliver:"+offer.id()+":",offer.title(),offer.prompt()+"\nRevenue €"+offer.gross()+" · tax €"+offer.tax(),"",10));
                var staff=business.staff();
                if(staff!=null){
                    for(var candidate:staff.candidates())add(actions,"rebirth-it:hire:"+candidate.id(),"Hire / Assumi "+candidate.name()+" · technical "+candidate.technical());
                    if(staff.canDismiss())add(actions,"rebirth-it:dismiss","End support contract / Termina contratto");
                    for(String id:staff.delegatable())add(actions,"rebirth-it:delegate:"+id,"Delegate / Delega "+id+" · €12");
                }
            }
        }
        return new View(college,workspace,projects,List.copyOf(actions),List.copyOf(inputs));
    }
    static boolean accepts(View view,String action){
        if(action==null)return false;
        if(view.actions().stream().anyMatch(a->a.id().equals(action)))return true;
        for(var input:view.inputs())if(action.startsWith(input.prefix())){
            String value=action.substring(input.prefix().length());
            return !value.isBlank()&&value.length()<=input.maxLength();
        }
        return false;
    }
    private static void add(List<GeoProjection.Choice> a,String id,String label){a.add(new GeoProjection.Choice(id,label));}
}
