package com.holovin.npclife;

import java.util.Locale;

/** Regional speech policy, not the interface locale and not a citizenship rule.
 * A region supplies the default profile for newly generated local residents.
 * The current world has one region; future maps must select their own region.
 */
enum WorldSpeechRegion {
    ITALY("IT","it-IT","Italian"),
    UNITED_STATES("US","en-US","American English"),
    UNITED_KINGDOM("GB","en-GB","British English");

    static final WorldSpeechRegion CURRENT=ITALY; // Vigevano and surrounding districts.
    final String countryCode;
    final String nativeLocale;
    final String speechName;

    WorldSpeechRegion(String countryCode,String nativeLocale,String speechName){
        this.countryCode=countryCode;
        this.nativeLocale=nativeLocale;
        this.speechName=speechName;
    }
    String nativeLanguage(){return Locale.forLanguageTag(nativeLocale).getLanguage();}
    String localeFor(String language){
        if(nativeLanguage().equals(language))return nativeLocale;
        return switch(language){
            case "it" -> "it-IT";
            case "en" -> "en-GB"; // Initial foreign-English teaching convention in Italy.
            default -> throw new IllegalArgumentException("Unsupported dialogue language: "+language);
        };
    }
    String speechNameFor(String language){
        if(nativeLanguage().equals(language))return speechName;
        return "it".equals(language)?"Italian":"British English";
    }
}
