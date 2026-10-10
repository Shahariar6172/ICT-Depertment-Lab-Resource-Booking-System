import com.mbstu.labbooking.util.StudentSchema;
import java.sql.*;
import java.util.*;

/** Regression check: old schema is upgraded; a ready schema is read-only. */
public class StudentSchemaTest {
    static class Schema {
        Set<String> tables = new HashSet<>(List.of("users", "devices", "bookings"));
        boolean batchColumn, seeded, failDDL;
        String category = "ENUM";
        List<String> writes = new ArrayList<>();
        Connection connection() {
            DatabaseMetaData meta = BatchRequestTest.proxy(DatabaseMetaData.class, (p,m,a) -> {
                switch (m.getName()) {
                    case "getSearchStringEscape": return "\\";
                    case "getTables": return row(tables.contains((String)a[2]) ? (String)a[2] : null);
                    case "getColumns":
                        String col = ((String)a[3]).replace("\\_", "_");
                        return row(col.equals("category") ? category : batchColumn ? "INT" : null);
                    default: return BatchRequestTest.defaultValue(m.getReturnType());
                }
            });
            Statement stmt = BatchRequestTest.proxy(Statement.class, (p,m,a) -> {
                if (m.getName().equals("executeQuery")) return row(seeded ? "EEE Lab" : null);
                if (m.getName().equals("execute")) {
                    if (failDDL) throw new SQLException("CREATE command denied", "42000", 1142);
                    String sql = (String)a[0]; writes.add(sql);
                    if (sql.startsWith("CREATE TABLE")) tables.add(sql.split("\s+")[5]);
                    if (sql.startsWith("ALTER TABLE devices")) category = "VARCHAR";
                    if (sql.startsWith("ALTER TABLE bookings")) batchColumn = true;
                    if (sql.startsWith("INSERT IGNORE INTO lab_subjects")) seeded = true;
                    return false;
                }
                return BatchRequestTest.defaultValue(m.getReturnType());
            });
            return BatchRequestTest.proxy(Connection.class, (p,m,a) -> {
                switch (m.getName()) {
                    case "getCatalog": return "lab_booking";
                    case "getMetaData": return meta;
                    case "createStatement": return stmt;
                    default: return BatchRequestTest.defaultValue(m.getReturnType());
                }
            });
        }
    }
    static ResultSet row(String value) {
        boolean[] used = {false};
        return BatchRequestTest.proxy(ResultSet.class, (p,m,a) -> {
            if (m.getName().equals("next")) {
                boolean result = !used[0] && value != null; used[0] = true; return result;
            }
            if (m.getName().equals("getString")) return value;
            return BatchRequestTest.defaultValue(m.getReturnType());
        });
    }
    public static void main(String[] args) throws Exception {
        Schema db = new Schema();
        StudentSchema.ensure(db.connection());
        BatchRequestTest.check(db.seeded && db.batchColumn && db.category.equals("VARCHAR"), "old schema fully upgraded");
        BatchRequestTest.check(db.tables.containsAll(List.of("lab_subjects", "material_variants", "subject_materials", "request_batches")), "all required tables created");
        BatchRequestTest.check(db.writes.stream().noneMatch(s -> s.contains("DROP ") || s.contains("DELETE ") || s.contains("TRUNCATE ")), "existing data preserved");
        int firstWrites = db.writes.size();
        db.failDDL = true;
        StudentSchema.ensure(db.connection());
        BatchRequestTest.check(db.writes.size() == firstWrites, "ready database must not require DDL permission");
        db = new Schema(); db.failDDL = true;
        try {
            StudentSchema.ensure(db.connection());
            throw new AssertionError("permission failure must propagate");
        } catch (SQLException expected) {
            BatchRequestTest.check(expected.getMessage().contains("upgrade_student_panel.sql") && expected.getErrorCode() == 1142, "actionable error and original error code");
        }
        System.out.println("PASS: automatic schema upgrade, catalog seeds, repeat launch without writes, permission diagnostics.");
    }
}
