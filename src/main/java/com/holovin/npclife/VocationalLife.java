package com.holovin.npclife;

import java.util.List;

/** Fictional vocational training and assessment, not a real electrical license. */
final class VocationalLife {
    private VocationalLife(){}
    static final int LESSONS=3,FEE=5;
    record Question(String prompt,List<String> choices,int correct){}
    static final List<Question> ELECTRIC=List.of(
        new Question("A virtual circuit has an unknown fault. What comes first?",List.of("Isolate the simulated circuit","Replace random components","Ignore the fault"),0),
        new Question("The simulator reports a failed control module. Select the matching component.",List.of("Drain pump","Control module","Water-level sensor"),1),
        new Question("The simulated repair is complete. What is the final step?",List.of("Charge twice","Skip checks","Run the functional test"),2));
    static final List<Question> MECHANICAL=List.of(
        new Question("The washer simulator reports code P. Select the indicated component.",List.of("Drain pump","Water-level sensor","Control module"),0),
        new Question("A spare part is missing from stock. What should you do?",List.of("Invent a part","Wait for a stocked part","Charge without repair"),1),
        new Question("The virtual repair is finished. What confirms success?",List.of("The invoice alone","The technician's opinion","A functional test"),2));
    static String key(TownLife.Town t,String f){return "service-company:vocational:"+t.player.incarnation+":"+f;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(t,f),0);}
    static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(t,f),n);}
    static boolean certified(TownLife.Town t,String course){return get(t,course+":certificate")>0;}
    static boolean campus(TownLife.Town t,WorldState s,int duration){
        return !RebirthLife.active(t)&&t.player.alive&&t.player.travelRemaining==0&&t.player.place==TownLife.Place.SCHOOL
            &&(s.day-1)%7<5&&s.minute>=480&&s.minute+duration<=1080&&t.player.fatigue<85;
    }
    static int course(TownLife.Town t){return get(t,"course");}
    static String id(int n){return n==1?"electrical":"mechanical";}
    static List<Question> questions(int n){return n==1?ELECTRIC:MECHANICAL;}
    static Question question(TownLife.Town t){
        int n=course(t),i=get(t,"exam-index");
        return n==0||i>=3?null:questions(n).get(i);
    }
    record Step(int minutes,String message){}
    static Step act(TownLife.Town t,WorldState s,String action){
        if(action.startsWith("enroll:")){
            String name=action.substring(7);int n=name.equals("electrical")?1:name.equals("mechanical")?2:0;
            if(n==0||course(t)!=0||certified(t,name)||!campus(t,s,1))throw new IllegalArgumentException("Course enrollment unavailable.");
            set(t,"course",n);set(t,"lessons",0);set(t,"exam-index",0);set(t,"exam-score",0);
            return new Step(1,"Enrolled in vocational training.");
        }
        int n=course(t);
        if(n==0)throw new IllegalArgumentException("Enroll in a course first.");
        if(action.equals("lesson")){
            if(!campus(t,s,60)||get(t,"lessons")>=LESSONS||get(t,"lesson-day")>=s.day||t.player.money<FEE)
                throw new IllegalArgumentException("Attend one paid lesson per weekday at school.");
            t.player.money-=FEE;t.employerFunds.merge("school",FEE,Math::addExact);
            set(t,"lesson-day",s.day);set(t,"lessons",get(t,"lessons")+1);
            t.player.engineeringSkill=Math.addExact(t.player.engineeringSkill,1);
            return new Step(60,"Practical lesson completed. Engineering skill +1.");
        }
        if(action.startsWith("answer:")){
            if(!campus(t,s,10)||get(t,"lessons")<LESSONS||get(t,"exam-day")>=s.day)
                throw new IllegalArgumentException("Complete the lessons before taking the exam.");
            int choice;
            try{choice=Integer.parseInt(action.substring(7));}catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid answer.");}
            var q=question(t);
            if(q==null||choice<0||choice>=q.choices().size())throw new IllegalArgumentException("Invalid exam answer.");
            int next=get(t,"exam-index")+1,score=get(t,"exam-score")+(choice==q.correct()?1:0);
            if(next<3){set(t,"exam-index",next);set(t,"exam-score",score);return new Step(10,"Answer recorded.");}
            set(t,"exam-day",s.day);set(t,"exam-index",0);set(t,"exam-score",0);
            if(score==3){set(t,id(n)+":certificate",1);set(t,"course",0);set(t,"lessons",0);return new Step(10,"Passed 3/3. Vocational certificate awarded.");}
            return new Step(10,"Exam score "+score+"/3. Retry on another weekday.");
        }
        throw new IllegalArgumentException("Unknown vocational action.");
    }
    record View(boolean atSchool,boolean electrical,boolean mechanical,String course,int lessons,int required,int fee,boolean canLesson,String prompt,List<String> choices,boolean canEnrollElectrical,boolean canEnrollMechanical){}
    static View view(TownLife.Town t,WorldState s){
        int n=course(t);boolean at=campus(t,s,1);
        var q=n>0&&get(t,"lessons")>=LESSONS&&campus(t,s,10)&&get(t,"exam-day")<s.day?question(t):null;
        return new View(at,certified(t,"electrical"),certified(t,"mechanical"),n==0?"":id(n),get(t,"lessons"),LESSONS,FEE,
            n>0&&campus(t,s,60)&&get(t,"lessons")<LESSONS&&get(t,"lesson-day")<s.day&&t.player.money>=FEE,
            q==null?"":q.prompt(),q==null?List.of():q.choices(),at&&n==0&&!certified(t,"electrical"),at&&n==0&&!certified(t,"mechanical"));
    }
}
