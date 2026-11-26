package com.holovin.npclife;

import java.util.Locale;
import java.util.Set;

/** Stable language abilities, independent of UI locale and current occupation. */
final class ResidentLanguages {
    enum Level { NONE, A1, A2, B1, B2, C1, NATIVE }
    private ResidentLanguages() {}
    static String normalize(String language) {
        if(language==null)return WorldSpeechRegion.CURRENT.nativeLanguage();
        String value=language.strip().toLowerCase(Locale.ROOT).replace('_','-');
        return switch(value){
            case "it","it-it" -> "it";
            case "en","en-us","en-gb" -> "en";
            default -> throw new IllegalArgumentException("Choose Italian or English.");
        };
    }
    static Level level(WorldSpeechRegion region,String residentId,String language){
        if(region.nativeLanguage().equals(language))return Level.NATIVE;
        // The initial Italian population is local; multilingual immigrants
        // will need an explicit persisted profile, not a change of map.
        if(region==WorldSpeechRegion.ITALY && "en".equals(language)){
            if("marco".equals(residentId))return Level.NONE;
            if("elena".equals(residentId))return Level.B2;
            int seed=Math.floorMod(("language:"+residentId).hashCode(),100);
            if(seed<15)return Level.B1;
            if(seed<30)return Level.B2;
            if(seed<35)return Level.C1;
        }
        return Level.NONE;
    }
    static boolean speaks(WorldSpeechRegion region,String residentId,String language){
        return level(region,residentId,language)!=Level.NONE;
    }
    static boolean speaks(String residentId,String language){
        return speaks(WorldSpeechRegion.CURRENT,residentId,language);
    }
    static String misunderstanding(WorldSpeechRegion region,String requestedLanguage){
        if(region==WorldSpeechRegion.ITALY)return "Mi dispiace, non parlo inglese.";
        return "Sorry, I don't speak Italian.";
    }
    static String languageQuestion(WorldSpeechRegion region,String text,String residentId,String language){
        String normalized=NaturalDialogue.normalize(text);
        String asked=null;
        if(Set.of("parli inglese","parla inglese","sai parlare inglese","do you speak english",
                "can you speak english").contains(normalized))asked="en";
        if(Set.of("parli italiano","parla italiano","do you speak italian","can you speak italian").contains(normalized))asked="it";
        if(asked==null)return null;
        if(!speaks(region,residentId,asked))return language.equals("it")
                ?"Mi dispiace, non parlo "+(asked.equals("en")?"inglese.":"italiano.")
                :"Sorry, I don't speak "+(asked.equals("en")?"English.":"Italian.");
        if(language.equals("it"))return asked.equals("it")?"Sì, certo.":"Sì, parlo anche inglese.";
        return asked.equals("it")?"Yes, I speak Italian.":"Yes, I speak English.";
    }
    static String languageQuestion(String text,String residentId,String language){
        return languageQuestion(WorldSpeechRegion.CURRENT,text,residentId,language);
    }
}
