package com.holovin.npclife;

/** Daily emotional drift. Memories, introductions and outstanding debts are not erased. */
final class ContactTimeLife {
    private ContactTimeLife(){}
    static void tick(TownLife.Town town,int day,int minute){
        if(minute!=0)return;
        for(var contact:town.player.contacts.values()){
            var resident=town.people.get(contact.residentId);
            if(resident==null||!resident.alive||contact.lastDay<=0||day<=contact.lastDay)continue;
            // Reuse the already persisted, incarnation-scoped social memory counters.
            String cursor="social-memory:"+town.player.incarnation+":"+contact.residentId+":last-drift-day";
            if(town.industryStocks.getOrDefault(cursor,0)>=day)continue;
            town.industryStocks.put(cursor,day);
            int absent=day-contact.lastDay;
            if(absent>=14 && absent%7==0)contact.affinity=approach(contact.affinity,0,1);
            // Familiarity fades faster than established trust. Betrayal and unpaid
            // debt cannot be fixed simply by waiting; explicit reconciliation matters.
            boolean serious=SocialMemoryLife.unresolved(town,contact.residentId)
                    && SocialMemoryLife.last(town,contact.residentId,SocialMemoryLife.Kind.BROKEN_TRUST)
                    >=Math.max(1,SocialMemoryLife.last(town,contact.residentId,SocialMemoryLife.Kind.APOLOGY));
            boolean overdue=PersonalLoanLife.outstanding(town,contact.residentId)>0
                    && day>PersonalLoanLife.dueDay(town,contact.residentId);
            if(absent>=30 && absent%30==0 && (contact.trust>50||(!serious&&!overdue)))
                contact.trust=approach(contact.trust,50,1);
            SocialMemoryLife.relaxHurt(town,contact.residentId,day,serious);
        }
    }
    private static int approach(int current,int target,int step){
        return current<target?Math.min(target,current+step):Math.max(target,current-step);
    }
}
