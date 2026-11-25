package com.holovin.npclife;

/** Small, interest-free private loans. Only explicit player actions move cash. */
final class PersonalLoanLife {
    private PersonalLoanLife() {}
    static final int AMOUNT=10, TERM=7, RESERVE=40;
    private static String key(TownLife.Town t,String resident,String field) {
        return "personal-loan:"+t.player.incarnation+":"+resident+":"+field;
    }
    static int get(TownLife.Town t,String resident,String field) {
        return t.industryStocks.getOrDefault(key(t,resident,field),0);
    }
    private static void set(TownLife.Town t,String resident,String field,int value) {
        t.industryStocks.put(key(t,resident,field),value);
    }
    static int outstanding(TownLife.Town t,String resident){return get(t,resident,"balance");}
    static int dueDay(TownLife.Town t,String resident){return get(t,resident,"due");}
    static String borrow(TownLife.Town t,TownLife.Citizen lender,PlayerLife.Contact contact,int day) {
        if(lender.age<18||lender.money<RESERVE+AMOUNT||contact.trust<65||contact.affinity<20||
                t.player.money>20||outstanding(t,lender.id)>0||get(t,lender.id,"last")>=day||
                SocialMemoryLife.unresolved(t,lender.id))
            return "Sorry, I cannot lend you money right now.";
        // One outstanding personal loan across the whole town.
        if(t.people.values().stream().anyMatch(c->outstanding(t,c.id)>0))
            return "Please repay your existing personal loan first.";
        int due=Math.addExact(day,TERM);
        lender.money-=AMOUNT;t.player.money=Math.addExact(t.player.money,AMOUNT);
        set(t,lender.id,"balance",AMOUNT);set(t,lender.id,"due",due);
        set(t,lender.id,"last",day);set(t,lender.id,"overdue-noted",0);
        return "Here are 10 EUR. Please repay me within seven days.";
    }
    static String repay(TownLife.Town t,TownLife.Citizen lender,PlayerLife.Contact contact,int day) {
        int balance=outstanding(t,lender.id);
        if(balance==0)return "You do not owe me any money.";
        if(t.player.money<balance)return "You do not have enough money to repay me.";
        int next=Math.addExact(lender.money,balance);
        boolean late=day>dueDay(t,lender.id);
        t.player.money-=balance;lender.money=next;
        set(t,lender.id,"balance",0);set(t,lender.id,"due",0);
        contact.trust=Math.min(100,contact.trust+(late?2:5));
        contact.affinity=Math.min(100,contact.affinity+(late?1:3));
        return late?"Thank you for finally repaying me.":"Thank you for keeping your promise.";
    }
    static void tick(TownLife.Town t,WorldState clock) {
        if(clock.minute%60!=0||RebirthLife.active(t)||!t.player.alive)return;
        for(var contact:t.player.contacts.values()){
            String id=contact.residentId;
            if(outstanding(t,id)==0||clock.day<=dueDay(t,id)||get(t,id,"overdue-noted")>0)continue;
            contact.trust=Math.max(0,contact.trust-10);
            contact.affinity=Math.max(-100,contact.affinity-5);
            set(t,id,"overdue-noted",1);
        }
    }
}
