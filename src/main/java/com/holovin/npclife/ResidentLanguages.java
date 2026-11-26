package com.holovin.npclife;

import java.util.Locale;
import java.util.Set;

/** Stable language abilities, separate from the client interface locale.
 * Existing Italian-town residents have Italian as their native language.
 * English is a second language for a deterministic subset; never depends on
 * current occupation, age, UI locale or a model's guesses.
 */
final class ResidentLanguages {
    private ResidentLanguages() {}
    static String normalize(String language) {
        if(language==null)return "it";
        String value=language.strip().toLowerCase(Locale.ROOT);
        if(!Set.of("it","en").contains(value))
            throw new IllegalArgumentException("Choose Italian or English.");
        return value;
    }
    static boolean speaks(String residentId,String language) {
        return "it".equals(language)||("en".equals(language)
                && ("elena".equals(residentId) || (!"marco".equals(residentId)
                && Math.floorMod(("language:"+residentId).hashCode(),100)<35)));
    }
    static String misunderstanding() {return "Mi dispiace, non parlo inglese.";}
    static String languageQuestion(String text,String residentId,String language) {
        String normalized=NaturalDialogue.normalize(text);
        if(Set.of("parli inglese","parla inglese","sai parlare inglese","do you speak english",
                "can you speak english").contains(normalized))
            return speaks(residentId,"en")?(language.equals("it")?"Sì, parlo anche inglese.":"Yes, I speak English."):
                    misunderstanding();
        if(Set.of("parli italiano","parla italiano","do you speak italian","can you speak italian").contains(normalized))
            return language.equals("it")?"Sì, certo.":"Yes, I speak Italian.";
        return null;
    }
}
