package com.holovin.npclife;

/** On-duty public service is not leisure conversation. Medical supplies are
 * fictional game resources; do not diagnose or name real medications. */
final class MedicalServiceDialogue {
    private MedicalServiceDialogue(){}
    record Reply(String text,String action){}
    static boolean staff(TownLife.Citizen c){
        return c!=null&&c.alive&&c.employed&&c.place==TownLife.Place.CLINIC
                &&c.travelRemaining==0&&"WORKING".equals(c.activity)
                &&java.util.Set.of("Pharmacist","Doctor","Nurse").contains(c.occupation)
                &&!ConversationAvailability.urgent(c);
    }
    static boolean pharmacy(TownLife.Citizen c){return staff(c)&&"Pharmacist".equals(c.occupation);}
    static boolean open(int minute){return minute>=480&&minute<1140;}
    private static boolean says(String n,String... phrases){
        for(String p:phrases)if((" "+n+" ").contains(" "+p+" "))return true;
        return false;
    }
    private static Reply reply(boolean it,String en,String italian){return new Reply(it?italian:en,null);}
    static Reply reply(TownLife.Town t,TownLife.Citizen c,String message,int day,int minute,String language){
        if(!staff(c)||!open(minute))return null;
        boolean it="it".equals(language);
        String n=NaturalDialogue.normalize(message);
        var patient=RebirthLife.child(t);
        var m=RebirthLife.medical(t,day,minute);
        boolean adult=m!=null&&m.atClinic();
        boolean pharmacy=pharmacy(c);
        if(!adult&&patient!=null&&patient.age<18)
            return reply(it,"Hello. Tell me what you need. If you need treatment, let's speak with a parent or a clinician.",
                    "Ciao. Dimmi cosa ti serve. Se hai bisogno di cure, parliamone con un genitore o un medico.");
        if(says(n,"confermo il kit","confirm recovery kit","confermo assistenza","confirm basic care","confermo la visita","confirm consultation")){
            String action=says(n,"confermo il kit","confirm recovery kit")?"collect":
                    says(n,"confermo assistenza","confirm basic care")?"first-aid":"consult";
            boolean available=adult&&(action.equals("collect")?pharmacy&&m.followup()!=null&&m.followup().canCollect():
                    action.equals("first-aid")?pharmacy&&m.canFirstAid():!pharmacy&&"Doctor".equals(c.occupation)&&m.canConsult());
            return available?new Reply(it?"D'accordo.":"All right.",action):
                    reply(it,"That service is not available to you right now. Check the service menu.",
                            "Questo servizio non è disponibile adesso. Controlli le opzioni al banco.");
        }
        if(says(n,"kit di recupero","kit di cura","recovery kit","ritirare il kit")){
            if(!pharmacy)return reply(it,"Please ask the pharmacy about collecting your kit.","Per ritirare il kit, si rivolga alla farmacia.");
            if(adult&&m.followup()!=null&&m.followup().canCollect())
                return reply(it,"Your recovery kit costs EUR 9. Type 'confirm recovery kit' to collect it.",
                        "Il kit di recupero costa 9 euro. Scriva 'confermo il kit' per ritirarlo.");
            return reply(it,"You need a current medical assessment and available funds before collecting a kit.",
                    "Per ritirare un kit serve una valutazione medica valida e il denaro necessario.");
        }
        if(says(n,"assistenza di base","primo soccorso","basic care","first aid")){
            if(pharmacy&&adult&&m.canFirstAid())
                return reply(it,"Basic pharmacy care costs EUR 5. Type 'confirm basic care' to proceed.",
                        "L'assistenza di base costa 5 euro. Scriva 'confermo assistenza' per procedere.");
            return reply(it,"Basic pharmacy care isn't available to you right now. Tell me what you need.",
                    "L'assistenza di base non è disponibile adesso. Mi dica cosa le serve.");
        }
        if(says(n,"vorrei una visita","voglio una visita","visita medica","medical consultation","see a doctor")){
            if(!pharmacy&&"Doctor".equals(c.occupation)&&adult&&m.canConsult())
                return reply(it,"A consultation costs EUR 15. Type 'confirm consultation' to proceed.",
                        "La visita costa 15 euro. Scriva 'confermo la visita' per procedere.");
            return reply(it,"A doctor at the clinic can assess you. Check available consultations at the clinic.",
                    "Un medico dell'ambulatorio può valutarla. Controlli le visite disponibili.");
        }
        if(says(n,"pastiglie","pastiglia","compresse","medicine","medicina","farmaci","farmaco","medicinali","pills","tablets","medication","medicine")){
            if(pharmacy&&adult&&m.followup()!=null&&m.followup().canCollect())
                return reply(it,"Do you mean your recovery kit? It costs EUR 9. If so, type 'confirm recovery kit'. Otherwise tell me what you need.",
                        "Intende il kit di recupero? Costa 9 euro. Se sì, scriva 'confermo il kit'. Altrimenti mi dica cosa le serve.");
            return reply(it,"What do you need the tablets for? Do you have a prescription or would you like to discuss your symptoms?",
                    "Per cosa le servono le pastiglie? Ha una prescrizione oppure vuole parlarmi dei suoi sintomi?");
        }
        if(says(n,"mal di testa","mal di pancia","febbre","mi fa male","sto male","ho dolore","headache","stomach ache","fever","i feel sick","pain")){
            return reply(it,"I understand. A clinician can assess your symptoms in the game. Would you like information about a consultation?",
                    "Capisco. Un medico può valutare i suoi sintomi. Vuole informazioni per una visita?");
        }
        if(says(n,"buongiorno","buonasera","salve","ciao","hello","hi","good morning","good evening","mi aiuta","puo aiutarmi","can you help"))
            return reply(it,"Hello. How can I help you today?","Buongiorno. Mi dica, come posso aiutarla?");
        if(says(n,"grazie","thank you","thanks"))
            return reply(it,"You're welcome. Let me know if you need anything else.","Prego. Mi dica pure se ha bisogno di altro.");
        return null;
    }
}
