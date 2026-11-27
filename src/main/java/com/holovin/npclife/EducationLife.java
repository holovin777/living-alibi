package com.holovin.npclife;
import java.util.List;

/** Persistent skills, attendance and aptitude-based employment. */
final class EducationLife {
    private EducationLife(){}
    static final String LESSONS="education:lessons", GRADUATES="education:graduates", EXPERIENCE="education:experience", SCHOOL_SERVICE="school-services";
    static final List<String> SUBJECTS=List.of("technical","social","academic","commerce");
    static final List<String> FIELDS=List.of("technical","social","academic","commerce","school-days","last-school","last-work","graduate","fitness","hobby-days","last-hobby","allowance-week","allowance-total","last-family-time","dirt","furniture-level","interior-kitchen","interior-bedroom","interior-children","interior-laundry","domestic-home-dirt","domestic-clothes-dirt","domestic-last-day","domestic-power","domestic-water","wardrobe-worn","wardrobe-last-day","wardrobe-casual-owned","wardrobe-casual-wear","wardrobe-summer-owned","wardrobe-summer-wear","wardrobe-winter-owned","wardrobe-winter-wear","wardrobe-work-owned","wardrobe-work-wear","medical-exam-day","medical-assessed-health","medical-prescription","medical-supplies","medical-collected","medical-completed","medical-last-recovery","sport-endurance","sport-flexibility","sport-strength","sport-karate","sport-sessions","sport-last-day","dojo-rank","dojo-kihon","dojo-kata","dojo-kumite","dojo-sessions","dojo-community-last-day","dojo-last-rei","dojo-match-partner","dojo-match-day","dojo-match-round","dojo-match-score","dojo-match-opponent","last-cafe-rest","last-consult","last-first-aid","course","course-lessons","last-course","diploma-engineering","diploma-programming","diploma-medicine","diploma-investigation","last-engineering-order","java-onboarded","developer-rank","developer-xp","developer-releases","developer-audits","last-developer-audit","devops-trained","devops-resolved","it-registered","it-cash","it-rent-until","it-last-retail","it-last-utility","it-contracts","it-tax-paid","it-employee","it-employer","it-delegated","it-last-assignment");
    static String key(TownLife.Citizen c,String f){return "education:"+c.id+":"+f;}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return get(t,key(c,f));}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){set(t,key(c,f),n);}
    static void ensure(TownLife.Town t){
        for(String k:List.of(LESSONS,GRADUATES,EXPERIENCE))t.industryStocks.putIfAbsent(k,0);
        for(var c:t.people.values())if(c.generationIndex>0)ensureResident(t,c);
    }
    static String subject(String job){
        return switch(job){
            case "Solar plant operator","Hydropower operator","Thermal plant operator","Service technician","Electrician","Engineer","Programmer","Mechanic","Miner","Factory machinist","Transport driver","Independent fabricator","Company freight driver" -> "technical";
            case "Nurse","Barista" -> "social";
            case "Teacher","Librarian","Doctor","Detective" -> "academic";
            case "Market vendor","Private stall seller","Office clerk","Baker","Pizzaiolo","Gelataio",PostalLife.OCCUPATION -> "commerce";
            default -> "academic";
        };
    }
    static int aptitude(TownLife.Citizen c,String f){
        return 1+Math.floorMod(Long.rotateLeft(c.personalSeed,7+13*SUBJECTS.indexOf(f)),5);
    }
    static void ensureResident(TownLife.Town t,TownLife.Citizen c){
        for(String f:FIELDS){
            int initial=0;
            if(SUBJECTS.contains(f)&&c.age>=18)initial=10+aptitude(c,f)*3+(c.employed&&f.equals(subject(c.occupation))?25:0);
            t.industryStocks.putIfAbsent(key(c,f),initial);
        }
    }
    static void learn(TownLife.Town t,TownLife.Citizen c,String f,int gain){
        set(t,c,f,Math.min(100,get(t,c,f)+gain));
    }
    static void graduate(TownLife.Town t,TownLife.Citizen c){
        ensureResident(t,c);
        if(get(t,c,"graduate")>0)return;
        set(t,c,"graduate",1);set(t,GRADUATES,Math.addExact(get(t,GRADUATES),1));
    }
    static void attendance(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!c.alive||c.generationIndex<=0||c.travelRemaining>0)return;
        ensureResident(t,c);
        int day=clock.day;
        if(DemographyLife.child(c)&&c.age>=6&&c.place==TownLife.Place.SCHOOL&&"STUDYING".equals(c.activity)
                &&clock.minute==600&&(day-1)%7<5&&get(t,c,"last-school")<day){
            int funds=t.employerFunds.getOrDefault("school",0);
            if(funds<1)return;
            t.employerFunds.put("school",funds-1);
            t.employerFunds.merge(SCHOOL_SERVICE,1,Math::addExact);
            set(t,c,"last-school",day);
            set(t,c,"school-days",Math.addExact(get(t,c,"school-days"),1));
            set(t,LESSONS,Math.addExact(get(t,LESSONS),1));
            String f=RebirthLife.focus(t,c);
            if(f==null)f=SUBJECTS.get(Math.floorMod(day+c.generationIndex,SUBJECTS.size()));
            learn(t,c,f,aptitude(c,f)>=4?2:1);
        }
        if(DemographyLife.workingAge(c)&&c.employed&&"WORKING".equals(c.activity)&&clock.minute%60==0
                &&c.place==c.workplace&&get(t,c,"last-work")<day){
            set(t,c,"last-work",day);
            learn(t,c,subject(c.occupation),1);
            set(t,EXPERIENCE,Math.addExact(get(t,EXPERIENCE),1));
        }
    }
    static boolean availableRole(TownLife.Town t,TownLife.Citizen c,String job){
        String f=subject(job);
        int minimum=switch(job){case "Doctor" -> 65;case "Engineer","Programmer","Detective" -> 45;case "Teacher","Nurse","Journalist","Police officer" -> 35;case "Librarian","Mechanic","Factory machinist" -> 20;default -> 0;};
        if(get(t,c,f)<minimum)return false;
        String diploma=switch(job){case "Engineer" -> "engineering";case "Programmer" -> "programming";case "Doctor" -> "medicine";case "Detective" -> "investigation";default -> null;};
        if(diploma!=null&&get(t,c,"diploma-"+diploma)==0)return false;
        if(PoliceLife.OFFICER.equals(job)&&c.age<21)return false;
        if("Doctor".equals(job)&&(c.age<25||get(t,c,"social")<35))return false;
        if(NewspaperLife.OCCUPATION.equals(job)&&(t.employerFunds.getOrDefault(NewspaperLife.JOB,0)<NewspaperLife.REPORTER_PAY*30||c.money<200))return false;
        if(PostalLife.OCCUPATION.equals(job))return PostalLife.funded(t)&&
                t.people.values().stream().filter(x->x.alive&&x.employed&&PostalLife.postalWorker(x)).count()<2;
        if(RetailLife.foodRole(job))return t.employerFunds.getOrDefault(RetailLife.foodAccount(job),0)>=RetailLife.FOOD_WAGE && t.people.values().stream().noneMatch(x->x.alive&&x.employed&&job.equals(x.occupation));
        String employer=IndustryLife.companyFor(job);
        if(employer!=null&&t.employerFunds.getOrDefault(employer,0)<IndustryLife.HOURLY_WAGE)return false;
        return t.people.values().stream().filter(x->x.alive&&x.employed&&job.equals(x.occupation)).count()<(employer==null?5:2);
    }
    static int preferredRole(TownLife.Town t,TownLife.Citizen c,String[] jobs){
        ensureResident(t,c);
        int best=-1,bestScore=Integer.MIN_VALUE;
        for(int i=0;i<jobs.length;i++){
            String job=jobs[i],f=subject(job);
            int skill=get(t,c,f);
            if(!availableRole(t,c,job))continue;
            long occupied=t.people.values().stream().filter(x->x.alive&&x.employed&&job.equals(x.occupation)).count();
            int score=skill*2+aptitude(c,f)*8-(int)occupied*12;
            if("LEARN_SKILLS".equals(c.goal)&&f.equals("academic"))score+=8;
            if("MAKE_FRIENDS".equals(c.goal)&&f.equals("social"))score+=8;
            if("SAVE_MONEY".equals(c.goal)&&f.equals("commerce"))score+=8;
            score+=PersonalityLife.careerBias(c,f);
            score+=Math.floorMod(c.personalSeed+i*31,7);
            if(score>bestScore){bestScore=score;best=i;}
        }
        return best;
    }
    record Course(String id,String label,String subject,int lessons,String career){}
    static final List<Course> COURSES=List.of(
            new Course("engineering","Engineering","technical",60,"Engineer"),
            new Course("programming","Computer science","technical",60,"Programmer"),
            new Course("medicine","Medicine","academic",120,"Doctor"),
            new Course("investigation","Investigation","academic",60,"Detective"));
    record CourseView(String id,String label,int required,int completed,boolean diploma,boolean canEnroll,String career){}
    record College(boolean atCampus,boolean open,String enrolled,int completed,int required,int fee,boolean canStudy,List<CourseView> courses){}
    static Course enrolled(TownLife.Town t,TownLife.Citizen c){
        int i=get(t,c,"course");return i>0&&i<=COURSES.size()?COURSES.get(i-1):null;
    }
    static College college(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!RebirthLife.linkedAdult(t,c))return null;
        boolean at=RebirthLife.awakePresent(c)&&c.place==TownLife.Place.SCHOOL&&!RebirthLife.atHome(c);
        boolean open=minute>=480&&minute<1080&&(day-1)%7<5&&!EmploymentLife.sick(c,day);
        var active=enrolled(t,c);int progress=get(t,c,"course-lessons");
        var courses=COURSES.stream().map(p->new CourseView(p.id(),p.label(),p.lessons(),
                p.equals(active)?progress:get(t,c,"diploma-"+p.id())>0?p.lessons():0,
                get(t,c,"diploma-"+p.id())>0,at&&open&&active==null&&get(t,c,"diploma-"+p.id())==0,p.career())).toList();
        return new College(at,open,active==null?null:active.id(),progress,active==null?0:active.lessons(),2,
                at&&open&&minute+60<=1080&&active!=null&&get(t,c,"last-course")<day&&c.money>=2&&c.fatigue<85,courses);
    }
    static String enroll(TownLife.Town t,int day,int minute,String id){
        var c=RebirthLife.requireAdult(t);var college=college(t,day,minute);
        if(college.courses().stream().noneMatch(p->p.id().equals(id)&&p.canEnroll()))
            throw new IllegalArgumentException("Enroll at the college during opening hours; finish your current course first.");
        int index=0;for(int i=0;i<COURSES.size();i++)if(COURSES.get(i).id().equals(id))index=i+1;
        set(t,c,"course",index);set(t,c,"course-lessons",0);
        return "Enrolled in "+COURSES.get(index-1).label()+".";
    }
    static String study(TownLife.Town t,int day,int minute){
        var c=RebirthLife.requireAdult(t);var college=college(t,day,minute);
        if(!college.canStudy())throw new IllegalArgumentException("Attend class on campus during opening hours, once per weekday.");
        var course=enrolled(t,c);
        int funds=Math.addExact(t.employerFunds.getOrDefault("school",0),2);
        c.money-=2;t.employerFunds.put("school",funds);
        learn(t,c,course.subject(),2);
        if("medicine".equals(course.id())||"investigation".equals(course.id()))learn(t,c,"social",1);
        c.fatigue=Math.min(100,c.fatigue+5);
        set(t,c,"last-course",day);
        int progress=get(t,c,"course-lessons")+1;
        if(progress>=course.lessons()){
            set(t,c,"diploma-"+course.id(),1);set(t,c,"course",0);set(t,c,"course-lessons",0);
            return "Graduated in "+course.label()+".";
        }
        set(t,c,"course-lessons",progress);
        return "Completed lesson "+progress+" of "+course.lessons()+".";
    }
    record Report(int lessons,int graduates,int workDays,int pupils,int schoolFunds){}
    static Report report(TownLife.Town t){
        int pupils=(int)t.people.values().stream().filter(c->c.alive&&DemographyLife.child(c)&&c.age>=6).count();
        return new Report(get(t,LESSONS),get(t,GRADUATES),get(t,EXPERIENCE),pupils,t.employerFunds.getOrDefault("school",0));
    }
}
