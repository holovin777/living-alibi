package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fictional long-term healthcare. Values, scheduling and recovery are gameplay
 * parameters, not medical advice or a real diagnosis. Clinical records are private.
 */
final class ClinicalLife {
    private ClinicalLife(){}
    enum Specialty {
        GENERAL("General practice / Medicina generale"), NEUROLOGY("Neurology / Neurologia"),
        CARDIOLOGY("Cardiology / Cardiologia"), ENDOCRINOLOGY("Endocrinology / Endocrinologia"),
        DENTISTRY("Dentistry / Odontoiatria"), PSYCHOLOGY("Psychology / Psicologia"),
        PSYCHIATRY("Psychiatry / Psichiatria"), SURGERY("Surgery / Chirurgia"),
        REHABILITATION("Rehabilitation / Riabilitazione");
        final String label;Specialty(String label){this.label=label;}
    }
    enum Condition {
        NEUROLOGICAL("Chronic neurological condition / Condizione neurologica cronica",Specialty.NEUROLOGY),
        CARDIOVASCULAR("Chronic cardiovascular condition / Condizione cardiovascolare cronica",Specialty.CARDIOLOGY),
        METABOLIC("Chronic metabolic condition / Condizione metabolica cronica",Specialty.ENDOCRINOLOGY),
        DENTAL("Dental condition / Problema dentale",Specialty.DENTISTRY);
        final String label;final Specialty specialty;
        Condition(String label,Specialty specialty){this.label=label;this.specialty=specialty;}
    }
    static String key(TownLife.Citizen c,String field){return "clinical:"+c.id+":"+field;}
    static int get(TownLife.Town t,TownLife.Citizen c,String field){return t.industryStocks.getOrDefault(key(c,field),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String field,int value){t.industryStocks.put(key(c,field),value);}
    static String field(Condition k,String f){return k.name()+":"+f;}
    static int severity(TownLife.Town t,TownLife.Citizen c,Condition k){return get(t,c,field(k,"severity"));}
    static void inherit(TownLife.Town t,TownLife.Citizen child,TownLife.Citizen a,TownLife.Citizen b){
        for(var k:Condition.values()){
            if(k==Condition.DENTAL)continue;
            // Predisposition only, not a disease or predetermined outcome.
            int risk=(severity(t,a,k)>0||get(t,a,field(k,"risk"))>0?1:0)
                    +(severity(t,b,k)>0||get(t,b,field(k,"risk"))>0?1:0);
            set(t,child,field(k,"risk"),risk);
        }
    }
    static boolean atClinic(TownLife.Citizen c){return RebirthLife.awakePresent(c)&&c.place==TownLife.Place.CLINIC&&!RebirthLife.atHome(c);}
    static boolean canRequest(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&atClinic(c)&&s.minute>=480&&s.minute<1020
            &&get(t,c,"request")==0&&get(t,c,"last-visit")<s.day;
    }
    static void request(TownLife.Town t,TownLife.Citizen c,WorldState s,Specialty specialty){
        if(!canRequest(t,c,s))throw new IllegalArgumentException("Visit the clinic during opening hours.");
        set(t,c,"request",specialty.ordinal()+1);set(t,c,"request-day",s.day);
    }
    static boolean seekingCare(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c.alive&&c.age>=18&&!RelationshipBoundaryLife.controlled(t,c)&&get(t,c,"request")>0
            &&s.minute>=540&&s.minute<1020&&!EmploymentLife.sick(c,s.day);
    }
    static Specialty specialty(TownLife.Citizen doctor){
        // General practitioners remain available; other doctors get stable specializations.
        return Specialty.values()[Math.floorMod(doctor.personalSeed,Specialty.values().length)];
    }
    static boolean onDuty(TownLife.Citizen c,WorldState s){
        return c.alive&&c.employed&&"Doctor".equals(c.occupation)&&!EmploymentLife.sick(c,s.day)
            &&c.presentAt(TownLife.Place.CLINIC)&&"WORKING".equals(c.activity);
    }
    static void daily(TownLife.Town t,WorldState s){
        for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0)continue;
            for(var k:Condition.values()){
                int severity=severity(t,c,k);
                if(severity==0&&c.age>=10){
                    long seed=c.personalSeed^((long)s.day*0x9E3779B97F4A7C15L)^((long)k.ordinal()*0xBF58476D1CE4E5B9L);
                    int risk=1+get(t,c,field(k,"risk"))*2+(c.age>=60?2:0);
                    if(Math.floorMod(seed^(seed>>>29),100000)<risk){
                        severity=15;set(t,c,field(k,"severity"),severity);
                    }
                }
                if(severity==0)continue;
                boolean treated=get(t,c,field(k,"care-until"))>=s.day;
                if(treated){
                    set(t,c,field(k,"severity"),Math.max(k==Condition.DENTAL?0:5,severity-1));
                }else if(Math.floorMod(c.personalSeed+s.day+k.ordinal(),7)==0){
                    set(t,c,field(k,"severity"),Math.min(100,severity+1));
                    if(severity>=45)c.fatigue=Math.min(100,c.fatigue+3);
                    if(severity>=75)c.health=Math.max(1,c.health-1);
                }
            }
            if(c.age>=18&&!RelationshipBoundaryLife.controlled(t,c)&&get(t,c,"request")==0
                    &&get(t,c,"last-visit")<s.day){
                for(var k:Condition.values())if(severity(t,c,k)>=20&&get(t,c,field(k,"care-until"))<s.day){
                    set(t,c,"request",k.specialty.ordinal()+1);set(t,c,"request-day",s.day);break;
                }
            }
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute==360&&t.industryStocks.getOrDefault("clinical:last-day",0)<s.day){
            t.industryStocks.put("clinical:last-day",s.day);daily(t,s);
        }
        if(s.minute<540||s.minute>=1020||s.minute%60!=0)return;
        for(var doctor:t.people.values()){
            if(!onDuty(doctor,s))continue;
            if(get(t,doctor,"last-slot")==s.day*1440+s.minute)continue;
            var patient=t.people.values().stream().filter(c->c!=doctor&&c.alive&&atClinic(c)
                    &&get(t,c,"request")>0&&get(t,c,"last-visit")<s.day
                    &&matches(t,c,doctor)).sorted(Comparator.comparingInt((TownLife.Citizen c)->get(t,c,"request-day"))
                    .thenComparingInt(c->c.health).thenComparing(c->c.id)).findFirst().orElse(null);
            if(patient==null)continue;
            set(t,doctor,"last-slot",s.day*1440+s.minute);
            complete(t,patient,s);
        }
    }
    static boolean matches(TownLife.Town t,TownLife.Citizen patient,TownLife.Citizen doctor){
        int request=get(t,patient,"request");if(request<=0||request>Specialty.values().length)return false;
        var wanted=Specialty.values()[request-1];
        return wanted==Specialty.GENERAL||specialty(doctor)==wanted;
    }
    static void complete(TownLife.Town t,TownLife.Citizen patient,WorldState s){
        var wanted=Specialty.values()[get(t,patient,"request")-1];
        for(var k:Condition.values())if(severity(t,patient,k)>0&&(wanted==Specialty.GENERAL||wanted==k.specialty)){
            set(t,patient,field(k,"diagnosed"),1);
            if(wanted==k.specialty)set(t,patient,field(k,"care-until"),s.day+29);
        }
        if(wanted==Specialty.PSYCHOLOGY||wanted==Specialty.PSYCHIATRY){
            PsychologyLife.adjust(t,patient,"stress",20,-5);
            PsychologyLife.adjust(t,patient,"loneliness",20,-3);
        }
        if(wanted==Specialty.REHABILITATION)patient.fatigue=Math.max(0,patient.fatigue-5);
        // No invented billing: the existing clinic service and doctor payroll remain authoritative.
        set(t,patient,"last-visit",s.day);set(t,patient,"request",0);
        set(t,patient,"visits",Math.addExact(get(t,patient,"visits"),1));
    }
    record Diagnosis(String name,String status,int severity,int followupUntil){}
    record View(String requested,int visits,List<Diagnosis> diagnosed){}
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null)return null;
        var diagnosed=new ArrayList<Diagnosis>();
        for(var k:Condition.values())if(get(t,c,field(k,"diagnosed"))>0){
            int until=get(t,c,field(k,"care-until")),severity=severity(t,c,k);
            diagnosed.add(new Diagnosis(k.label,severity==0?"RESOLVED":until>=s.day?"FOLLOW-UP":"REVIEW DUE",severity,until));
        }
        int r=get(t,c,"request");
        return new View(r>0&&r<=Specialty.values().length?Specialty.values()[r-1].label:"",get(t,c,"visits"),List.copyOf(diagnosed));
    }
}
