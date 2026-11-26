package com.holovin.npclife;

/** Run with java -ea after test-compile; no model or network is needed. */
public final class ResidentLanguagesSmokeTest {
    public static void main(String[] args){
        assert ResidentLanguages.speaks("marco","it");
        assert !ResidentLanguages.speaks("marco","en");
        assert ResidentLanguages.speaks("elena","it");
        assert ResidentLanguages.speaks("elena","en");
        assert ResidentLanguages.languageQuestion("Parli inglese?","marco","it").equals("Mi dispiace, non parlo inglese.");
        assert ResidentLanguages.languageQuestion("Do you speak English?","elena","en").equals("Yes, I speak English.");
        assert ResidentLanguages.languageQuestion("Come stai?","marco","it")==null;
        assert ResidentLanguages.normalize(" EN ").equals("en");
        boolean invalid=false;
        try {ResidentLanguages.normalize("fr");} catch(IllegalArgumentException ex){invalid=true;}
        assert invalid;
        int english=0;
        for(int i=1;i<=100;i++){
            String id="resident-%04d".formatted(i);
            assert ResidentLanguages.speaks(id,"it");
            if(ResidentLanguages.speaks(id,"en"))english++;
        }
        assert english>0 && english<100;
        System.out.println("ResidentLanguagesSmokeTest: ALL CHECKS PASSED");
    }
}
