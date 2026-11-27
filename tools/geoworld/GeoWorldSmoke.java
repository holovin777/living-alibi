import java.nio.file.*;
public class GeoWorldSmoke {
    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("geo-world-test");
        var graph=dir.resolve("graph.tsv");var save=dir.resolve("save.properties");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nN\t3\t100\t100\nE\t1\t2\nE\t2\t3\nB\t10\t1\nB\t20\t3\n");
        var world=new GeoWorld(graph,save,500);
        if(world.residents.size()!=500||!Files.exists(save))throw new AssertionError("roster");
        var route=world.route("1","3");
        if(route.meters()!=200||!route.position(.75).equals(new GeoWorld.Point(100,50)))throw new AssertionError("route");
        var resident=world.residents.get(0);
        if(world.visible(resident,resident.start(),new GeoWorld.Point(0,0))!=null)throw new AssertionError("working resident exposed");
        if(world.visible(resident,resident.start()-resident.duration()/2.0,new GeoWorld.Point(0,0))==null)throw new AssertionError("commute");
        var loaded=new GeoWorld(graph,save,500);
        if(!resident.equals(loaded.residents.get(0))||loaded.initialMinute<465)throw new AssertionError("reload");
        if(!world.snapshot(465,new GeoWorld.Point(0,0)).contains("\"population\":500"))throw new AssertionError("snapshot");
        System.out.println("GeoWorldSmoke: ALL CHECKS PASSED");
    }
}
