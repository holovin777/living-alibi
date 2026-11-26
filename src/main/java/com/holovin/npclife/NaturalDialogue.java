package com.holovin.npclife;

import java.util.Locale;
import java.util.regex.Pattern;

/** Bounded offline natural-language understanding. Never executes text as commands. */
final class NaturalDialogue {
    private NaturalDialogue() {}
    private static final Pattern INTRO=Pattern.compile(
            "(?iu)^(?:(?:hello|hi|hey|ciao|salve|привіт|добрий день)[, !]+)?(?:my name is|mi chiamo|il mio nome è|мене звати|моє ім'я)\\s+([\\p{L}][\\p{L}\\p{M}'’\\-]*(?:\\s+[\\p{L}][\\p{L}\\p{M}'’\\-]*){0,3})[.!]?\\s*$");
    static String normalize(String input) {
        if(input==null)return "";
        return input.toLowerCase(Locale.ROOT).replace('’','\'')
                .replaceAll("[àáâä]", "a").replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i").replaceAll("[òóôö]", "o")
                .replaceAll("[ùúûü]", "u").replaceAll("[^\\p{L}\\p{N}']+", " ").strip();
    }
    private static boolean has(String text,String... phrases) {
        String padded=" "+text+" ";
        for(String phrase:phrases)if(padded.contains(" "+phrase+" "))return true;
        return false;
    }
    private static boolean eq(String text,String... phrases) {
        for(String phrase:phrases)if(text.equals(phrase))return true;
        return false;
    }
    static String canonical(String raw,String previous) {
        String n=normalize(raw);
        // Consent, money and item transfers retain their explicit existing wording.
        if(eq(n,"you can tell people my name","ask for a 10 eur loan","repay personal loan",
                "offer bread","offer water","ask for bread","ask for water",
                "any news about visitors","any public news for the newspaper"))return raw;
        var intro=INTRO.matcher(raw.strip());
        if(intro.matches()) {
            String alias=intro.group(1);
            if(!has(normalize(alias),"and","but","how","what","e","ma","come","і","але","як"))
                return "My name is "+alias+".";
        }
        // Preserve old explicit introductions, including validation in PlayerLife.
        if(raw.regionMatches(true,0,"My name is ",0,11))return raw;
        if(has(n,"leave me alone","lasciami in pace","залиш мене в спокої"))return "Leave me alone!";
        if(has(n,"what is your name","what's your name","whats your name","your name","who are you",
                "come ti chiami","come si chiama","come posso chiamarti","qual e il tuo nome","chi sei",
                "як тебе звати","як вас звати","як тебе називати","твоє ім'я","хто ти"))return "What is your name?";
        if(has(n,"where do you live","where is your home","where's your house","dove abiti","dove vivi","dove stai di casa",
                "де ти живеш","де ви живете","де твій дім"))return "Where do you live?";
        if(has(n,"what do you remember","do you remember me","do you know me","ti ricordi di me","mi riconosci","cosa ricordi di me",
                "ти мене пам'ятаєш","пам'ятаєш мене","що ти пам'ятаєш"))return "What do you remember about me?";
        if(has(n,"i'm here for you","i am here for you","you can count on me","ti sono vicino","sono qui per te","puoi contare su di me",
                "я поруч","можеш на мене розраховувати"))return "I'm here for you.";
        if(has(n,"i'm sorry","i am sorry","i apologize","mi dispiace","scusami","chiedo scusa","вибач","пробач","перепрошую"))return "I'm sorry.";
        if(has(n,"looking for a job","looking for work","find a job","find work","need a job","any jobs","job openings",
                "cerco lavoro","sto cercando lavoro","trovare lavoro","hai lavoro per me","c'e lavoro","assumono",
                "шукаю роботу","знайти роботу","потрібна робота","є робота"))return "@jobs";
        if(has(n,"where do you work","where is your workplace","dove lavori","dove lavora","де ти працюєш","де ви працюєте"))return "@workplace";
        if(has(n,"what do you do","what is your job","what's your job","what do you work as","your profession","your occupation",
                "che lavoro fai","che lavoro fa","cosa fai nella vita","di cosa ti occupi","che mestiere fai",
                "ким ти працюєш","яка твоя професія","що ти робиш по життю"))return "What do you do?";
        if(has(n,"what are you doing","what are you up to","cosa stai facendo","che stai facendo","cosa fai adesso",
                "що ти робиш","чим займаєшся зараз"))return "@activity";
        if(has(n,"how are you","how's it going","how are things","how do you feel","come stai","come va","come si sente","come ti senti",
                "як справи","як ти","як себе почуваєш","як почуваєшся"))return "How are you?";
        if(has(n,"what time","what's the time","che ore sono","che ora e","котра година","скільки часу"))return "@time";
        if(has(n,"what do you like","your hobbies","what are your goals","what do you want in life","cosa ti piace","quali sono i tuoi hobby","cosa desideri",
                "що тобі подобається","які твої захоплення","яка твоя мета"))return "@goal";
        if(has(n,"i am sad","i'm sad","i feel lonely","i am lonely","i'm lonely","sono triste","mi sento solo","mi sento sola",
                "мені сумно","мені самотньо","я сумний"))return "@comfort";
        if(has(n,"i am hungry","i'm hungry","i need food","ho fame","mi serve da mangiare","я голодний","хочу їсти"))return "@food";
        if(has(n,"i am thirsty","i'm thirsty","i need water","ho sete","мені хочеться пити","хочу пити"))return "@drink";
        if(has(n,"thank you","thanks","grazie","дякую","спасибі"))return "@thanks";
        if(eq(n,"bye","goodbye","see you","see you later","ciao ciao","arrivederci","a dopo","до побачення","бувай"))return "@bye";
        if(has(n,"where is","where's","how do i get to","how can i get to","dove si trova","dov'e","come arrivo","come raggiungo",
                "де знаходиться","де є","як дістатися","як пройти")||n.startsWith("де ")) {
            var place=mentionedPlace(n);
            if(place!=null)return "@place:"+place.name();
        }
        if(eq(n,"and where","where","where exactly","e dove","dove","dove esattamente","а де","де","де саме")) {
            String prior=previous==null?"":canonical(previous,null);
            if(eq(prior,"What do you do?","@workplace"))return "@workplace";
            if(prior.startsWith("@place:"))return prior;
            if(eq(prior,"@jobs"))return "@jobs";
        }
        if(eq(n,"and you","what about you","e tu","e lei","а ти","а ви")) {
            String prior=previous==null?"":canonical(previous,null);
            if(eq(prior,"@comfort","How are you?"))return "How are you?";
            if(eq(prior,"@goal"))return "@goal";
        }
        if(eq(n,"hello","hello there","hi","hi there","hey","good morning","good afternoon","good evening","ciao","salve","buongiorno","buonasera",
                "привіт","добрий день","добрий ранок","добрий вечір"))return "Hello";
        return raw;
    }
    private static TownLife.Place mentionedPlace(String n) {
        if(has(n,"pharmacy","farmacia","аптека","аптеку","clinic","clinica","ambulatorio","лікарня","лікарню"))return TownLife.Place.CLINIC;
        if(has(n,"market","supermarket","grocery store","mercato","supermercato","negozio","ринок","магазин"))return TownLife.Place.MARKET;
        if(has(n,"school","scuola","школа","школу"))return TownLife.Place.SCHOOL;
        if(has(n,"bakery","panetteria","forno","пекарня","пекарню"))return TownLife.Place.BAKERY;
        if(has(n,"cafe","coffee shop","bar","caffe","кафе"))return TownLife.Place.CAFE;
        if(has(n,"library","biblioteca","бібліотека","бібліотеку"))return TownLife.Place.LIBRARY;
        if(has(n,"workshop","officina","майстерня","майстерню"))return TownLife.Place.WORKSHOP;
        if(has(n,"office","ufficio","офіс"))return TownLife.Place.OFFICE;
        if(has(n,"park","parco","парк"))return TownLife.Place.PARK;
        if(has(n,"factory","fabbrica","завод"))return TownLife.Place.FACTORY;
        if(has(n,"town center","city centre","centro","центр"))return TownLife.Place.CENTRO;
        return null;
    }
    static String reply(TownLife.Town town,TownLife.Citizen resident,PlayerLife.Contact contact,
                        String intent,int day,int minute) {
        return switch(intent) {
            case "@jobs" -> "Look for job notices on your phone or ask at the market. They can tell you whether work is available.";
            case "@workplace" -> resident.employed&&resident.workplace!=null
                    ? "My workplace is "+resident.workplace.label+"." : "I don't have a workplace at the moment.";
            case "@activity" -> switch(resident.activity) {
                case "WORKING" -> "I'm working at the moment.";
                case "STUDYING" -> "I'm studying at the moment.";
                case "SHOPPING" -> "I'm getting some supplies.";
                case "SOCIALIZING" -> "I'm taking some time to talk to people.";
                case "JOB_SEARCH" -> "I'm looking for work myself.";
                default -> "I'm taking a moment for myself.";
            };
            case "@time" -> String.format(Locale.ROOT,"It's %02d:%02d.",minute/60,minute%60);
            case "@goal" -> switch(resident.goal==null?"":resident.goal) {
                case "MAKE_FRIENDS" -> "I'd like to make more friends.";
                case "LEARN_SKILLS" -> "I'd like to learn something new.";
                case "SAVE_MONEY","STABILITY" -> "I'd like a more stable life.";
                case "STAY_HEALTHY","IMPROVE_HEALTH" -> "I'm trying to take better care of myself.";
                default -> "I'm trying to find a good balance in life.";
            };
            case "@comfort" -> contact.trust>=55||resident.sociability>=60
                    ? "I'm sorry you're feeling that way. Would you like to tell me more?"
                    : "I'm sorry to hear that. I hope things get better.";
            case "@food" -> "You can find food at the market or bakery.";
            case "@drink" -> "Try the market for drinking water, or check the park fountain.";
            case "@thanks" -> contact.trust>=55?"You're very welcome.":"You're welcome.";
            case "@bye" -> contact.trust>=55?"See you again. Take care!":"Goodbye.";
            default -> intent.startsWith("@place:") ? directions(town,resident,intent) : null;
        };
    }
    private static String directions(TownLife.Town town,TownLife.Citizen resident,String intent) {
        TownLife.Place destination;
        try {destination=TownLife.Place.valueOf(intent.substring(7));}
        catch(IllegalArgumentException e){return null;}
        return resident.place==destination?"You're already in that district."
                :"Look for "+destination.label+" on your town map.";
    }
}
