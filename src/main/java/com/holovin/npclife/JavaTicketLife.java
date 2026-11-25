package com.holovin.npclife;

import java.util.List;
import java.util.Map;

/** Real business rules, exercised with a deliberately tiny integer Java-expression subset.
 * No compilation, reflection, file access, arbitrary JVM execution or external processes.
 */
final class JavaTicketLife {
    private JavaTicketLife(){}
    static int waterUnits(int requested,int stock,int freeCapacity){
        return Math.min(requested,Math.min(stock,freeCapacity));
    }
    static int factoryUnits(int stock,int limit){
        return Math.min(stock,Math.max(0,limit));
    }
    record Ticket(String id,String number,String signature,String requirement,String examples,String language,String starter){}
    static final List<Ticket> TICKETS=List.of(
        new Ticket("water-control","BD-101 · Utility service","int waterUnits(int requested, int stock, int freeCapacity)",
            "Return the number of units to pump. All inputs are nonnegative. Never exceed the requested amount, river stock or free tank capacity.",
            "Examples: (6,2,9) -> 2; (6,20,1) -> 1; (6,20,0) -> 0.","Java","return 0;"),
        new Ticket("factory-planning","BD-102 · Production service","int factoryUnits(int stock, int limit)",
            "Return the allowed production quantity. Stock is nonnegative; a negative limit must produce zero. Never exceed stock.",
            "Examples: (7,3) -> 3; (1,4) -> 1; (5,-2) -> 0.","Java","return 0;"),
        new Ticket("retail-api","BD-103 · Spring Boot REST","@RestController class RetailController { ... public List<RetailView> catalog() {...} }",
            "The controller already has @RequestMapping(\"/api/v1\"). Supply the Spring annotation for a GET endpoint at /api/v1/retail/catalog. It must not mutate inventory.",
            "GET reads a resource. @GetMapping specifies the method-level path; the class-level path is added automatically.","Spring Boot","@GetMapping(\"/TODO\")"),
        new Ticket("retail-sql","BD-104 · SQLite reporting","products(id TEXT, stock INTEGER, price INTEGER)",
            "Select only product IDs whose stock is strictly below 3. Sort by id ascending. The result must include sold-out products and exclude stock equal to 3.",
            "Example rows: bread=0, water=3, eggs=2. Expected IDs: bread, eggs. Use SELECT, FROM, WHERE and ORDER BY.","SQL","SELECT id FROM products"));
    static final String INTRO="Welcome to Borgo Digital, a fictional Java consulting team serving local utilities and factories. "
        +"A product owner describes a business need in a ticket. Read the acceptance criteria, write the return statement, run tests, then review and deploy. "
        +"Use integer variables, literals, +, -, *, /, %, parentheses and Math.min/Math.max. "
        +"Example: return Math.max(0, value);. The server checks a bounded expression, not arbitrary Java. "
        +"Git keeps changes isolated in a feature branch. Commit records changes; a pull request invites review; CI checks the change; merge updates main. Deployment releases it to users. "
        +"Spring Boot maps HTTP GET requests to controller methods; JSON is the response format. SQL reads structured tables with SELECT, WHERE and ORDER BY. "
        +"SQL tickets run against disposable test databases, not the saved city. This is an in-game Git workflow, not a connection to a real repository. Only a delivered contract pays.";
    record Workspace(String company,String intro,boolean onboarded,boolean canOnboard,List<Ticket> tickets,DeveloperCareerLife.Career career,DevOpsLife.Console devops,ITBusinessLife.Status companyStatus){}
    static Workspace workspace(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!SoftwareLife.linkedProgrammer(t,c))return null;
        boolean onboarded=EducationLife.get(t,c,"java-onboarded")>0;
        boolean ready=RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==TownLife.Place.OFFICE
            &&!EmploymentLife.sick(c,day)&&minute>=480&&minute+15<=1080;
        return new Workspace("Borgo Digital",INTRO,onboarded,!onboarded&&ready,TICKETS,DeveloperCareerLife.report(t,day,minute),DevOpsLife.report(t,day,minute),ITBusinessLife.report(t,day,minute));
    }
    static String onboard(TownLife.Town t,int day,int minute){
        var c=RebirthLife.requireAdult(t);var workspace=workspace(t,day,minute);
        if(workspace==null||!workspace.canOnboard())throw new IllegalArgumentException("Onboarding is only available once at the office during working hours.");
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        EducationLife.set(t,c,"java-onboarded",1);
        return "Completed Java team onboarding. Read a ticket and submit its return statement.";
    }
    static void verify(String id,String source){
        if(source==null||source.length()>180)throw new IllegalArgumentException("Enter a solution up to 180 characters.");
        if("retail-api".equals(id)){RetailTicketLife.verifyMapping(source);return;}
        if("retail-sql".equals(id)){RetailTicketLife.verifyQuery(source);return;}
        String code=source.strip();
        if(!code.startsWith("return ")||!code.endsWith(";"))throw new IllegalArgumentException("Write return <expression>;");
        String expr=code.substring(7,code.length()-1);
        int[] values={0,1,2,3,6,12,24,101};
        if("water-control".equals(id)){
            for(int a:values)for(int b:values)for(int c:values)
                check(expr,Map.of("requested",a,"stock",b,"freeCapacity",c),waterUnits(a,b,c));
        }else if("factory-planning".equals(id)){
            for(int a:values)for(int b:new int[]{-9,-1,0,1,2,3,6,24,101})
                check(expr,Map.of("stock",a,"limit",b),factoryUnits(a,b));
        }else throw new IllegalArgumentException("Unknown Java ticket.");
    }
    private static void check(String code,Map<String,Integer> vars,int expected){
        int actual;
        try {actual=new Parser(code,vars).parse();}
        catch(ArithmeticException e){throw new IllegalArgumentException("Arithmetic error for "+vars+".");}
        if(actual!=expected)throw new IllegalArgumentException("Test failed: "+vars+" expected "+expected+", got "+actual+".");
    }
    private static final class Parser {
        final String s; final Map<String,Integer> vars; int pos;
        Parser(String s,Map<String,Integer> vars){this.s=s;this.vars=vars;}
        void ws(){while(pos<s.length()&&Character.isWhitespace(s.charAt(pos)))pos++;}
        boolean eat(String x){ws();if(s.startsWith(x,pos)){pos+=x.length();return true;}return false;}
        void require(String x){if(!eat(x))throw bad();}
        IllegalArgumentException bad(){return new IllegalArgumentException("Unsupported Java expression near character "+(pos+1)+".");}
        int parse(){int n=expression();ws();if(pos!=s.length())throw bad();return n;}
        int expression(){int n=term();while(true){if(eat("+"))n+=term();else if(eat("-"))n-=term();else return n;}}
        int term(){int n=factor();while(true){if(eat("*"))n*=factor();else if(eat("/"))n/=factor();else if(eat("%"))n%=factor();else return n;}}
        int factor(){
            if(eat("-"))return -factor();
            if(eat("(")){int n=expression();require(")");return n;}
            boolean min=eat("Math.min");
            if(min||eat("Math.max")){require("(");int a=expression();require(",");int b=expression();require(")");return min?Math.min(a,b):Math.max(a,b);}
            ws();int start=pos;
            if(pos<s.length()&&s.charAt(pos)>='0'&&s.charAt(pos)<='9'){
                while(pos<s.length()&&s.charAt(pos)>='0'&&s.charAt(pos)<='9')pos++;
                if(pos-start>1&&s.charAt(start)=='0')throw bad();
                try{return Integer.parseInt(s.substring(start,pos));}catch(NumberFormatException e){throw bad();}
            }
            while(pos<s.length()&&Character.isJavaIdentifierPart(s.charAt(pos)))pos++;
            var value=vars.get(s.substring(start,pos));
            if(value==null)throw bad();return value;
        }
    }
}
