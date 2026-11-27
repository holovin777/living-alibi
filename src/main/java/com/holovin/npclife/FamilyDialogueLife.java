package com.holovin.npclife;

/** Family recognition is derived from the actual resident genealogy, not a
 * visitor introduction or a fabricated friendship. Parent styles still differ. */
final class FamilyDialogueLife {
    private FamilyDialogueLife(){}
    static boolean parentOf(TownLife.Town t,TownLife.Citizen parent,TownLife.Citizen child){
        return parent!=null&&child!=null&&parent.generationIndex>0&&child.generationIndex>0
            &&(DemographyLife.get(t,child,"parent-a")==parent.generationIndex
            ||DemographyLife.get(t,child,"parent-b")==parent.generationIndex);
    }
    static boolean family(TownLife.Town t,TownLife.Citizen other){
        var self=RebirthLife.child(t);
        if(self==null||!self.alive||other==null||other==self||other.generationIndex<=0)return false;
        if(parentOf(t,other,self)||parentOf(t,self,other)||RebirthLife.partner(t,self)==other)return true;
        int a=DemographyLife.get(t,self,"parent-a"),b=DemographyLife.get(t,self,"parent-b");
        return (a>0&&(DemographyLife.get(t,other,"parent-a")==a||DemographyLife.get(t,other,"parent-b")==a))
            ||(b>0&&(DemographyLife.get(t,other,"parent-a")==b||DemographyLife.get(t,other,"parent-b")==b));
    }
    static String reply(TownLife.Town t,TownLife.Citizen other,String text,String language){
        if(!family(t,other))return null;
        var self=RebirthLife.child(t);
        boolean it="it".equals(language);
        String said=NaturalDialogue.canonical(text,null);
        String first=self.name.split(" ",2)[0];
        boolean parent=parentOf(t,other,self);
        if(parent&&self.age<3){
            if(self.hunger>=65)return it?"Hai fame, "+first+"? Vediamo cosa ti serve.":"Are you hungry, "+first+"? Let's see what you need.";
            if(self.fatigue>=65)return it?"Sei stanco, "+first+"? È ora di riposare.":"Are you tired, "+first+"? Time to rest.";
            return switch(ParentingLife.style(other)){
                case WARM->it?"Ciao, piccolo mio. Sono qui con te.":"Hello, little one. I'm here with you.";
                case STRICT->it?"Eccomi, "+first+". Ora facciamo con calma.":"I'm here, "+first+". Let's settle down.";
                case INDEPENDENCE->it?"Ciao, "+first+". Ti ascolto.":"Hello, "+first+". I'm listening.";
                case DISTANT->it?"Sì, "+first+"? Sono qui.":"Yes, "+first+"? I'm here.";
            };
        }
        if(said.equalsIgnoreCase("Hello")||said.regionMatches(true,0,"My name is ",0,11))
            return it?"Ciao, "+first+". Come stai?":"Hi, "+first+". How are you?";
        if(said.equalsIgnoreCase("What is your name?")||said.equalsIgnoreCase("Come ti chiami?"))
            return it?"Mi chiamo "+other.name+".":"My name is "+other.name+".";
        if(ConversationMemory.importantQuestion(text)||ConversationMemory.recallQuestion(text)){
            if(parent)return it?"Sei mio figlio o mia figlia, "+first+". Ti conosco da quando eri piccolo.":"You're my child, "+first+". I've known you since you were little.";
            return it?"Sei della mia famiglia, "+first+".":"You're part of my family, "+first+".";
        }
        return null;
    }
}
