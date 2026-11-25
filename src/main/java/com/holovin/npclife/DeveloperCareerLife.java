package com.holovin.npclife;

import java.util.List;

/** Personal career progression and recurring, read-only checks against current city data. */
final class DeveloperCareerLife {
    private DeveloperCareerLife(){}
    record Level(String name,int xp,int technical,int audits,int wage){}
    static final List<Level> LEVELS=List.of(
        new Level("Junior",0,45,0,18),
        new Level("Middle",4,55,2,24),
        new Level("Senior",10,65,6,32),
        new Level("Tech Lead",20,75,12,40),
        new Level("Software Architect",35,85,20,50));
    record Task(String title,String prompt,int expected){}
    record Audit(String title,String prompt,int wage,boolean available){}
    record Career(String rank,int xp,int technical,int releases,int audits,String nextRank,
                  int requiredXp,int requiredTechnical,int requiredAudits,boolean canPromote,Audit audit){}
    static int rank(TownLife.Town t,TownLife.Citizen c){
        return Math.max(0,Math.min(LEVELS.size()-1,EducationLife.get(t,c,"developer-rank")));
    }
    static void increment(TownLife.Town t,TownLife.Citizen c,String field,int by){
        EducationLife.set(t,c,field,Math.addExact(EducationLife.get(t,c,field),by));
    }
    static void release(TownLife.Town t,TownLife.Citizen c){
        increment(t,c,"developer-xp",3);increment(t,c,"developer-releases",1);
    }
    static Task task(TownLife.Town t,int rank){
        int bread=t.retailStock.getOrDefault("bread",0),water=t.retailStock.getOrDefault("water",0);
        var items=RetailLife.PRODUCTS;
        return switch(rank){
            case 0 -> new Task("Inventory reconciliation","The inventory API reports bread="+bread+", water="+water+". Return the total units of these two products.",bread+water);
            case 1 -> {
                var data=items.stream().map(p->p.id()+"="+t.retailStock.getOrDefault(p.id(),0)).toList();
                int count=(int)items.stream().filter(p->t.retailStock.getOrDefault(p.id(),0)<3).count();
                yield new Task("SQL report verification","Count products whose stock is strictly below 3, including zero: "+String.join(", ",data),count);
            }
            case 2 -> {
                var data=items.stream().map(p->p.id()+"="+t.retailStock.getOrDefault(p.id(),0)+"/"+p.capacity()).toList();
                int gap=items.stream().mapToInt(p->Math.max(0,p.capacity()-t.retailStock.getOrDefault(p.id(),0))).sum();
                yield new Task("Capacity review","For each product, current stock/capacity is shown. Sum nonnegative free shelf spaces: "+String.join(", ",data),gap);
            }
            case 3 -> {
                var orders=TechnicalWorkLife.orders(t);
                int total=orders.stream().mapToInt(o->Math.max(0,70-o.condition)).sum();
                yield new Task("Incident triage","Target condition is 70 for this review. Sum only positive shortfalls for all assets: "+String.join(", ",orders.stream().map(o->o.id+"="+o.condition).toList()),total);
            }
            default -> {
                var status=UtilityLife.status(t);
                var source=t.naturalResources.get("river-water");
                int stock=source==null?0:source.stock;
                int limit=SoftwareLife.deployed(t,"water-control")?6:4;
                int free=Math.max(0,status.cleanWaterCapacity()-status.cleanWater());
                int expected=status.fountainWorking()?Math.min(limit,Math.min(stock,free)):0;
                yield new Task("Architecture capacity assessment","Pump operational="+status.fountainWorking()+", daily transfer limit="+limit+", river stock="+stock+", free tank capacity="+free+". Predict next transfer if nothing else changes; a broken pump transfers zero.",expected);
            }
        };
    }
    static boolean ready(TownLife.Town t,TownLife.Citizen c,int day,int minute,int duration){
        return SoftwareLife.linkedProgrammer(t,c)&&EducationLife.get(t,c,"java-onboarded")>0
            &&RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==TownLife.Place.OFFICE
            &&!EmploymentLife.sick(c,day)&&c.fatigue<85&&minute>=480&&minute+duration<=1080;
    }
    static Career report(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!SoftwareLife.linkedProgrammer(t,c))return null;
        int r=rank(t,c),xp=EducationLife.get(t,c,"developer-xp"),audits=EducationLife.get(t,c,"developer-audits");
        int skill=EducationLife.get(t,c,"technical");
        var level=LEVELS.get(r);var next=r+1<LEVELS.size()?LEVELS.get(r+1):null;
        var task=task(t,r);
        return new Career(level.name(),xp,skill,EducationLife.get(t,c,"developer-releases"),audits,
            next==null?null:next.name(),next==null?0:next.xp(),next==null?0:next.technical(),next==null?0:next.audits(),
            next!=null&&ready(t,c,day,minute,15)&&xp>=next.xp()&&skill>=next.technical()&&audits>=next.audits(),
            new Audit(task.title(),task.prompt(),level.wage(),ready(t,c,day,minute,60)
                &&EducationLife.get(t,c,"last-developer-audit")<day&&t.employerFunds.getOrDefault("city-maintenance",0)>=level.wage()));
    }
    static String promote(TownLife.Town t,int day,int minute){
        var c=RebirthLife.requireAdult(t);var career=report(t,day,minute);
        if(career==null||!career.canPromote())throw new IllegalArgumentException("Promotion requires the listed experience, skills and completed audits.");
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        EducationLife.set(t,c,"developer-rank",rank(t,c)+1);
        return "Career review passed. Promoted to "+career.nextRank()+".";
    }
    static RebirthLife.HomeAction audit(TownLife.Town t,int day,int minute,String answer){
        var c=RebirthLife.requireAdult(t);var career=report(t,day,minute);
        if(career==null||!career.audit().available())throw new IllegalArgumentException("Daily audit unavailable: check hours, funds and daily limit.");
        int submitted;
        try{if(answer==null||answer.length()>10)throw new NumberFormatException();submitted=Integer.parseInt(answer.strip());}
        catch(NumberFormatException e){throw new IllegalArgumentException("Enter a whole number.");}
        if(submitted!=task(t,rank(t,c)).expected())throw new IllegalArgumentException("Audit result does not match current city data. Recheck the numbers.");
        int wage=career.audit().wage(),wallet=Math.addExact(c.money,wage);
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        t.employerFunds.merge("city-maintenance",-wage,Integer::sum);c.money=wallet;
        EducationLife.set(t,c,"last-developer-audit",day);
        increment(t,c,"developer-audits",1);increment(t,c,"developer-xp",1);
        EducationLife.learn(t,c,"technical",1);c.fatigue=Math.min(100,c.fatigue+4);
        return new RebirthLife.HomeAction(60,"Audit accepted. Earned EUR "+wage+".");
    }
}
