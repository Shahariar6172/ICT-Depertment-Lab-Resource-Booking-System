import com.mbstu.labbooking.MainApp;
import com.mbstu.labbooking.dao.*;
import com.mbstu.labbooking.model.*;
import com.mbstu.labbooking.util.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.scene.image.WritableImage;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Runs against a disposable real MySQL/MariaDB database and the actual JavaFX FXML. */
public class FullIntegrationTest {
    static Connection db;
    static Stage stage;
    static int passed;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++; System.out.println("PASS " + message);
    }
    static void exec(String sql) throws Exception { try (Statement st=db.createStatement()) {st.execute(sql);} }
    static int count(String sql) throws Exception {try (Statement st=db.createStatement(); ResultSet r=st.executeQuery(sql)){r.next();return r.getInt(1);}}
    static void script(String source) throws Exception {
        String clean = source.lines().filter(l -> !l.stripLeading().startsWith("--"))
                .collect(java.util.stream.Collectors.joining("\n"));
        for(String sql : clean.split(";")) if(!sql.isBlank())exec(sql);
    }
    @SuppressWarnings("unchecked") static <T> T node(String id) {return (T)stage.getScene().lookup("#"+id);}
    static ButtonBase button(String text) {
        return stage.getScene().getRoot().lookupAll("*").stream()
                .filter(n -> n instanceof ButtonBase && ((ButtonBase)n).getText().equals(text))
                .map(n -> (ButtonBase)n).findFirst().orElseThrow(() -> new AssertionError("Missing button "+text));
    }
    static void layout() {stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();}
    static void click(String text) {layout();button(text).fire();layout();}
    static void dismissNextDialog() {
        Platform.runLater(() -> new ArrayList<>(Window.getWindows()).stream().filter(w -> w != stage).forEach(Window::hide));
    }
    static void fill(String id, String value) {((TextInputControl)node(id)).setText(value);}
    static void screenshot(String filename) throws Exception {
        layout(); WritableImage img = stage.getScene().snapshot(null);
        BufferedImage out = new BufferedImage((int)img.getWidth(), (int)img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,img.getPixelReader().getArgb(x,y));
        ImageIO.write(out,"png",Path.of(System.getProperty("test.output","."),filename).toFile());
    }
    static void ui() throws Exception {
        stage=new Stage();new MainApp().start(stage);layout();
        check(stage.getTitle().contains("Login"),"login FXML opens");
        click("New student? Register here");
        fill("nameField","Test Student");fill("studentIdField","TEST-1");fill("emailField","student@test.local");
        fill("passwordField","test123");fill("confirmField","test123");click("Register");
        check(new UserDAO().login("student@test.local","test123") != null,"registration and login against real DB");
        fill("emailField","student@test.local");fill("passwordField","test123");click("Login");
        check(stage.getTitle().equals("Student Panel"),"login opens actual student FXML");
        ComboBox<String> subjects=node("subjectBox");
        check(subjects.getItems().containsAll(List.of("EEE Lab","IoT Project","Robotics Lab")),"subjects populated");
        subjects.setValue("EEE Lab");
        ComboBox<String> materials=node("materialBox");
        check(materials.getItems().containsAll(List.of("Breadboard","Sensor")),"EEE material dropdown contains breadboards and sensors");
        exec("UPDATE devices SET status='DAMAGED'");click("Refresh Stock");
        check(!materials.getItems().isEmpty() && ((Button)node("addButton")).isDisabled(), "zero stock shows catalog with booking disabled");
        check(((Label)node("catalogStatus")).getText().contains("No units available"), "zero stock explains what to do");
        exec("UPDATE devices SET status='AVAILABLE'");click("Refresh Stock");
        materials.setValue("Breadboard");
        ComboBox<MaterialOption> variants=node("variantBox");
        MaterialOption bread=variants.getItems().stream().filter(v->v.getDeviceName().equals("Bread board - 2 part")).findFirst().orElseThrow();
        check(bread.getAvailable()==2 && bread.getVariantName().equals("2 parts"),"legacy spelling and OTHER category map to real breadboard stock");
        variants.setValue(bread);
        Spinner<Integer> quantity=node("quantitySpinner"); quantity.getValueFactory().setValue(1);click("Add to List");
        quantity.getValueFactory().setValue(1);click("Add to List");
        TableView<RequestItem> cart=node("cartTable");
        check(cart.getItems().size()==1 && cart.getItems().get(0).getQuantity()==2,"repeated variant merges quantity");
        check(((Button)node("addButton")).isDisabled(),"cannot exceed available stock");
        materials.setValue("Sensor");
        MaterialOption sensor=variants.getItems().stream().filter(v->v.getDeviceName().equals("Custom Temp Sensor")).findFirst().orElseThrow();
        variants.setValue(sensor);click("Add to List");
        check(cart.getItems().size()==2,"multiple materials in one draft");
        click("Refresh Stock"); check(cart.getItems().size()==2,"refresh preserves draft");
        ((CheckBox)node("allStockCheck")).fire();
        check(materials.getItems().contains("Lab Widget"),"all stock includes unmapped admin inventory with subject retained");
        check(subjects.getValue().equals("EEE Lab"),"all-stock view keeps request subject");
        ((CheckBox)node("allStockCheck")).fire();
        fill("purposeField","Circuit experiment");
        dismissNextDialog();click("Send All Requests");
        check(count("SELECT COUNT(*) FROM bookings")==0 && cart.getItems().size()==2,"missing return date blocks submission and preserves draft");
        ((DatePicker)node("duePicker")).setValue(LocalDate.now().plusDays(2));
        exec("UPDATE devices SET status='BOOKED' WHERE name='Custom Temp Sensor'");
        dismissNextDialog();click("Send All Requests");
        check(cart.getItems().size()==2 && count("SELECT COUNT(*) FROM bookings")==0, "stale stock failure keeps UI draft and submits no partial request");
        check(count("SELECT COUNT(*) FROM devices WHERE name='Bread board - 2 part' AND status='AVAILABLE'")==2, "UI failure restores earlier reserved items");
        exec("UPDATE devices SET status='AVAILABLE' WHERE name='Custom Temp Sensor'");click("Refresh Stock");
        screenshot("student-cart.png");
        dismissNextDialog();click("Send All Requests");
        check(count("SELECT COUNT(*) FROM bookings")==3 && count("SELECT COUNT(*) FROM request_batches")==1,"single click creates one batch and three unit bookings");
        check(cart.getItems().isEmpty(),"successful request clears draft");
        TabPane tabs=(TabPane)stage.getScene().lookup(".tab-pane");tabs.getSelectionModel().select(1);layout();
        check(((TableView<?>)node("bookingTable")).getItems().size()==3,"My Bookings loads submitted units");
        tabs.getSelectionModel().select(2);layout();check(node("penaltyTable")!=null,"My Penalties opens");
        click("Logout");fill("emailField","admin@mbstu.ac.bd");fill("passwordField","admin123");click("Login");
        check(stage.getTitle().equals("Admin Panel"),"existing admin login works");
        TableView<Booking> pending=node("pendingTable");
        check(pending.getItems().size()==3,"admin sees every requested unit");
        pending.getSelectionModel().selectFirst();click("Approve");
        TabPane adminTabs=(TabPane)stage.getScene().lookup(".tab-pane");adminTabs.getSelectionModel().select(1);layout();
        TableView<Booking> approved=node("approvedTable"); approved.getSelectionModel().selectFirst();click("Mark as Issued");
        TableView<Booking> issued=node("issuedTable");check(issued.getItems().size()==1,"admin issue workflow works");
        issued.getSelectionModel().selectFirst();click("Process Return");
        check(count("SELECT COUNT(*) FROM bookings WHERE status='RETURNED'")==1,"admin return workflow works");
        for(int i=2;i<adminTabs.getTabs().size();i++){adminTabs.getSelectionModel().select(i);layout();}
        check(true,"all admin tabs load");
        stage.close();
    }
    public static void main(String[] args) throws Exception {
        String dbName="lab_booking_integration_"+System.currentTimeMillis();
        String server=System.getProperty("test.server","jdbc:mysql://127.0.0.1:3307/");
        try(Connection setup=DriverManager.getConnection(server,"root","");Statement st=setup.createStatement()) {st.execute("CREATE DATABASE "+dbName);}
        db=DriverManager.getConnection(server+dbName,"root","");
        try {
            script(Files.readString(Path.of("tests/fixtures/legacy-database.sql")));
            StudentSchema.ensure(db);
            check(count("SELECT COUNT(*) FROM lab_subjects")==3,"real legacy schema upgrades and seeds subjects");
            check(count("SELECT COUNT(*) FROM devices")==10,"migration preserves legacy device count");
            StudentSchema.ensure(db);check(count("SELECT COUNT(*) FROM lab_subjects")==3,"repeat startup is idempotent");
            // Reproduce the partial setup path: subject rows exist but mappings are absent.
            exec("DELETE FROM subject_materials");StudentSchema.ensure(db);
            check(count("SELECT COUNT(*) FROM subject_materials")>0,"partial catalog repaired");
            Field connection=DBConnection.class.getDeclaredField("connection");connection.setAccessible(true);connection.set(null,db);
            new DeviceDAO().insert("Bread board - 2 part","OTHER","TEST-B1");
            new DeviceDAO().insert("Bread board - 2 part","OTHER","TEST-B2");
            new DeviceDAO().insert("Custom Temp Sensor","MODULE","TEST-S1");
            new DeviceDAO().insert("Lab Widget","OTHER","TEST-X1");
            var options=new DeviceDAO().findMaterialOptions("EEE Lab");
            check(options.stream().anyMatch(v->v.getDeviceName().equals("Bread board - 2 part")&&v.getAvailable()==2),"real DAO resolves legacy naming");
            check(options.stream().anyMatch(v->v.getVariantName().equals("3 parts")&&v.getAvailable()==0),"unstocked catalog variant explicitly has zero availability");
            Platform.setImplicitExit(false);
            CountDownLatch done=new CountDownLatch(1);Throwable[] failure={null};
            Platform.startup(()->{try{ui();}catch(Throwable e){failure[0]=e;e.printStackTrace();}finally{done.countDown();}});
            if(!done.await(90,TimeUnit.SECONDS))throw new AssertionError("UI test timed out");
            Platform.exit();if(failure[0]!=null)throw new AssertionError("UI integration failed",failure[0]);
            // Real transaction rollback after an earlier line was already reserved.
            MaterialOption a=new MaterialOption("Arduino","Uno","Arduino Uno R3","ARDUINO",2);
            MaterialOption b=new MaterialOption("Sensor","PIR","PIR Motion Sensor","SENSOR",1);
            int before=count("SELECT COUNT(*) FROM bookings"), batches=count("SELECT COUNT(*) FROM request_batches");
            int student=count("SELECT id FROM users WHERE email='student@test.local'");
            try {new BookingDAO().createBatchRequest(student,"EEE Lab","Short stock",LocalDate.now().plusDays(1),List.of(new RequestItem(a,1),new RequestItem(b,2)));throw new AssertionError("short stock accepted");}
            catch(SQLException expected) {check(expected.getMessage().contains("Not enough stock"),"short stock rejected in real database");}
            check(count("SELECT COUNT(*) FROM bookings")==before && count("SELECT COUNT(*) FROM request_batches")==batches,"real transaction rolls back all request rows");
            check(count("SELECT COUNT(*) FROM devices WHERE name='Arduino Uno R3' AND status='AVAILABLE'")==2,"real rollback restores previously reserved unit");
            // Exercise the shipped fresh-install SQL and its repeatable manual upgrade too.
            String freshName=dbName+"_fresh";
            try {
                String fresh=Files.readString(Path.of("sql/database.sql"))
                        .replace("CREATE DATABASE IF NOT EXISTS lab_booking", "CREATE DATABASE IF NOT EXISTS "+freshName)
                        .replace("USE lab_booking;", "USE "+freshName+";");
                script(fresh);
                check(count("SELECT COUNT(*) FROM lab_subjects")==3 && count("SELECT COUNT(*) FROM devices")==10,
                        "shipped fresh-install SQL creates populated catalog and original stock");
                script(Files.readString(Path.of("sql/upgrade_student_panel.sql")).replace("USE lab_booking;", "USE "+freshName+";"));
                check(count("SELECT COUNT(*) FROM devices")==10,"manual SQL upgrade is repeatable without duplicating stock");
            } finally {exec("USE "+dbName);exec("DROP DATABASE IF EXISTS "+freshName);}
            System.out.println("ALL "+passed+" INTEGRATION CHECKS PASSED");
        } finally {
            db.close();
            try(Connection setup=DriverManager.getConnection(server,"root","");Statement st=setup.createStatement()){st.execute("DROP DATABASE "+dbName);}
        }
    }
}
