package com.holovin.npclife;

import java.util.List;

/** Fictional driving qualification. No claim of real Italian licensing rules. */
final class DrivingSchoolLife {
    private DrivingSchoolLife(){}
    static final List<String> CATEGORIES=List.of("AM","A","B");
    static final int THEORY_SESSIONS=2, PRACTICE_SESSIONS=2;
    static final int THEORY_FEE=5, PRACTICE_FEE=8, ISSUE_FEE=4;
    record Question(String prompt,List<String> answers,int correct){}
    static final List<Question> QUESTIONS=List.of(
        new Question("Red light / Semaforo rosso",List.of("Continue / Prosegui","Stop before the line / Fermati prima della linea","Accelerate / Accelera"),1),
        new Question("Pedestrian on crossing / Pedone sulle strisce",List.of("Stop and yield / Fermati e dai precedenza","Sound horn and continue / Suona e prosegui","Overtake / Sorpassa"),0),
        new Question("Wet road / Strada bagnata",List.of("Reduce distance / Riduci distanza","Keep accelerating / Accelera","Slow down and increase gap / Rallenta e aumenta distanza"),2));
    static String key(TownLife.Town t,String category,String field){
        if(!CATEGORIES.contains(category))throw new IllegalArgumentException("Unknown driving category.");
        return "driving-school:"+t.player.incarnation+":"+category+":"+field;
    }
    static int get(TownLife.Town t,String category,String field){return t.industryStocks.getOrDefault(key(t,category,field),0);}
    static void set(TownLife.Town t,String category,String field,int value){t.industryStocks.put(key(t,category,field),value);}
    static boolean valid(TownLife.Town t,String category){return get(t,category,"issued")>0;}
    static String required(String kind){return switch(kind){case "scooter" -> "AM";case "motorcycle" -> "A";default -> "B";};}
    static boolean allowed(TownLife.Town t,String kind){
        return valid(t,required(kind))||(kind.equals("scooter")&&(valid(t,"A")||valid(t,"B")));
    }
    record Progress(String category,int theory,int question,int correct,boolean theoryPassed,int practice,boolean licensed,int issueDay){}
    static List<Progress> view(TownLife.Town t){
        return CATEGORIES.stream().map(c->new Progress(c,get(t,c,"theory"),get(t,c,"question"),get(t,c,"correct"),get(t,c,"passed")>0,get(t,c,"practice"),valid(t,c),get(t,c,"issued"))).toList();
    }
    static boolean ready(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0&&!t.player.cycling
            &&!IntercomLife.insideVisit(t,s.day,s.minute)&&s.minute>=480&&s.minute<1080
            &&(PassportLife.visitorAge(t,t.player.incarnation,s.day)<0||PassportLife.visitorAge(t,t.player.incarnation,s.day)>=18);
    }
    record Option(String id,String label){}
    static List<Option> options(TownLife.Town t,WorldState s){
        var result=new java.util.ArrayList<Option>();
        if(!ready(t,s))return result;
        for(String c:CATEGORIES){
            if(valid(t,c))continue;
            int theory=get(t,c,"theory"),q=get(t,c,"question"),practice=get(t,c,"practice");
            boolean passed=get(t,c,"passed")>0;
            String prefix="license:"+c+":";
            if(t.player.place==TownLife.Place.SCHOOL){
                if(theory<THEORY_SESSIONS&&s.minute+30<=1080&&t.player.money>=THEORY_FEE)
                    result.add(new Option(prefix+"lesson","Driving theory / Teoria "+c+" ("+theory+"/"+THEORY_SESSIONS+") · €"+THEORY_FEE));
                else if(theory>=THEORY_SESSIONS&&!passed&&q<QUESTIONS.size()&&s.minute+5<=1080){
                    var question=QUESTIONS.get(q);
                    for(int i=0;i<question.answers().size();i++)result.add(new Option(prefix+"answer"+i,c+" · "+question.prompt()+" → "+question.answers().get(i)));
                }
            }
            if(t.player.place==TownLife.Place.WORKSHOP&&passed&&practice<PRACTICE_SESSIONS&&s.minute+45<=1080&&t.player.money>=PRACTICE_FEE)
                result.add(new Option(prefix+"practice","Supervised practice / Guida assistita "+c+" ("+practice+"/"+PRACTICE_SESSIONS+") · €"+PRACTICE_FEE));
            if(t.player.place==TownLife.Place.OFFICE&&passed&&practice>=PRACTICE_SESSIONS&&s.minute+10<=1080&&t.player.money>=ISSUE_FEE)
                result.add(new Option(prefix+"issue","Issue license / Rilascio patente "+c+" · €"+ISSUE_FEE));
        }
        return List.copyOf(result);
    }
    record Result(String message,int minutes){}
    static Result action(TownLife.Town t,WorldState s,String category,String action){
        if(options(t,s).stream().noneMatch(o->o.id().equals("license:"+category+":"+action)))throw new IllegalArgumentException("Driving school action unavailable.");
        if(action.equals("lesson")){
            pay(t,"city-maintenance",THEORY_FEE);set(t,category,"theory",get(t,category,"theory")+1);
            return new Result("Driving theory lesson completed.",30);
        }
        if(action.startsWith("answer")){
            int q=get(t,category,"question");
            int score=get(t,category,"correct")+(Integer.parseInt(action.substring(6))==QUESTIONS.get(q).correct()?1:0);
            q++;
            if(q==QUESTIONS.size()){
                boolean passed=score==QUESTIONS.size();
                set(t,category,"passed",passed?1:0);set(t,category,"question",0);set(t,category,"correct",0);
                return new Result(passed?"Theory passed. Visit the workshop for supervised practice.":"Theory not passed. Try the quiz again.",5);
            }
            set(t,category,"question",q);set(t,category,"correct",score);
            return new Result("Answer recorded.",5);
        }
        if(action.equals("practice")){
            pay(t,"technician",PRACTICE_FEE);set(t,category,"practice",get(t,category,"practice")+1);
            return new Result("Supervised driving practice completed.",45);
        }
        pay(t,"city-maintenance",ISSUE_FEE);set(t,category,"issued",Math.max(1,s.day));
        return new Result("Driving license "+category+" issued.",10);
    }
    private static void pay(TownLife.Town t,String account,int amount){
        if(t.player.money<amount)throw new IllegalArgumentException("Insufficient funds.");
        t.player.money-=amount;t.employerFunds.merge(account,amount,Math::addExact);
    }
}
