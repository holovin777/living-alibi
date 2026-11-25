package com.holovin.npclife;

/** Educational incident state machine; commands are matched, never executed. */
final class DevOpsLife {
    private DevOpsLife(){}
    static final String CAUSE="devops:cause", STEP="devops:step", DAY="devops:last-incident";
    static final String HEALTH="curl -I http://localhost/retail/health";
    static void ensure(TownLife.Town t){
        for(String key:java.util.List.of(CAUSE,STEP,DAY))t.industryStocks.putIfAbsent(key,0);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static boolean unavailable(TownLife.Town t){return get(t,CAUSE)>0&&get(t,STEP)<3;}
    static void tick(TownLife.Town t,int day,int minute){
        if(minute==360&&day>=3&&day%3==0&&day>get(t,DAY)
                &&get(t,CAUSE)==0&&SoftwareLife.deployed(t,"retail-api")){
            set(t,DAY,day);set(t,CAUSE,1+Math.floorMod(day/3-1,3));set(t,STEP,0);
        }
    }
    record Console(boolean trained,boolean canTrain,boolean active,boolean canRun,String status,
                   String guide,String output,int resolved,int wage){}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c){
        return SoftwareLife.linkedProgrammer(t,c)&&DeveloperCareerLife.rank(t,c)>=1&&EducationLife.get(t,c,"technical")>=60;
    }
    static Console report(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!SoftwareLife.linkedProgrammer(t,c))return null;
        boolean trained=EducationLife.get(t,c,"devops-trained")>0;
        int cause=get(t,CAUSE),step=get(t,STEP);
        boolean active=cause>0;
        String guide="Requires Middle Developer and technical skill 60. Onboarding covers health checks, Linux services, Docker Compose and reverse proxies.";
        String output="Retail catalog not deployed.";
        if(SoftwareLife.deployed(t,"retail-api"))output="Retail API healthy (200).";
        if(trained){
            if(!active)guide="Monitor the retail service. Incidents can occur every three simulated days after deployment.";
            else if(step==0){guide="Start with the simulated health endpoint. Use curl -I to request headers: http://localhost/retail/health";output="Monitoring alert: retail unavailable.";}
            else if(step==1){
                guide=switch(cause){case 1->"Inspect containers: docker compose ps";case 2->"Inspect reverse proxy logs: journalctl -u nginx -n 20";default->"Inspect disk usage: df -h";};
                output="HTTP/1.1 503 Service Unavailable";
            }else if(step==2){
                guide=switch(cause){case 1->"Start the stopped service using docker compose up -d retail";case 2->"The simulated known-good proxy configuration was restored by automation. Reload it using systemctl reload nginx";default->"The simulated disk is full of rotated application logs. Use the training-only command journalctl --vacuum-size=100M to free journal space.";};
                output=switch(cause){case 1->"NAME retail · STATUS Exited";case 2->"nginx: old upstream configuration active";default->"/var/log/journal 100% · journal storage full";};
            }else{guide="Verify recovery using curl -I against the same health endpoint.";output="Recovery applied; monitoring awaits verification.";}
        }
        boolean ready=eligible(t,c)&&DeveloperCareerLife.ready(t,c,day,minute,5);
        return new Console(trained,!trained&&eligible(t,c)&&DeveloperCareerLife.ready(t,c,day,minute,30),
            active,trained&&active&&ready&&(step<3||t.employerFunds.getOrDefault("city-maintenance",0)>=30),
            !SoftwareLife.deployed(t,"retail-api")?"NOT DEPLOYED":unavailable(t)?"OUTAGE":active?"VERIFY RECOVERY":"HEALTHY",
            guide,output,EducationLife.get(t,c,"devops-resolved"),30);
    }
    static String train(TownLife.Town t,int day,int minute){
        var c=RebirthLife.requireAdult(t);var console=report(t,day,minute);
        if(console==null||!console.canTrain())throw new IllegalArgumentException("DevOps requires Middle rank, technical 60 and office onboarding.");
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        EducationLife.set(t,c,"devops-trained",1);
        return "Completed DevOps onboarding. Commands are simulated; never run automatically on a real host.";
    }
    static RebirthLife.HomeAction command(TownLife.Town t,int day,int minute,String command){
        var c=RebirthLife.requireAdult(t);var console=report(t,day,minute);
        if(console==null||!console.canRun())throw new IllegalArgumentException("No available DevOps incident step.");
        int cause=get(t,CAUSE),step=get(t,STEP);
        String expected=step==0||step==3?HEALTH:step==1?
            switch(cause){case 1->"docker compose ps";case 2->"journalctl -u nginx -n 20";default->"df -h";}:
            switch(cause){case 1->"docker compose up -d retail";case 2->"systemctl reload nginx";default->"journalctl --vacuum-size=100M";};
        if(command==null||command.length()>120||!command.strip().equals(expected))
            throw new IllegalArgumentException("Command does not match this simulated incident step. Read the diagnostic guide.");
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        if(step<3){set(t,STEP,step+1);return new RebirthLife.HomeAction(5,"Command accepted. "+report(t,day,minute).output());}
        int wallet=Math.addExact(c.money,30);
        t.employerFunds.merge("city-maintenance",-30,Integer::sum);c.money=wallet;
        set(t,CAUSE,0);set(t,STEP,0);
        DeveloperCareerLife.increment(t,c,"devops-resolved",1);
        DeveloperCareerLife.increment(t,c,"developer-xp",2);
        EducationLife.learn(t,c,"technical",1);
        return new RebirthLife.HomeAction(5,"HTTP/1.1 200 OK. Incident closed. Earned EUR 30.");
    }
}
