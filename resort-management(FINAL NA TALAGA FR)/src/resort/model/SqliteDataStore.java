package resort.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class SqliteDataStore {
    private SqliteDataStore() {
    }

    static void save(Path database, DataManager.DataSnapshot snapshot) throws IOException {
        try {
            Files.createDirectories(database.toAbsolutePath().getParent());
            try (Connection connection = open(database)) {
                createSchema(connection);
                connection.setAutoCommit(false);
                try {
                    writeSettings(connection, snapshot.extraPersonFeePerNight);
                    writeRooms(connection, snapshot.rooms);
                    writeGuests(connection, snapshot.guests);
                    writeStaff(connection, snapshot.staffList);
                    writeAddOns(connection, snapshot.addOns);
                    writeAccounts(connection, snapshot.accounts);
                    writeTasks(connection, snapshot.tasks);
                    writeBookings(connection, snapshot.bookings);
                    writeBills(connection, snapshot.bills);
                    removeMissingRows(connection, "bills", "bill_id",
                            snapshot.bills, Bill::getBillId);
                    removeMissingRows(connection, "booking_add_ons", "booking_id",
                            snapshot.bookings, Booking::getBookingId);
                    removeMissingRows(connection, "bookings", "booking_id",
                            snapshot.bookings, Booking::getBookingId);
                    removeMissingRows(connection, "tasks", "task_id",
                            snapshot.tasks, Task::getTaskId);
                    removeMissingRows(connection, "accounts", "username",
                            snapshot.accounts, Account::getUsername);
                    removeMissingRows(connection, "add_ons", "add_on_id",
                            snapshot.addOns, AddOn::getAddOnId);
                    removeMissingRows(connection, "staff", "staff_id",
                            snapshot.staffList, Staff::getStaffId);
                    removeMissingRows(connection, "guests", "guest_id",
                            snapshot.guests, Guest::getGuestId);
                    removeMissingRows(connection, "rooms", "room_id",
                            snapshot.rooms, Room::getRoomId);
                    removeMissingRows(connection, "app_settings", "setting_key",
                            java.util.Collections.singletonList("extraPersonFeePerNight"),
                            Function.identity());
                    connection.commit();
                } catch (SQLException | RuntimeException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        } catch (SQLException ex) {
            throw new IOException("Could not save resort data to SQLite: "
                    + ex.getMessage(), ex);
        }
    }

    static DataManager.DataSnapshot load(Path database) throws IOException {
        try (Connection connection = open(database)) {
            createSchema(connection);
            DataManager.DataSnapshot snapshot = new DataManager.DataSnapshot();
            snapshot.rooms = readRooms(connection);
            snapshot.guests = readGuests(connection);
            snapshot.staffList = readStaff(connection);
            snapshot.addOns = readAddOns(connection);
            snapshot.accounts = readAccounts(connection);
            snapshot.tasks = readTasks(connection, snapshot.staffList);
            snapshot.bookings = readBookings(connection, snapshot.rooms, snapshot.guests);
            snapshot.bills = readBills(connection, snapshot.bookings);
            snapshot.extraPersonFeePerNight = readExtraPersonFee(connection);
            return snapshot;
        } catch (SQLException | RuntimeException ex) {
            throw new IOException("Could not load resort data from SQLite: "
                    + ex.getMessage(), ex);
        }
    }

    private static Connection open(Path database) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQLite JDBC driver is missing.", ex);
        }
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS app_settings "
                    + "(setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS rooms "
                    + "(room_id TEXT PRIMARY KEY, room_type TEXT NOT NULL, "
                    + "view_type TEXT NOT NULL, rate REAL NOT NULL, capacity INTEGER NOT NULL, "
                    + "max_extra_guests INTEGER NOT NULL, status TEXT NOT NULL, "
                    + "status_before_archive TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS room_amenities "
                    + "(room_id TEXT NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE, "
                    + "position INTEGER NOT NULL, amenity TEXT NOT NULL, "
                    + "PRIMARY KEY(room_id, position))");
            statement.execute("CREATE TABLE IF NOT EXISTS guests "
                    + "(guest_id TEXT PRIMARY KEY, name TEXT NOT NULL, contact_info TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS staff "
                    + "(staff_id TEXT PRIMARY KEY, name TEXT NOT NULL, role TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS add_ons "
                    + "(add_on_id TEXT PRIMARY KEY, name TEXT NOT NULL, price REAL NOT NULL, "
                    + "category TEXT NOT NULL, details TEXT NOT NULL, "
                    + "charge_basis TEXT NOT NULL DEFAULT 'PER_BOOKING')");
            statement.execute("CREATE TABLE IF NOT EXISTS accounts "
                    + "(username TEXT PRIMARY KEY COLLATE NOCASE, password_hash TEXT NOT NULL, "
                    + "staff_id TEXT REFERENCES staff(staff_id) ON DELETE SET NULL, "
                    + "password_change_required INTEGER NOT NULL, administrator INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS tasks "
                    + "(task_id TEXT PRIMARY KEY, description TEXT NOT NULL, status TEXT NOT NULL, "
                    + "assigned_staff_id TEXT REFERENCES staff(staff_id) ON DELETE SET NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS bookings "
                    + "(booking_id TEXT PRIMARY KEY, guest_id TEXT NOT NULL REFERENCES guests(guest_id), "
                    + "room_id TEXT NOT NULL REFERENCES rooms(room_id), check_in TEXT NOT NULL, "
                    + "check_out TEXT NOT NULL, guest_count INTEGER NOT NULL, status TEXT NOT NULL, "
                    + "extra_fee REAL NOT NULL, late_checkout_selected INTEGER NOT NULL, "
                    + "late_checkout_hours INTEGER NOT NULL, late_checkout_fee REAL NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS booking_add_ons "
                    + "(booking_id TEXT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE, "
                    + "position INTEGER NOT NULL, add_on_id TEXT NOT NULL, name TEXT NOT NULL, "
                    + "price REAL NOT NULL, category TEXT NOT NULL, details TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL DEFAULT 1, "
                    + "charge_basis TEXT NOT NULL DEFAULT 'PER_BOOKING', "
                    + "PRIMARY KEY(booking_id, position))");
            statement.execute("CREATE TABLE IF NOT EXISTS bills "
                    + "(bill_id TEXT PRIMARY KEY, booking_id TEXT NOT NULL REFERENCES bookings(booking_id), "
                    + "room_charge REAL NOT NULL, add_on_charge REAL NOT NULL, "
                    + "extra_person_charge REAL NOT NULL, late_checkout_charge REAL NOT NULL, "
                    + "total_amount REAL NOT NULL)");
        }
        ensureColumn(connection, "add_ons", "charge_basis",
                "TEXT NOT NULL DEFAULT 'PER_BOOKING'");
        ensureColumn(connection, "booking_add_ons", "quantity",
                "INTEGER NOT NULL DEFAULT 1");
        ensureColumn(connection, "booking_add_ons", "charge_basis",
                "TEXT NOT NULL DEFAULT 'PER_BOOKING'");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 2");
        }
    }

    private static void ensureColumn(Connection connection, String table, String column,
                                     String definition) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equals(columns.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column
                    + " " + definition);
        }
    }

    private static <T> void removeMissingRows(Connection connection, String table,
                                               String keyColumn, List<T> values,
                                               Function<T, String> keyExtractor)
            throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM ")
                .append(table).append(" WHERE ");
        if (values.isEmpty()) {
            sql.append("1 = 1");
        } else {
            sql.append(keyColumn).append(" NOT IN (");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append('?');
            }
            sql.append(')');
        }
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                statement.setString(i + 1, keyExtractor.apply(values.get(i)));
            }
            statement.executeUpdate();
        }
    }

    private static void writeSettings(Connection connection, Double extraFee)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_settings(setting_key, setting_value) VALUES(?, ?) "
                        + "ON CONFLICT(setting_key) DO UPDATE SET "
                        + "setting_value = excluded.setting_value")) {
            statement.setString(1, "extraPersonFeePerNight");
            statement.setString(2, String.valueOf(extraFee == null ? 500.0 : extraFee));
            statement.executeUpdate();
        }
    }

    private static void writeRooms(Connection connection, List<Room> rooms)
            throws SQLException {
        try (PreparedStatement roomStatement = connection.prepareStatement(
                     "INSERT INTO rooms(room_id, room_type, view_type, rate, capacity, "
                             + "max_extra_guests, status, status_before_archive) "
                             + "VALUES(?, ?, ?, ?, ?, ?, ?, ?) "
                             + "ON CONFLICT(room_id) DO UPDATE SET room_type = excluded.room_type, "
                             + "view_type = excluded.view_type, rate = excluded.rate, "
                             + "capacity = excluded.capacity, "
                             + "max_extra_guests = excluded.max_extra_guests, "
                             + "status = excluded.status, "
                             + "status_before_archive = excluded.status_before_archive");
             PreparedStatement amenityStatement = connection.prepareStatement(
                     "INSERT INTO room_amenities(room_id, position, amenity) "
                             + "VALUES(?, ?, ?) ON CONFLICT(room_id, position) "
                             + "DO UPDATE SET amenity = excluded.amenity")) {
            for (Room room : rooms) {
                roomStatement.setString(1, room.getRoomId());
                roomStatement.setString(2, room.getRoomType());
                roomStatement.setString(3, room.getViewType());
                roomStatement.setDouble(4, room.getRate());
                roomStatement.setInt(5, room.getCapacity());
                roomStatement.setInt(6, room.getMaxExtraGuests());
                roomStatement.setString(7, room.getStatus().name());
                setNullableString(roomStatement, 8,
                        room.getStatusBeforeArchive() == null ? null
                                : room.getStatusBeforeArchive().name());
                roomStatement.executeUpdate();
                try (PreparedStatement deleteAmenities = connection.prepareStatement(
                        "DELETE FROM room_amenities WHERE room_id = ?")) {
                    deleteAmenities.setString(1, room.getRoomId());
                    deleteAmenities.executeUpdate();
                }
                int position = 0;
                for (String amenity : room.getIncludedAmenities()) {
                    amenityStatement.setString(1, room.getRoomId());
                    amenityStatement.setInt(2, position++);
                    amenityStatement.setString(3, amenity);
                    amenityStatement.executeUpdate();
                }
            }
        }
    }

    private static void writeGuests(Connection connection, List<Guest> guests)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO guests(guest_id, name, contact_info) VALUES(?, ?, ?) "
                        + "ON CONFLICT(guest_id) DO UPDATE SET name = excluded.name, "
                        + "contact_info = excluded.contact_info")) {
            for (Guest guest : guests) {
                statement.setString(1, guest.getGuestId());
                statement.setString(2, guest.getName());
                statement.setString(3, guest.getContactInfo());
                statement.executeUpdate();
            }
        }
    }

    private static void writeStaff(Connection connection, List<Staff> staff)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO staff(staff_id, name, role) VALUES(?, ?, ?) "
                        + "ON CONFLICT(staff_id) DO UPDATE SET name = excluded.name, "
                        + "role = excluded.role")) {
            for (Staff member : staff) {
                statement.setString(1, member.getStaffId());
                statement.setString(2, member.getName());
                statement.setString(3, member.getRole());
                statement.executeUpdate();
            }
        }
    }

    private static void writeAddOns(Connection connection, List<AddOn> addOns)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO add_ons(add_on_id, name, price, category, details, charge_basis) "
                        + "VALUES(?, ?, ?, ?, ?, ?) ON CONFLICT(add_on_id) DO UPDATE SET "
                        + "name = excluded.name, price = excluded.price, "
                        + "category = excluded.category, details = excluded.details, "
                        + "charge_basis = excluded.charge_basis")) {
            for (AddOn addOn : addOns) {
                statement.setString(1, addOn.getAddOnId());
                statement.setString(2, addOn.getName());
                statement.setDouble(3, addOn.getPrice());
                statement.setString(4, addOn.getCategory().name());
                statement.setString(5, addOn.getDetails());
                statement.setString(6, addOn.getChargeBasis().name());
                statement.executeUpdate();
            }
        }
    }

    private static void writeAccounts(Connection connection, List<Account> accounts)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO accounts(username, password_hash, staff_id, "
                        + "password_change_required, administrator) VALUES(?, ?, ?, ?, ?) "
                        + "ON CONFLICT(username) DO UPDATE SET "
                        + "password_hash = excluded.password_hash, staff_id = excluded.staff_id, "
                        + "password_change_required = excluded.password_change_required, "
                        + "administrator = excluded.administrator")) {
            for (Account account : accounts) {
                statement.setString(1, account.getUsername());
                statement.setString(2, account.getStoredPassword());
                setNullableString(statement, 3, account.getStaffId());
                statement.setInt(4, account.isPasswordChangeRequired() ? 1 : 0);
                statement.setInt(5, account.isAdmin() ? 1 : 0);
                statement.executeUpdate();
            }
        }
    }

    private static void writeTasks(Connection connection, List<Task> tasks)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tasks(task_id, description, status, assigned_staff_id) "
                        + "VALUES(?, ?, ?, ?) ON CONFLICT(task_id) DO UPDATE SET "
                        + "description = excluded.description, status = excluded.status, "
                        + "assigned_staff_id = excluded.assigned_staff_id")) {
            for (Task task : tasks) {
                statement.setString(1, task.getTaskId());
                statement.setString(2, task.getDescription());
                statement.setString(3, task.getTaskStatus().name());
                setNullableString(statement, 4, task.getAssignedTo() == null ? null
                        : task.getAssignedTo().getStaffId());
                statement.executeUpdate();
            }
        }
    }

    private static void writeBookings(Connection connection, List<Booking> bookings)
            throws SQLException {
        try (PreparedStatement bookingStatement = connection.prepareStatement(
                     "INSERT INTO bookings(booking_id, guest_id, room_id, check_in, "
                             + "check_out, guest_count, status, extra_fee, "
                             + "late_checkout_selected, late_checkout_hours, "
                             + "late_checkout_fee) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                             + "ON CONFLICT(booking_id) DO UPDATE SET "
                             + "guest_id = excluded.guest_id, room_id = excluded.room_id, "
                             + "check_in = excluded.check_in, check_out = excluded.check_out, "
                             + "guest_count = excluded.guest_count, status = excluded.status, "
                             + "extra_fee = excluded.extra_fee, "
                             + "late_checkout_selected = excluded.late_checkout_selected, "
                             + "late_checkout_hours = excluded.late_checkout_hours, "
                             + "late_checkout_fee = excluded.late_checkout_fee");
             PreparedStatement addOnStatement = connection.prepareStatement(
                          "INSERT INTO booking_add_ons(booking_id, position, add_on_id, name, "
                                  + "price, category, details, quantity, charge_basis) "
                                  + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                  + "ON CONFLICT(booking_id, position) DO UPDATE SET "
                                  + "add_on_id = excluded.add_on_id, name = excluded.name, "
                                  + "price = excluded.price, category = excluded.category, "
                                  + "details = excluded.details, quantity = excluded.quantity, "
                                  + "charge_basis = excluded.charge_basis")) {
            for (Booking booking : bookings) {
                bookingStatement.setString(1, booking.getBookingId());
                bookingStatement.setString(2, booking.getGuest().getGuestId());
                bookingStatement.setString(3, booking.getRoom().getRoomId());
                bookingStatement.setString(4, booking.getCheckInDate().toString());
                bookingStatement.setString(5, booking.getCheckOutDate().toString());
                bookingStatement.setInt(6, booking.getGuestCount());
                bookingStatement.setString(7, booking.getStatus().name());
                bookingStatement.setDouble(8, booking.getExtraPersonFeePerNight());
                bookingStatement.setInt(9, booking.isLateCheckoutSelected() ? 1 : 0);
                bookingStatement.setInt(10, booking.getLateCheckoutHours());
                bookingStatement.setDouble(11, booking.getLateCheckoutFeePerHour());
                bookingStatement.executeUpdate();
                try (PreparedStatement deleteAddOns = connection.prepareStatement(
                        "DELETE FROM booking_add_ons WHERE booking_id = ?")) {
                    deleteAddOns.setString(1, booking.getBookingId());
                    deleteAddOns.executeUpdate();
                }
                int position = 0;
                for (AddOn addOn : booking.getAddOns()) {
                    addOnStatement.setString(1, booking.getBookingId());
                    addOnStatement.setInt(2, position++);
                    addOnStatement.setString(3, addOn.getAddOnId());
                    addOnStatement.setString(4, addOn.getName());
                    addOnStatement.setDouble(5, addOn.getPrice());
                    addOnStatement.setString(6, addOn.getCategory().name());
                    addOnStatement.setString(7, addOn.getDetails());
                    addOnStatement.setInt(8, booking.getAddOnQuantity(addOn));
                    addOnStatement.setString(9, addOn.getChargeBasis().name());
                    addOnStatement.executeUpdate();
                }
            }
        }
    }

    private static void writeBills(Connection connection, List<Bill> bills)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bills(bill_id, booking_id, room_charge, add_on_charge, "
                        + "extra_person_charge, late_checkout_charge, total_amount) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?) ON CONFLICT(bill_id) DO UPDATE SET "
                        + "booking_id = excluded.booking_id, room_charge = excluded.room_charge, "
                        + "add_on_charge = excluded.add_on_charge, "
                        + "extra_person_charge = excluded.extra_person_charge, "
                        + "late_checkout_charge = excluded.late_checkout_charge, "
                        + "total_amount = excluded.total_amount")) {
            for (Bill bill : bills) {
                statement.setString(1, bill.getBillId());
                statement.setString(2, bill.getBooking().getBookingId());
                statement.setDouble(3, bill.getRoomCharge());
                statement.setDouble(4, bill.getAddOnCharge());
                statement.setDouble(5, bill.getExtraPersonCharge());
                statement.setDouble(6, bill.getLateCheckoutCharge());
                statement.setDouble(7, bill.getTotalAmount());
                statement.executeUpdate();
            }
        }
    }

    private static List<Room> readRooms(Connection connection) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        Map<String, Room> byId = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM rooms")) {
            while (rows.next()) {
                Room room = new Room(rows.getString("room_id"),
                        rows.getString("room_type"), rows.getString("view_type"),
                        rows.getDouble("rate"), rows.getInt("capacity"),
                        rows.getInt("max_extra_guests"));
                room.setStatus(RoomStatus.valueOf(rows.getString("status")));
                String previousStatus = rows.getString("status_before_archive");
                if (previousStatus != null) {
                    room.setStatusBeforeArchive(RoomStatus.valueOf(previousStatus));
                }
                rooms.add(room);
                byId.put(room.getRoomId(), room);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT room_id, amenity FROM room_amenities ORDER BY room_id, position")) {
            Map<String, List<String>> amenities = new HashMap<>();
            while (rows.next()) {
                amenities.computeIfAbsent(rows.getString("room_id"),
                        ignored -> new ArrayList<>()).add(rows.getString("amenity"));
            }
            amenities.forEach((roomId, values) -> byId.get(roomId).setIncludedAmenities(values));
        }
        return rooms;
    }

    private static List<Guest> readGuests(Connection connection) throws SQLException {
        List<Guest> guests = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM guests")) {
            while (rows.next()) {
                guests.add(new Guest(rows.getString("guest_id"),
                        rows.getString("name"), rows.getString("contact_info")));
            }
        }
        return guests;
    }

    private static List<Staff> readStaff(Connection connection) throws SQLException {
        List<Staff> staff = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM staff")) {
            while (rows.next()) {
                staff.add(new Staff(rows.getString("staff_id"),
                        rows.getString("name"), rows.getString("role")));
            }
        }
        return staff;
    }

    private static List<AddOn> readAddOns(Connection connection) throws SQLException {
        List<AddOn> addOns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM add_ons")) {
            while (rows.next()) {
                addOns.add(readAddOn(rows));
            }
        }
        return addOns;
    }

    private static List<Account> readAccounts(Connection connection) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM accounts")) {
            while (rows.next()) {
                boolean administrator = rows.getInt("administrator") != 0;
                Account account = administrator
                        ? new Admin(rows.getString("username"))
                        : new Employee(rows.getString("username"));
                account.restorePasswordState(rows.getString("password_hash"),
                        rows.getInt("password_change_required") != 0);
                account.setStaffId(rows.getString("staff_id"));
                accounts.add(account);
            }
        }
        return accounts;
    }

    private static List<Task> readTasks(Connection connection, List<Staff> staffList)
            throws SQLException {
        Map<String, Staff> staffById = new HashMap<>();
        for (Staff staff : staffList) {
            staffById.put(staff.getStaffId(), staff);
        }
        List<Task> tasks = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM tasks")) {
            while (rows.next()) {
                Task task = new Task(rows.getString("task_id"),
                        rows.getString("description"), null);
                task.setStatus(TaskStatus.valueOf(rows.getString("status")));
                String staffId = rows.getString("assigned_staff_id");
                Staff assignedStaff = staffId == null ? null : staffById.get(staffId);
                if (assignedStaff != null) {
                    assignedStaff.assignTask(task);
                }
                tasks.add(task);
            }
        }
        return tasks;
    }

    private static List<Booking> readBookings(Connection connection, List<Room> rooms,
                                               List<Guest> guests)
            throws SQLException {
        Map<String, Room> roomsById = new HashMap<>();
        Map<String, Guest> guestsById = new HashMap<>();
        for (Room room : rooms) roomsById.put(room.getRoomId(), room);
        for (Guest guest : guests) guestsById.put(guest.getGuestId(), guest);

        List<Booking> bookings = new ArrayList<>();
        Map<String, Booking> bookingsById = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM bookings")) {
            while (rows.next()) {
                Booking booking = new Booking(rows.getString("booking_id"),
                        guestsById.get(rows.getString("guest_id")),
                        roomsById.get(rows.getString("room_id")),
                        LocalDate.parse(rows.getString("check_in")),
                        LocalDate.parse(rows.getString("check_out")),
                        rows.getInt("guest_count"), rows.getDouble("extra_fee"));
                booking.restorePersistentState(
                        BookingStatus.valueOf(rows.getString("status")),
                        rows.getInt("late_checkout_selected") != 0,
                        rows.getInt("late_checkout_hours"),
                        rows.getDouble("late_checkout_fee"));
                bookings.add(booking);
                bookingsById.put(booking.getBookingId(), booking);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT * FROM booking_add_ons ORDER BY booking_id, position")) {
            while (rows.next()) {
                Booking booking = bookingsById.get(rows.getString("booking_id"));
                String addOnId = rows.getString("add_on_id");
                AddOn addOn = new AddOn(addOnId, rows.getString("name"),
                        rows.getDouble("price"),
                        AddOnCategory.valueOf(rows.getString("category")),
                        rows.getString("details"),
                        AddOnChargeBasis.valueOf(rows.getString("charge_basis")));
                booking.addAddOn(addOn, rows.getInt("quantity"));
            }
        }
        return bookings;
    }

    private static List<Bill> readBills(Connection connection, List<Booking> bookings)
            throws SQLException {
        Map<String, Booking> bookingsById = new HashMap<>();
        for (Booking booking : bookings) {
            bookingsById.put(booking.getBookingId(), booking);
        }
        List<Bill> bills = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM bills")) {
            while (rows.next()) {
                Bill bill = new Bill(rows.getString("bill_id"),
                        bookingsById.get(rows.getString("booking_id")));
                bill.restoreAmounts(rows.getDouble("room_charge"),
                        rows.getDouble("add_on_charge"),
                        rows.getDouble("extra_person_charge"),
                        rows.getDouble("late_checkout_charge"),
                        rows.getDouble("total_amount"));
                bills.add(bill);
            }
        }
        return bills;
    }

    private static Double readExtraPersonFee(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key = ?")) {
            statement.setString(1, "extraPersonFeePerNight");
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Double.valueOf(row.getString(1)) : 500.0;
            }
        }
    }

    private static AddOn readAddOn(ResultSet row) throws SQLException {
        return new AddOn(row.getString("add_on_id"), row.getString("name"),
                row.getDouble("price"),
                AddOnCategory.valueOf(row.getString("category")),
                row.getString("details"),
                AddOnChargeBasis.valueOf(row.getString("charge_basis")));
    }

    private static void setNullableString(PreparedStatement statement, int index,
                                          String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
