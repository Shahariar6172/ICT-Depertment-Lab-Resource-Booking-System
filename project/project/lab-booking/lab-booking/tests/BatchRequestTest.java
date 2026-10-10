import com.mbstu.labbooking.dao.BookingDAO;
import com.mbstu.labbooking.model.MaterialOption;
import com.mbstu.labbooking.model.RequestItem;
import com.mbstu.labbooking.util.DBConnection;
import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Dependency-free transaction tests using a JDBC double, plus Java source syntax checks.
 * Run from project root; see tests/README.md. Real MySQL integration still requires a lab DB. */
public class BatchRequestTest {
    static class Database {
        Map<Integer, String[]> stock = new LinkedHashMap<>();
        List<Integer> bookings = new ArrayList<>();
        Map<Integer, String[]> beforeStock;
        List<Integer> beforeBookings;
        int batches, beforeBatches, commits, rollbacks;
        boolean autoCommit = true;
        Database() {
            stock.put(1, new String[]{"Breadboard 2 Part", "BOARD", "AVAILABLE"});
            stock.put(2, new String[]{"Breadboard 2 Part", "BOARD", "AVAILABLE"});
            stock.put(3, new String[]{"Breadboard 2 Part", "OTHER", "AVAILABLE"});
            stock.put(4, new String[]{"DHT11 Temp Sensor", "SENSOR", "AVAILABLE"});
        }
        Map<Integer, String[]> copyStock() {
            Map<Integer, String[]> copy = new LinkedHashMap<>();
            stock.forEach((k,v) -> copy.put(k,v.clone()));
            return copy;
        }
        Connection connection() {
            return proxy(Connection.class, (p,m,a) -> {
                switch (m.getName()) {
                    case "isClosed": return false;
                    case "getAutoCommit": return autoCommit;
                    case "setAutoCommit":
                        autoCommit = (boolean)a[0];
                        if (!autoCommit) {
                            beforeStock = copyStock();
                            beforeBookings = new ArrayList<>(bookings);
                            beforeBatches = batches;
                        }
                        return null;
                    case "prepareStatement": return statement((String)a[0]);
                    case "commit": commits++; return null;
                    case "rollback":
                        rollbacks++; stock = beforeStock; bookings = beforeBookings; batches = beforeBatches;
                        return null;
                    default: return defaultValue(m.getReturnType());
                }
            });
        }
        PreparedStatement statement(String sql) {
            Map<Integer, Object> values = new HashMap<>();
            return proxy(PreparedStatement.class, (p,m,a) -> {
                if (m.getName().startsWith("set")) { values.put((int)a[0],a[1]); return null; }
                switch (m.getName()) {
                    case "executeUpdate":
                        if (sql.startsWith("INSERT INTO request_batches")) { batches++; return 1; }
                        if (sql.startsWith("UPDATE devices")) {
                            String[] unit = stock.get((int)values.get(1));
                            if (!unit[2].equals("AVAILABLE")) return 0;
                            unit[2] = "BOOKED"; return 1;
                        }
                        if (sql.startsWith("INSERT INTO bookings")) {
                            bookings.add((int)values.get(2)); return 1;
                        }
                        throw new AssertionError(sql);
                    case "getGeneratedKeys": return result(List.of(batches));
                    case "executeQuery":
                        check(sql.contains("FOR UPDATE"), "stock query must lock rows");
                        List<Integer> ids = new ArrayList<>();
                        for (var entry : stock.entrySet()) {
                            String[] unit = entry.getValue();
                            if (unit[0].equals(values.get(1)) && unit[1].equals(values.get(2)) && unit[2].equals("AVAILABLE")
                                    && ids.size() < (int)values.get(3)) ids.add(entry.getKey());
                        }
                        return result(ids);
                    default: return defaultValue(m.getReturnType());
                }
            });
        }
    }
    static ResultSet result(List<Integer> ids) {
        int[] cursor = {-1};
        return proxy(ResultSet.class, (p,m,a) -> {
            if (m.getName().equals("next")) return ++cursor[0] < ids.size();
            if (m.getName().equals("getInt")) return ids.get(cursor[0]);
            return defaultValue(m.getReturnType());
        });
    }
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler));
    }
    static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        return null;
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void install(Database db) throws Exception {
        Field f = DBConnection.class.getDeclaredField("connection");
        f.setAccessible(true); f.set(null,db.connection());
    }
    static RequestItem bread(int quantity) {
        return new RequestItem(new MaterialOption("Breadboard", "2 parts", "Breadboard 2 Part", "BOARD", 2),quantity);
    }
    static RequestItem sensor(int quantity) {
        return new RequestItem(new MaterialOption("Sensor", "DHT11", "DHT11 Temp Sensor", "SENSOR", 1),quantity);
    }
    public static void main(String[] args) throws Exception {
        BookingDAO dao = new BookingDAO();
        Database db = new Database(); install(db);
        String code = dao.createBatchRequest(7,"EEE Lab","Circuit experiment",LocalDate.now().plusDays(3),List.of(sensor(1),bread(2)));
        check(UUID.fromString(code) != null, "request code");
        check(db.commits == 1 && db.batches == 1 && db.bookings.size() == 3, "one transaction, one batch, three unit bookings");
        check(db.stock.get(3)[2].equals("AVAILABLE"), "same name in another category must remain available");
        check(db.autoCommit, "autocommit restored after success");

        db = new Database(); install(db);
        try {
            dao.createBatchRequest(7,"EEE Lab","Circuit experiment",LocalDate.now().plusDays(3),List.of(bread(2),sensor(2)));
            throw new AssertionError("short stock must fail");
        } catch (SQLException expected) { }
        check(db.rollbacks == 1 && db.batches == 0 && db.bookings.isEmpty(), "batch and all previous line inserts must roll back");
        check(db.stock.values().stream().allMatch(v -> v[2].equals("AVAILABLE")), "reserved stock must roll back");
        check(db.autoCommit, "autocommit restored after failure");

        db = new Database(); install(db);
        dao.createBatchRequest(7,"EEE Lab","First student",LocalDate.now().plusDays(3),List.of(bread(2)));
        try {
            dao.createBatchRequest(8,"EEE Lab","Second student",LocalDate.now().plusDays(3),List.of(bread(1)));
            throw new AssertionError("stale inventory must fail");
        } catch (SQLException expected) { }
        check(db.batches == 1 && db.bookings.size() == 2, "failed later request must preserve the successful request");
        try {
            dao.createBatchRequest(7,"EEE Lab","",LocalDate.now(),List.of());
            throw new AssertionError("invalid draft must fail");
        } catch (IllegalArgumentException expected) { }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics,null,null);
             var paths = Files.walk(Path.of("src/main/java"))) {
            var sources = paths.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
            JavacTask task = (JavacTask)compiler.getTask(null,fm,diagnostics,List.of("-proc:none"),null,fm.getJavaFileObjectsFromFiles(sources));
            task.parse();
            check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR), diagnostics.getDiagnostics().toString());
        }
        System.out.println("PASS: multi-item transaction, category isolation, rollback, stale stock, validation, Java syntax.");
    }
}
