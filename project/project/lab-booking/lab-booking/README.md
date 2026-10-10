# ICT Lab Resource Booking System

JavaFX + MySQL/MariaDB project. This revision fixes empty student material lists and validates the complete student-to-admin booking flow.

## Run this ZIP

1. Close the old app. Extract this ZIP into a new folder.
2. Open `project/lab-booking/lab-booking` (the directory containing `pom.xml`).
3. Install/use **JDK 21+**, **Maven 3.8+**, and a running **MySQL 8** or compatible MariaDB server.
4. In `src/main/java/com/mbstu/labbooking/util/DBConnection.java`, set `URL`, `USER` and `PASS` to your existing lab database credentials.
5. Run:

```bash
mvn clean javafx:run
```

**Existing database:** keep it. The app creates the missing student catalog and request-batch schema automatically before login. Do not re-import `database.sql` over existing data.

**New installation only:** first create the database with:

```bash
mysql -u root -p < sql/database.sql
```

For a database account without CREATE/ALTER permissions, the database owner can apply `sql/upgrade_student_panel.sql` once. Ready databases are opened without repeating schema changes.

Admin login: `admin@mbstu.ac.bd` / `admin123`. Students register from the login screen.

## Student workflow

1. Choose a subject/work. The first subject loads automatically.
2. Choose a material, a variant and the required quantity.
3. Click **Add to List**. Repeat for breadboards, sensors and other items.
4. Review the list. Repeated variants merge quantities; **Remove Selected** removes a line.
5. Enter a future return date and a purpose, then **Send All Requests** once.
6. **My Bookings** shows each physical unit with a shared request code and its status.

A submission uses one database transaction. If another student takes stock before submission, no partial request is saved and your draft remains available to edit/retry.

**Refresh Stock** retains your draft. Changing subject clears it. **Show all lab stock** includes unmapped inventory while keeping the chosen subject for your request.

## Material names and stock

- Existing stock is matched to catalog labels with normalized names. Differences in case, spaces, hyphens or legacy categories no longer silently hide known devices.
- Common breadboard names such as `Bread board - 2 part` are grouped under **Breadboard / 2 parts**. Sensor-category devices and names containing Sensor are grouped under **Sensor**.
- Exact configured mappings take priority. Reservation still uses the original device name and category, so another category's units cannot be booked by mistake.
- Configured variants without physical stock are visible with **0 available** and cannot be added. The app never creates stock during an upgrade.
- Unrecognized equipment remains visible via **Show all lab stock** or custom work.

To add real breadboard stock in the existing admin panel:

| Device name | Category | Variant shown |
| --- | --- | --- |
| Breadboard 1 Part | BOARD | 1 part |
| Breadboard 2 Part | BOARD | 2 parts |
| Breadboard 3 Part | BOARD | 3 parts |

Add one row per physical unit with a unique serial number. Quantity comes from the number of AVAILABLE units. No available stock means the admin needs to register actual units or complete their return/repair.

## Custom catalog configuration

Subjects are stored in `lab_subjects`, variant labels in `material_variants`, and subject-material links in `subject_materials`. For example:

```sql
INSERT IGNORE INTO lab_subjects (name) VALUES ('Circuit Lab');
INSERT INTO material_variants (device_name, category, material_name, variant_name)
VALUES ('Resistor 220 Ohm', 'COMPONENT', 'Resistor', '220 ohm')
ON DUPLICATE KEY UPDATE material_name='Resistor', variant_name='220 ohm';
INSERT IGNORE INTO subject_materials (subject_name, material_name)
VALUES ('Circuit Lab', 'Resistor'), ('Circuit Lab', 'Breadboard');
```

Add the physical Resistor 220 Ohm units in the admin panel separately.

The admin retains its existing per-unit approve/reject/issue/return workflow. The purpose field includes the batch code and subject.

## Verification in this release

- Full Maven compilation: passed on JDK 21 with JavaFX 23.0.1.
- 37 real database + JavaFX integration checks: passed, including registration, both logins, legacy database upgrade, fresh-install SQL, catalog recovery, student cart, quantities, zero stock, stale stock rollback, booking history, admin issue/return and all admin tabs.
- Database used for integration: MariaDB 11.4.5 with MySQL Connector/J 8.4.0. JavaFX screens ran headlessly through Monocle.
- Additional catalog and transaction regression tests: passed.

See `tests/README.md`, `docs/verification.txt`, and `docs/student-panel.png`. Your local MySQL credentials, server state and permissions still need to match your installation.
