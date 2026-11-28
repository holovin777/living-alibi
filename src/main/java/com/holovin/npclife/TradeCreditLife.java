package com.holovin.npclife;

import java.util.*;

/** Short supplier credit on delivered domestic inputs, with matching payable/receivable.
 * At most one outstanding invoice per factory; no synthetic cash or interest. */
final class TradeCreditLife {
    private TradeCreditLife(){}
    static final int TERM=3, MAX_INVOICE=6, SELLER_RESERVE=40;
    private static String key(FactoryNetworkLife.Plant p,String item,String f){
        return "trade-credit:"+p.id()+":"+item+":"+f;
    }
    private static int get(TownLife.Town t,FactoryNetworkLife.Plant p,String item,String f){
        return t.industryStocks.getOrDefault(key(p,item,f),0);
    }
    private static void set(TownLife.Town t,FactoryNetworkLife.Plant p,String item,String f,int n){
        t.industryStocks.put(key(p,item,f),n);
    }
    private static int cash(TownLife.Town t,String a){return t.employerFunds.getOrDefault(a,0);}
    static int payable(TownLife.Town t,String a){
        return FactoryNetworkLife.PLANTS.stream().filter(p->FactoryNetworkLife.account(p).equals(a))
            .flatMapToInt(p->p.inputs().keySet().stream().mapToInt(i->get(t,p,i,"balance"))).sum();
    }
    static int receivable(TownLife.Town t,String a){
        return FactoryNetworkLife.PLANTS.stream().flatMapToInt(p->p.inputs().keySet().stream()
            .filter(i->FactoryNetworkLife.supplier(i).equals(a)).mapToInt(i->get(t,p,i,"balance"))).sum();
    }
    static int overdue(TownLife.Town t,String a,int day){
        return FactoryNetworkLife.PLANTS.stream().filter(p->FactoryNetworkLife.account(p).equals(a))
            .flatMapToInt(p->p.inputs().keySet().stream().filter(i->get(t,p,i,"due")<=day)
                .mapToInt(i->get(t,p,i,"balance"))).sum();
    }
    static boolean eligible(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int amount){
        if(FactoryNetworkLife.imported(item)||amount<=0||amount>MAX_INVOICE)return false;
        String buyer=FactoryNetworkLife.account(p),seller=FactoryNetworkLife.supplier(item);
        return !buyer.equals(seller)&&!BusinessSolvencyLife.suspended(t,buyer)&&!BusinessSolvencyLife.suspended(t,seller)
            &&BusinessSolvencyLife.outstanding(t,buyer)==0&&BusinessSolvencyLife.outstanding(t,seller)==0
            &&BusinessCreditLife.get(t,buyer,"missed")==0&&payable(t,buyer)==0
            &&cash(t,buyer)<BusinessCreditLife.WORKING_RESERVE+amount
            &&cash(t,seller)>=SELLER_RESERVE+amount+receivable(t,seller);
    }
    static void issue(TownLife.Town t,FactoryNetworkLife.Plant p,String item,int units,int amount,int day){
        if(day<=0||get(t,p,item,"balance")>0||amount<=0||amount>MAX_INVOICE)throw new IllegalStateException("Invalid trade invoice.");
        set(t,p,item,"issued",day);set(t,p,item,"due",day+TERM);set(t,p,item,"units",units);
        set(t,p,item,"amount",amount);set(t,p,item,"balance",amount);set(t,p,item,"paid-day",0);
    }
    static void settle(TownLife.Town t,String a,int day){
        if(overdue(t,a,day)==0)return;
        BusinessSolvencyLife.settle(t,a);
        if(BusinessSolvencyLife.outstanding(t,a)>0)return;
        for(var p:FactoryNetworkLife.PLANTS){
            if(!FactoryNetworkLife.account(p).equals(a))continue;
            for(String item:p.inputs().keySet().stream().sorted().toList()){
                int balance=get(t,p,item,"balance");
                if(balance<=0||get(t,p,item,"due")>day)continue;
                int amount=Math.min(balance,Math.max(0,cash(t,a)-BusinessCreditLife.WORKING_RESERVE));
                if(amount<=0)continue;
                if(!SupplyChainLife.pay(t,a,FactoryNetworkLife.supplier(item),amount))continue;
                set(t,p,item,"balance",balance-amount);
                if(amount==balance)set(t,p,item,"paid-day",day);
            }
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=390)return;
        for(String a:BusinessSolvencyLife.accounts(t))settle(t,a,s.day);
    }
    record Invoice(String buyer,String supplier,String item,int issuedDay,int dueDay,int units,
                   int amount,int balance,int paidDay){}
    record View(int payables,int receivables,List<Invoice> invoices){}
    static View view(TownLife.Town t,String a){
        var invoices=new ArrayList<Invoice>();
        for(var p:FactoryNetworkLife.PLANTS)for(String item:p.inputs().keySet().stream().sorted().toList()){
            String buyer=FactoryNetworkLife.account(p),seller=FactoryNetworkLife.supplier(item);
            int day=get(t,p,item,"issued");
            if(day<=0||!a.equals(buyer)&&!a.equals(seller))continue;
            invoices.add(new Invoice(buyer,seller,item,day,get(t,p,item,"due"),get(t,p,item,"units"),
                get(t,p,item,"amount"),get(t,p,item,"balance"),get(t,p,item,"paid-day")));
        }
        return new View(payable(t,a),receivable(t,a),List.copyOf(invoices));
    }
}
