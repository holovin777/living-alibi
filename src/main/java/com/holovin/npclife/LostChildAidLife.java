package com.holovin.npclife;

/** Lost-child requests are safety events, not ordinary small talk.
 * An adult must meet the child; there are no remote location revelations or teleports. */
final class LostChildAidLife {
    private LostChildAidLife(){}

    private static boolean says(String normalized,String... phrases){
        for(var phrase:phrases)if(normalized.contains(" "+phrase+" "))return true;
        return false;
    }

    static boolean request(TownLife.Town town,String message){
        if(!RebirthLife.active(town))return false;
        var child=RebirthLife.child(town);
        if(child==null||!child.alive||child.age<3||child.age>=18)return false;
        String n=" "+NaturalDialogue.normalize(message)+" ";
        if(says(n,"non sono perso","non sono persa","i am not lost","i'm not lost"))return false;
        return says(n,
                "mi sono perso","mi sono persa","sono perso","sono persa",
                "mi sono smarrito","mi sono smarrita",
                "non trovo i miei genitori","non trovo mia mamma","non trovo mio papa",
                "non trovo la mamma","non trovo il papa","ho perso la mamma","ho perso i genitori",
                "i am lost","i'm lost","i got lost","i can't find my parents",
                "i cannot find my parents","i can't find my mum","i can't find my mom",
                "i can't find my dad","i lost my parents",
                "я загубився","я загубилась","я загубилася","загубився","загубилась",
                "не можу знайти маму","не можу знайти батьків");
    }

    static String inferredLanguage(String message){
        String n=" "+NaturalDialogue.normalize(message)+" ";
        return says(n,"mi sono perso","mi sono persa","sono perso","sono persa",
                "non trovo i miei genitori","non trovo mia mamma","non trovo mio papa",
                "non trovo la mamma","non trovo il papa","ho perso la mamma","ho perso i genitori",
                "mi sono smarrito","mi sono smarrita")?"it":"en";
    }

    static String respond(TownLife.Town town,TownLife.Citizen adult,WorldState clock,String language){
        boolean it="it".equals(language);
        var child=RebirthLife.child(town);
        if(child==null||!child.alive)throw new IllegalArgumentException("No child needs assistance.");
        if(adult.age<18)
            return it?"Sono anch'io minorenne. Chiediamo aiuto a un adulto o a un agente."
                    :"I'm a child too. Let's ask an adult or a police officer.";
        if(FamilyDialogueLife.parentOf(town,adult,child)){
            ChildSearchLife.foundWithParent(town,child,adult,clock);
            return it?"Sono qui con te. Ti riconosco. Restiamo insieme e decidiamo come tornare a casa."
                    :"I'm here with you. I know you. Let's stay together and work out how to get home.";
        }
        if(ConversationAvailability.urgent(adult))
            return it?"Non sto bene e non riesco ad accompagnarti. Chiedi aiuto a un agente o al personale di un negozio."
                    :"I'm unwell and can't accompany you. Please ask a police officer or a shop worker for help.";
        // People differ, but most adults recognize a child's request for safety.
        boolean helps=PoliceLife.staff(adult)||FamilyDialogueLife.family(town,adult)
                ||adult.carefulness>=35||adult.sociability>=45;
        if(!helps)
            return it?"Mi dispiace, non posso fermarmi. Vai dal personale del negozio o chiedi a un agente di aiutarti."
                    :"I'm sorry, I can't stay. Please ask a shop worker or a police officer to help.";
        boolean already=ChildSearchLife.view(town,child,clock).policeReported();
        ChildSearchLife.reportedBy(town,child,adult,clock);
        if(PoliceLife.officer(adult)){
            var search=ChildSearchLife.view(town,child,clock);
            if("WAITING_FOR_FAMILY".equals(search.status())&&search.searchingParent()!=null)
                return it?"Ti abbiamo trovato. Un tuo genitore è stato avvisato e verrà qui. Rimaniamo in un luogo visibile."
                        :"We've found you. One of your parents has been notified and is coming here. Let's stay somewhere visible.";
            return it?"Ho registrato la segnalazione. Cercheremo di contattare i tuoi familiari. Non sappiamo ancora quando arriveranno."
                    :"I've recorded the report. We'll try to contact your family. We don't yet know when they will arrive.";
        }
        if(already)
            return it?"La polizia è già stata avvisata. Puoi restare in un posto visibile mentre cerchiamo aiuto."
                    :"The police have already been notified. You can stay somewhere visible while we find help.";
        return it?"Certo, ti aiuto. Ho avvisato la polizia. Puoi restare qui in un posto visibile mentre cerchiamo i tuoi familiari."
                :"Of course I'll help. I've notified the police. You can stay here somewhere visible while we look for your family.";
    }
}
