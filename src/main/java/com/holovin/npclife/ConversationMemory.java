package com.holovin.npclife;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Private, bounded conversation memory belonging to one contact in one player life. */
final class ConversationMemory {
    static final int LIMIT=32;
    private static final int PROMPT_LIMIT=10;
    private static final ObjectMapper JSON=new ObjectMapper();
    record Turn(int day,int minute,String language,String player,String reply) {}
    private ConversationMemory(){}
    static void remember(PlayerLife.Contact contact,int day,int minute,String language,String player,String reply){
        contact.dialogue.add(new Turn(day,minute,language==null?"und":language,clip(player,1000),clip(reply,1200)));
        while(contact.dialogue.size()>LIMIT)contact.dialogue.remove(0);
    }
    private static String clip(String text,int max){
        if(text==null)return "";
        if(text.length()<=max)return text;
        int end=Character.isHighSurrogate(text.charAt(max-1))?max-1:max;
        return text.substring(0,end);
    }
    private static Set<String> tokens(String text){
        var words=new HashSet<String>();
        for(String word:NaturalDialogue.normalize(text).split(" "))
            if(word.length()>=4&&!Set.of("what","that","with","have","about","come","cosa","sono","della","delle","questo","quello","ricordi","remember").contains(word))words.add(word);
        return words;
    }
    /** Latest six plus up to four older turns with overlapping words, chronological. */
    static List<Turn> context(PlayerLife.Contact contact,String message){
        if(contact==null)return List.of();
        var turns=contact.dialogue;
        int recentStart=Math.max(0,turns.size()-6);
        var query=tokens(message);
        var scored=new ArrayList<int[]>();
        for(int i=0;i<recentStart;i++){
            var words=tokens(turns.get(i).player());
            words.addAll(tokens(turns.get(i).reply()));
            words.retainAll(query);
            if(!words.isEmpty())scored.add(new int[]{i,words.size()});
        }
        scored.sort((a,b)->a[1]!=b[1]?Integer.compare(b[1],a[1]):Integer.compare(b[0],a[0]));
        var indices=new java.util.TreeSet<Integer>();
        for(int i=recentStart;i<turns.size();i++)indices.add(i);
        for(var score:scored){if(indices.size()>=PROMPT_LIMIT)break;indices.add(score[0]);}
        return indices.stream().map(turns::get).toList();
    }
    static boolean recallQuestion(String message){
        return Set.of("what did we talk about","what did we talk about last time",
                "do you remember what we talked about","what did i tell you last time",
                "di cosa abbiamo parlato","di cosa abbiamo parlato l'ultima volta",
                "ti ricordi di cosa abbiamo parlato","cosa ti ho detto l'ultima volta").contains(NaturalDialogue.normalize(message));
    }
    /** Exact recall of the last recorded utterance; never invent a missing turn. */
    static String recall(PlayerLife.Contact contact,String language){
        boolean it="it".equals(language);
        if(contact==null||contact.dialogue.isEmpty()){
            if(contact!=null&&contact.lastHeard!=null&&!contact.lastHeard.isBlank())
                return (it?"Ricordo che mi hai detto: «":"I remember you said: “")+clip(contact.lastHeard,500)+(it?"».":"”.");
            return it?"Non ricordo una conversazione precedente.":"I don't remember an earlier conversation.";
        }
        for(int i=contact.dialogue.size()-1;i>=0;i--){
            var t=contact.dialogue.get(i);
            if(recallQuestion(t.player()))continue;
            return (it?"Ricordo che mi hai detto: «":"I remember you said: “")+clip(t.player(),500)+(it?"».":"”.");
        }
        return it?"Non ricordo altro.":"I don't remember anything else.";
    }
    static String encode(PlayerLife.Contact contact) throws java.sql.SQLException {
        var array=JSON.createArrayNode();
        for(var t:contact.dialogue){
            var row=array.addObject();row.put("day",t.day());row.put("minute",t.minute());
            row.put("language",t.language());row.put("player",t.player());row.put("reply",t.reply());
        }
        return array.toString();
    }
    static void decode(PlayerLife.Contact contact,String stored) throws java.sql.SQLException {
        if(stored==null||stored.isBlank())return;
        try {
            var array=JSON.readTree(stored);
            if(!array.isArray())throw new IllegalArgumentException("Expected conversation array");
            for(var t:array)remember(contact,t.path("day").asInt(),t.path("minute").asInt(),
                    t.path("language").asText("und"),t.path("player").asText(""),t.path("reply").asText(""));
        } catch(Exception ex){throw new java.sql.SQLException("Invalid conversation memory",ex);}
    }
}
