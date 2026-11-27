package com.holovin.npclife;

import java.util.*;

/** A derived family tree. Resident IDs and the persisted parent links are authoritative. */
final class GenealogyLife {
    private GenealogyLife(){}
    record Relative(String id,String name,int age,boolean alive,String role,String roleIt,int incarnation){}
    private static Set<Integer> parents(TownLife.Town t,TownLife.Citizen c){
        Set<Integer> result=new HashSet<>();
        if(c!=null)for(String field:List.of("parent-a","parent-b")){
            int index=DemographyLife.get(t,c,field);
            if(index>0&&index!=c.generationIndex)result.add(index);
        }
        return result;
    }
    private static boolean intersects(Set<Integer> a,Set<Integer> b){
        return !Collections.disjoint(a,b);
    }
    static List<Relative> relatives(TownLife.Town t,TownLife.Citizen self){
        if(self==null||self.generationIndex<=0)return List.of();
        var ownParents=parents(t,self);
        var roster=new ArrayList<>(t.people.values().stream().filter(c->c.generationIndex>0).toList());
        Map<Integer,Set<Integer>> links=new HashMap<>();
        for(var c:roster)links.put(c.generationIndex,parents(t,c));
        Set<Integer> grandparents=new HashSet<>(),siblings=new HashSet<>(),auntsUncles=new HashSet<>(),children=new HashSet<>();
        for(int p:ownParents)grandparents.addAll(links.getOrDefault(p,Set.of()));
        for(var c:roster){
            int id=c.generationIndex;
            if(id==self.generationIndex)continue;
            var cp=links.get(id);
            if(intersects(cp,ownParents))siblings.add(id);
            if(!ownParents.contains(id)&&intersects(cp,grandparents))auntsUncles.add(id);
            if(cp.contains(self.generationIndex))children.add(id);
        }
        List<Relative> result=new ArrayList<>();
        for(var c:roster){
            int id=c.generationIndex;
            if(id==self.generationIndex)continue;
            var cp=links.get(id);
            String role,it;
            if(ownParents.contains(id)){
                int sex=ResidentSexLife.sex(t,c);
                role=sex==ResidentSexLife.FEMALE?"Mother":sex==ResidentSexLife.MALE?"Father":"Parent";
                it=sex==ResidentSexLife.FEMALE?"Madre":sex==ResidentSexLife.MALE?"Padre":"Genitore";
            }
            else if(grandparents.contains(id)){role="Grandparent";it="Nonno/a";}
            else if(children.contains(id)){role="Child";it="Figlio/a";}
            else if(intersects(cp,children)){role="Grandchild";it="Nipote (discendente)";}
            else if(siblings.contains(id)){
                boolean full=ownParents.size()==2&&cp.equals(ownParents);
                role=full?"Sibling":"Sibling (one known shared parent)";it=full?"Fratello/sorella":"Fratello/sorella (un genitore noto in comune)";
            }
            else if(auntsUncles.contains(id)){role="Aunt/uncle";it="Zio/a";}
            else if(intersects(cp,siblings)){role="Niece/nephew";it="Nipote (di fratello/sorella)";}
            else if(intersects(cp,auntsUncles)){role="Cousin";it="Cugino/a";}
            else if(id==DemographyLife.get(t,self,"partner")&&DemographyLife.get(t,c,"partner")==self.generationIndex){role="Partner";it="Partner";}
            else continue;
            result.add(new Relative(c.id,c.name,c.age,c.alive,role,it,DemographyLife.get(t,c,"played-incarnation")));
        }
        result.sort(Comparator.comparing(Relative::role).thenComparing(Relative::name).thenComparing(Relative::id));
        return List.copyOf(result);
    }
    /** A game-design approximation, not a biological prediction. */
    static int inheritedTrait(int first,int second,long seed){
        int midpoint=(first+second)/2;
        int variation=Math.floorMod(seed,31)-15;
        return Math.max(15,Math.min(90,midpoint+variation));
    }
}
