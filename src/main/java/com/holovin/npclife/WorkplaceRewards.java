package com.holovin.npclife;

import java.util.Comparator;

/** A small, fictional profit-sharing incentive after a successful wholesale sale.
 * No bonus if the firm cannot retain its payroll reserve. No new money or inventory. */
final class WorkplaceRewards {
    private WorkplaceRewards(){}
    static final int BONUS=4, MIN_SAMPLES=5, MIN_QUALITY=75, MIN_PACE=65;
    static String key(FactoryNetworkLife.Plant p){
        return "workplace-performance:bonus-week:"+p.id();
    }
    /** Only called by a successful delivery, not by an attendance or dialogue action. */
    static boolean afterSale(TownLife.Town t,WorldState s,FactoryNetworkLife.Plant p){
        int week=(s.day-1)/7+1;
        if(t.industryStocks.getOrDefault(key(p),0)>=week)return false;
        var roster=t.people.values().stream().filter(c->WorkplaceHierarchy.eligible(c)
            &&p.occupation().equals(c.occupation)&&p.site()==c.workplace).toList();
        String account=FactoryNetworkLife.account(p);
        int cash=t.employerFunds.getOrDefault(account,0);
        // Preserve at least one eight-hour shift's gross payroll for the entire roster.
        long reserve=32L*roster.size();
        if(cash<reserve+BONUS)return false;
        var winner=roster.stream().filter(c->WorkplaceHierarchy.onDuty(c,s.day)
                &&!RebirthLife.linkedAdult(t,c)&&!TownLife.inPlayerConversation(t,c))
            .filter(c->{var r=WorkplacePerformance.report(t,c);
                return r.samples()>=MIN_SAMPLES&&r.quality()>=MIN_QUALITY&&r.pace()>=MIN_PACE;})
            .sorted(Comparator.<TownLife.Citizen>comparingInt(c->WorkplacePerformance.report(t,c).quality())
                .reversed().thenComparing(Comparator.<TownLife.Citizen>comparingInt(c->WorkplacePerformance.report(t,c).pace()).reversed())
                .thenComparing(c->c.id)).findFirst().orElse(null);
        if(winner==null)return false;
        int wallet=Math.addExact(winner.money,BONUS);
        t.employerFunds.put(account,cash-BONUS);
        winner.money=wallet;
        t.industryStocks.put(key(p),week);
        t.industryStocks.put(WorkplacePerformance.key(winner,"last-bonus-day"),s.day);
        return true;
    }
}
