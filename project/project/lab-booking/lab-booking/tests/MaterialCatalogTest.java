import com.mbstu.labbooking.model.*;
import com.mbstu.labbooking.util.MaterialCatalog;
import java.util.*;

public class MaterialCatalogTest {
    public static void main(String[] args) {
        var definitions=List.of(new MaterialOption("Breadboard","2 parts","Breadboard 2 Part","BOARD",0),
                new MaterialOption("Sensor","DHT11","DHT11 Temp Sensor","SENSOR",0));
        var old=List.of(new DeviceGroup("Bread board - 2 part","OTHER",3,2,1,0),
                new DeviceGroup("DHT11 Temp Sensor","MODULE",1,1,0,0),
                new DeviceGroup("Widget","OTHER",1,1,0,0));
        var options=MaterialCatalog.build(old,definitions,Set.of("Breadboard","Sensor"));
        if(options.size()!=2 || options.stream().mapToInt(MaterialOption::getAvailable).sum()!=3)
            throw new AssertionError("legacy spelling/category mapping or deduplication failed");
        if(options.stream().noneMatch(o->o.getDeviceName().equals("Bread board - 2 part") && o.getCategory().equals("OTHER")))
            throw new AssertionError("reservation identity changed");
        if(MaterialCatalog.build(old,definitions,null).stream().noneMatch(o->o.getMaterialName().equals("Widget")))
            throw new AssertionError("unmapped stock hidden from all-stock view");
        var empty=MaterialCatalog.build(List.of(),definitions,Set.of("Breadboard","Sensor"));
        if(empty.size()!=2 || empty.stream().anyMatch(o->o.getAvailable()!=0))
            throw new AssertionError("empty inventory must show catalog without invented availability");
        System.out.println("PASS: catalog matching, subject filter, real stock identity, unmapped stock and zero stock.");
    }
}
