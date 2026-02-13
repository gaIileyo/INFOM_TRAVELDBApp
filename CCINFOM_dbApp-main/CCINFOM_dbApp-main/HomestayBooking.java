import java.awt.BorderLayout;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class HomestayBooking extends JFrame {

    private Connection conn;
    private static final String DEFAULT_DB_NAME = "homestay_tour_system";
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_CONNECTOR_JAR = "mysql-connector-j-9.5.0.jar";

    public HomestayBooking() {
        connectDatabase();

        setTitle("Homestay Booking & Tour Management");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // Tabs for management
        tabs.add("Homestay Management", homestayPanel());
        tabs.add("Guide Management", guidePanel());
        tabs.add("Tour Package Management", tourPanel());
        tabs.add("Guest Management", guestPanel());
        tabs.add("Guest Transaction", transactionPanel());
        tabs.add("Booking History Report", reportPanel());

        add(tabs);
        setVisible(true);
    }

    private void connectDatabase() {
        try {
            ensureMySqlDriverLoaded();
            ensureDatabaseExists();
            conn = DriverManager.getConnection(
                    buildJdbcUrl(true),
                    DEFAULT_DB_USER, DEFAULT_DB_PASSWORD
            );
            ensureTourPackageTableExists();
            ensureBaseTourPackagesLoaded();
            ensureTransactionTablesExist();
            ensureBaseTransactionsLoaded();
            System.out.println("Database connected successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database connection failed:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ensureDatabaseExists() throws SQLException {
        String createDatabaseSql = "CREATE DATABASE IF NOT EXISTS `" + DEFAULT_DB_NAME + "`";
        try (Connection serverConnection = DriverManager.getConnection(
                buildJdbcUrl(false),
                DEFAULT_DB_USER,
                DEFAULT_DB_PASSWORD
        );
             Statement statement = serverConnection.createStatement()) {
            statement.executeUpdate(createDatabaseSql);
        }
    }

    private String buildJdbcUrl(boolean includeDatabase) {
        String baseUrl = "jdbc:mysql://" + DEFAULT_DB_HOST + ":" + DEFAULT_DB_PORT;
        if (includeDatabase) {
            return baseUrl + "/" + DEFAULT_DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        return baseUrl + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private void ensureTourPackageTableExists() throws SQLException {
        String createTourPackageTableSql = "CREATE TABLE IF NOT EXISTS tour_package ("
                + "package_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "package_name VARCHAR(100) NOT NULL, "
                + "category ENUM('City Tour','Heritage','Food') NOT NULL, "
                + "duration VARCHAR(50) NOT NULL, "
                + "price DECIMAL(10,2) NOT NULL, "
                + "max_guests INT NOT NULL, "
                + "inclusions TEXT"
                + ")";

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(createTourPackageTableSql);
        }
    }

    private void ensureBaseTourPackagesLoaded() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM tour_package";
        try (Statement countStatement = conn.createStatement();
             ResultSet resultSet = countStatement.executeQuery(countSql)) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO tour_package (package_name, category, duration, price, max_guests, inclusions) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement insertStatement = conn.prepareStatement(insertSql)) {
            addBasePackage(insertStatement, "Manila Heritage Walk", "Heritage", "Half Day", 1200.00, 15,
                    "Licensed guide, museum entry, bottled water");
            addBasePackage(insertStatement, "Intramuros City Highlights", "City Tour", "1 Day", 1800.00, 20,
                    "Transport, guide, lunch");
            addBasePackage(insertStatement, "Binondo Food Adventure", "Food", "Evening", 1500.00, 12,
                    "Food tastings, guide, local maps");
            insertStatement.executeBatch();
        }
    }

    private void addBasePackage(PreparedStatement statement,
                                String packageName,
                                String category,
                                String duration,
                                double price,
                                int maxGuests,
                                String inclusions) throws SQLException {
        statement.setString(1, packageName);
        statement.setString(2, category);
        statement.setString(3, duration);
        statement.setDouble(4, price);
        statement.setInt(5, maxGuests);
        statement.setString(6, inclusions);
        statement.addBatch();
    }

    private void ensureTransactionTablesExist() throws SQLException {
        ensureGuestTableExists();
        ensureHomestayTableExists();

        String createBookingTransactionTableSql = "CREATE TABLE IF NOT EXISTS booking_transaction ("
                + "booking_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "guest_id INT NOT NULL, "
                + "property_id INT NOT NULL, "
                + "check_in_date DATE NOT NULL, "
                + "check_out_date DATE NOT NULL, "
                + "total_stay_cost DECIMAL(10,2) NOT NULL, "
                + "status ENUM('Confirmed','Pending') NOT NULL, "
                + "FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE ON UPDATE CASCADE, "
                + "FOREIGN KEY (property_id) REFERENCES homestay(property_id) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ")";

        String createBookingTransactionWithoutFkSql = "CREATE TABLE IF NOT EXISTS booking_transaction ("
            + "booking_id INT AUTO_INCREMENT PRIMARY KEY, "
            + "guest_id INT NOT NULL, "
            + "property_id INT NOT NULL, "
            + "check_in_date DATE NOT NULL, "
            + "check_out_date DATE NOT NULL, "
            + "total_stay_cost DECIMAL(10,2) NOT NULL, "
            + "status ENUM('Confirmed','Pending') NOT NULL"
            + ")";

        String createGuestActivityTableSql = "CREATE TABLE IF NOT EXISTS guest_activity_transaction ("
                + "activity_transaction_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "guest_id INT NOT NULL, "
                + "service_type ENUM('Accommodation','Tour','Combined') NOT NULL, "
                + "reference_id INT NOT NULL, "
                + "activity_start_date DATE NOT NULL, "
                + "activity_end_date DATE NOT NULL, "
                + "base_amount DECIMAL(10,2) NOT NULL, "
                + "additional_charges DECIMAL(10,2) DEFAULT 0, "
                + "discount_applied DECIMAL(10,2) DEFAULT 0, "
                + "final_amount DECIMAL(10,2) NOT NULL, "
                + "payment_method ENUM('Cash','GCash','Credit Card','Bank Transfer') NOT NULL, "
                + "payment_confirmation_status ENUM('Paid','Pending','Refunded') NOT NULL, "
                + "FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE ON UPDATE CASCADE"
                + ")";

        String createGuestActivityWithoutFkSql = "CREATE TABLE IF NOT EXISTS guest_activity_transaction ("
                + "activity_transaction_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "guest_id INT NOT NULL, "
                + "service_type ENUM('Accommodation','Tour','Combined') NOT NULL, "
                + "reference_id INT NOT NULL, "
                + "activity_start_date DATE NOT NULL, "
                + "activity_end_date DATE NOT NULL, "
                + "base_amount DECIMAL(10,2) NOT NULL, "
                + "additional_charges DECIMAL(10,2) DEFAULT 0, "
                + "discount_applied DECIMAL(10,2) DEFAULT 0, "
                + "final_amount DECIMAL(10,2) NOT NULL, "
                + "payment_method ENUM('Cash','GCash','Credit Card','Bank Transfer') NOT NULL, "
                + "payment_confirmation_status ENUM('Paid','Pending','Refunded') NOT NULL"
                + ")";

        try (Statement statement = conn.createStatement()) {
            try {
                statement.executeUpdate(createBookingTransactionTableSql);
            } catch (SQLException fkError) {
                statement.executeUpdate(createBookingTransactionWithoutFkSql);
            }

            try {
                statement.executeUpdate(createGuestActivityTableSql);
            } catch (SQLException fkError) {
                statement.executeUpdate(createGuestActivityWithoutFkSql);
            }
        }
    }

    private void ensureGuestTableExists() throws SQLException {
        String createGuestTableSql = "CREATE TABLE IF NOT EXISTS guest ("
                + "guest_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "first_name VARCHAR(50) NOT NULL, "
                + "last_name VARCHAR(50) NOT NULL, "
                + "address VARCHAR(100) NOT NULL, "
                + "contact_number VARCHAR(30) NOT NULL, "
                + "email VARCHAR(100) NOT NULL, "
                + "valid_id_status ENUM('Verified','Not Verified') NOT NULL, "
                + "trust_rating INT NOT NULL"
                + ")";

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(createGuestTableSql);
        }
    }

    private void ensureHomestayTableExists() throws SQLException {
        String createHomestayTableSql = "CREATE TABLE IF NOT EXISTS homestay ("
                + "property_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "property_name VARCHAR(100) NOT NULL, "
                + "host_name VARCHAR(100) NOT NULL, "
                + "room_type ENUM('Solo','Family') NOT NULL, "
                + "address VARCHAR(150) NOT NULL, "
                + "room_capacity INT NOT NULL, "
                + "price_per_night DECIMAL(10,2) NOT NULL, "
                + "amenities TEXT, "
                + "availability_status ENUM('Available','Booked') DEFAULT 'Available'"
                + ")";

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(createHomestayTableSql);
        }
    }

    private void ensureBaseTransactionsLoaded() throws SQLException {
        if (hasAnyRows("guest_activity_transaction")) {
            return;
        }

        int guestId = ensureBaseGuest();
        int propertyId = ensureBaseHomestay();
        int bookingId = ensureBaseBookingTransaction(guestId, propertyId);

        String insertActivitySql = "INSERT INTO guest_activity_transaction (guest_id, service_type, reference_id, activity_start_date, activity_end_date, base_amount, additional_charges, discount_applied, final_amount, payment_method, payment_confirmation_status) VALUES (?, 'Accommodation', ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2500.00, 0.00, 0.00, 2500.00, 'Cash', 'Paid')";
        try (PreparedStatement statement = conn.prepareStatement(insertActivitySql)) {
            statement.setInt(1, guestId);
            statement.setInt(2, bookingId);
            statement.executeUpdate();
        }
    }

    private boolean hasAnyRows(String tableName) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM " + tableName + " LIMIT 1)";
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() && resultSet.getInt(1) == 1;
        }
    }

    private int ensureBaseGuest() throws SQLException {
        Integer existingGuestId = getFirstId("guest", "guest_id");
        if (existingGuestId != null) {
            return existingGuestId;
        }

        String insertGuestSql = "INSERT INTO guest (first_name, last_name, address, contact_number, email, valid_id_status, trust_rating) VALUES ('Sample', 'Guest', 'Metro Manila', '09171234567', 'sample.guest@example.com', 'Verified', 5)";
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(insertGuestSql);
        }
        Integer guestId = getFirstId("guest", "guest_id");
        if (guestId == null) {
            throw new SQLException("Unable to create base guest record.");
        }
        return guestId;
    }

    private int ensureBaseHomestay() throws SQLException {
        Integer existingPropertyId = getFirstId("homestay", "property_id");
        if (existingPropertyId != null) {
            return existingPropertyId;
        }

        String insertHomestaySql = "INSERT INTO homestay (property_name, host_name, room_type, address, room_capacity, price_per_night, amenities, availability_status) VALUES ('Sample Homestay', 'Default Host', 'Solo', 'Quezon City', 2, 2500.00, 'WiFi, Aircon', 'Available')";
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(insertHomestaySql);
        }
        Integer propertyId = getFirstId("homestay", "property_id");
        if (propertyId == null) {
            throw new SQLException("Unable to create base homestay record.");
        }
        return propertyId;
    }

    private int ensureBaseBookingTransaction(int guestId, int propertyId) throws SQLException {
        Integer existingBookingId = getFirstId("booking_transaction", "booking_id");
        if (existingBookingId != null) {
            return existingBookingId;
        }

        String insertBookingSql = "INSERT INTO booking_transaction (guest_id, property_id, check_in_date, check_out_date, total_stay_cost, status) VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2500.00, 'Confirmed')";
        try (PreparedStatement statement = conn.prepareStatement(insertBookingSql)) {
            statement.setInt(1, guestId);
            statement.setInt(2, propertyId);
            statement.executeUpdate();
        }
        Integer bookingId = getFirstId("booking_transaction", "booking_id");
        if (bookingId == null) {
            throw new SQLException("Unable to create base booking transaction record.");
        }
        return bookingId;
    }

    private Integer getFirstId(String tableName, String idColumn) throws SQLException {
        String sql = "SELECT " + idColumn + " FROM " + tableName + " ORDER BY " + idColumn + " ASC LIMIT 1";
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return null;
        }
    }

    private void ensureMySqlDriverLoaded() throws ClassNotFoundException, SQLException {
        try {
            Class.forName(MYSQL_DRIVER_CLASS);
            return;
        } catch (ClassNotFoundException ignored) {
            try {
                File[] candidates = new File[]{
                        new File("lib" + File.separator + MYSQL_CONNECTOR_JAR),
                    new File("CCINFOM_dbApp-main" + File.separator + "lib" + File.separator + MYSQL_CONNECTOR_JAR),
                        new File("InfomA" + File.separator + "lib" + File.separator + MYSQL_CONNECTOR_JAR),
                    new File("InfomA" + File.separator + "dbApp" + File.separator + "lib" + File.separator + MYSQL_CONNECTOR_JAR),
                    new File("CCINFOM_dbApp-main" + File.separator + "InfomA" + File.separator + "lib" + File.separator + MYSQL_CONNECTOR_JAR),
                    new File("CCINFOM_dbApp-main" + File.separator + "InfomA" + File.separator + "dbApp" + File.separator + "lib" + File.separator + MYSQL_CONNECTOR_JAR)
                };

                for (File jar : candidates) {
                    if (!jar.exists()) {
                        continue;
                    }

                    URL jarUrl = jar.toURI().toURL();
                    URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
                    Class<?> driverClass = Class.forName(MYSQL_DRIVER_CLASS, true, loader);
                    Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                    DriverManager.registerDriver(new DriverShim(driver));
                    return;
                }

                throw new ClassNotFoundException(
                    "MySQL JDBC driver not found. Expected: " + MYSQL_CONNECTOR_JAR
                        + " under /lib or /CCINFOM_dbApp-main/lib. Current user.dir="
                        + System.getProperty("user.dir")
                );
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new ClassNotFoundException("Unable to load MySQL JDBC driver: " + e.getMessage(), e);
            }
        }
    }

    private static final class DriverShim implements Driver {
        private final Driver delegate;

        private DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() {
            try {
                return delegate.getParentLogger();
            } catch (SQLFeatureNotSupportedException ignored) {
                return Logger.getGlobal();
            }
        }
    }

    private JPanel homestayPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        model.setColumnIdentifiers(new String[]{"Property ID", "Name", "Host", "Address", "Price/Night"});

        JButton loadBtn = new JButton("Load Homestays");
        JButton addBtn = new JButton("Add Homestay");

        loadBtn.addActionListener(e -> loadHomestays(model));

        addBtn.addActionListener(e -> {
            JTextField idField = new JTextField();
            JTextField nameField = new JTextField();
            JTextField hostField = new JTextField();
            JTextField addressField = new JTextField();
            JTextField priceField = new JTextField();

            Object[] message = {
                    "Property ID:", idField,
                    "Name:", nameField,
                    "Host Name:", hostField,
                    "Address:", addressField,
                    "Price per Night:", priceField
            };

            int option = JOptionPane.showConfirmDialog(this, message, "Add Homestay", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO homestay (property_id, property_name, host_name, address, price_per_night) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, idField.getText());
                    ps.setString(2, nameField.getText());
                    ps.setString(3, hostField.getText());
                    ps.setString(4, addressField.getText());
                    ps.setDouble(5, Double.parseDouble(priceField.getText()));
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Homestay added successfully.");
                    loadHomestays(model);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error adding homestay: " + ex.getMessage());
                }
            }
        });

        JPanel top = new JPanel();
        top.add(loadBtn);
        top.add(addBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void loadHomestays(DefaultTableModel model) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM homestay")) {
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("property_id"),
                        rs.getString("property_name"),
                        rs.getString("host_name"),
                        rs.getString("address"),
                        rs.getDouble("price_per_night")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading homestays: " + ex.getMessage());
        }
    }

    private JPanel guidePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        model.setColumnIdentifiers(new String[]{"Guide ID", "First Name", "Last Name", "Specialization", "Daily Rate"});

        JButton loadBtn = new JButton("Load Guides");
        JButton addBtn = new JButton("Add Guide");

        loadBtn.addActionListener(e -> loadGuides(model));

        addBtn.addActionListener(e -> {
            JTextField idField = new JTextField();
            JTextField fname = new JTextField();
            JTextField lname = new JTextField();
            JTextField spec = new JTextField();
            JTextField rate = new JTextField();

            Object[] msg = {
                    "Guide ID:", idField,
                    "First Name:", fname,
                    "Last Name:", lname,
                    "Specialization:", spec,
                    "Daily Rate:", rate
            };

            int option = JOptionPane.showConfirmDialog(this, msg, "Add Guide", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO guide (guide_id, first_name, last_name, specialization, daily_service_rate) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, idField.getText());
                    ps.setString(2, fname.getText());
                    ps.setString(3, lname.getText());
                    ps.setString(4, spec.getText());
                    ps.setDouble(5, Double.parseDouble(rate.getText()));
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Guide added successfully.");
                    loadGuides(model);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error adding guide: " + ex.getMessage());
                }
            }
        });

        JPanel top = new JPanel();
        top.add(loadBtn);
        top.add(addBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadGuides(DefaultTableModel model) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM guide")) {
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("guide_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("specialization"),
                        rs.getDouble("daily_service_rate")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading guides: " + ex.getMessage());
        }
    }

    private JPanel tourPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    DefaultTableModel model = new DefaultTableModel();
    JTable table = new JTable(model);

    model.setColumnIdentifiers(new String[]{"Package ID", "Package Name", "Category", "Duration", "Price", "Max Guests", "Inclusions"});

    JButton loadBtn = new JButton("Load Packages");
    JButton addBtn = new JButton("Add Package");

    // Load button action
    loadBtn.addActionListener(e -> loadPackages(model));

    // Add package action
    addBtn.addActionListener(e -> {
        JTextField pname = new JTextField();
        JComboBox<String> category = new JComboBox<>(new String[]{"City Tour", "Heritage", "Food"});
        JTextField duration = new JTextField();
        JTextField price = new JTextField();
        JTextField maxGuests = new JTextField();
        JTextField inclusions = new JTextField();

        Object[] msg = {
                "Package Name:", pname,
                "Category:", category,
                "Duration:", duration,
                "Price:", price,
                "Max Guests:", maxGuests,
                "Inclusions:", inclusions
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Add Tour Package", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tour_package (package_name, category, duration, price, max_guests, inclusions) VALUES (?,?,?,?,?,?)")) {

                ps.setString(1, pname.getText().trim());
                ps.setString(2, String.valueOf(category.getSelectedItem()));
                ps.setString(3, duration.getText().trim());
                ps.setDouble(4, Double.parseDouble(price.getText().trim()));
                ps.setInt(5, Integer.parseInt(maxGuests.getText().trim()));
                ps.setString(6, inclusions.getText().trim());

                ps.executeUpdate();

                // Automatically reload table after adding
                loadPackages(model);

                JOptionPane.showMessageDialog(this, "Package added successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding package: " + ex.getMessage());
            }
        }
    });

    JPanel top = new JPanel();
    top.add(loadBtn);
    top.add(addBtn);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    // Load packages initially
    loadPackages(model);

    return panel;
}

    private void loadPackages(DefaultTableModel model) {
        model.setRowCount(0);
        addInMemoryBasePackages(model);

        if (conn == null) {
            return;
        }

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tour_package")) {
            model.setRowCount(0);
            int loadedRows = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("package_id"),
                        rs.getString("package_name"),
                        rs.getString("category"),
                        rs.getString("duration"),
                        rs.getDouble("price"),
                        rs.getInt("max_guests"),
                        rs.getString("inclusions")
                });
                loadedRows++;
            }

            if (loadedRows == 0) {
                try {
                    ensureBaseTourPackagesLoaded();
                    model.setRowCount(0);
                    try (ResultSet reloaded = st.executeQuery("SELECT * FROM tour_package")) {
                        while (reloaded.next()) {
                            model.addRow(new Object[]{
                                    reloaded.getString("package_id"),
                                    reloaded.getString("package_name"),
                                    reloaded.getString("category"),
                                    reloaded.getString("duration"),
                                    reloaded.getDouble("price"),
                                    reloaded.getInt("max_guests"),
                                    reloaded.getString("inclusions")
                            });
                        }
                    }

                    if (model.getRowCount() == 0) {
                        addInMemoryBasePackages(model);
                    }
                } catch (SQLException ignored) {
                    model.setRowCount(0);
                    addInMemoryBasePackages(model);
                }
            }
        } catch (Exception ex) {
            model.setRowCount(0);
            addInMemoryBasePackages(model);
            JOptionPane.showMessageDialog(this, "Error loading packages from DB. Showing default packages instead: " + ex.getMessage());
        }
    }

    private void addInMemoryBasePackages(DefaultTableModel model) {
        if (model.getRowCount() > 0) {
            return;
        }

        model.addRow(new Object[]{1, "Manila Heritage Walk", "Heritage", "Half Day", 1200.00, 15,
                "Licensed guide, museum entry, bottled water"});
        model.addRow(new Object[]{2, "Intramuros City Highlights", "City Tour", "1 Day", 1800.00, 20,
                "Transport, guide, lunch"});
        model.addRow(new Object[]{3, "Binondo Food Adventure", "Food", "Evening", 1500.00, 12,
                "Food tastings, guide, local maps"});
    }

    private JPanel guestPanel() {
        return createReadOnlyPanel("Load Guests", "SELECT * FROM guest");
    }

    private JPanel transactionPanel() {
        String transactionQuery = "SELECT activity_transaction_id, guest_id, service_type, reference_id, activity_start_date, activity_end_date, " +
                "base_amount, additional_charges, discount_applied, final_amount, payment_method, payment_confirmation_status " +
                "FROM guest_activity_transaction ORDER BY activity_start_date ASC, activity_transaction_id ASC";

        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        JButton loadBtn = new JButton("Load Transactions");
        JButton deleteBtn = new JButton("Delete Selected");

        loadBtn.addActionListener(e -> loadDynamicTable(model, transactionQuery));
        deleteBtn.addActionListener(e -> deleteSelectedTransaction(table, model, transactionQuery));

        JPanel top = new JPanel();
        top.add(loadBtn);
        top.add(deleteBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        loadDynamicTable(model, transactionQuery);
        return panel;
    }

    private void deleteSelectedTransaction(JTable table, DefaultTableModel model, String reloadQuery) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a transaction row to delete.");
            return;
        }

        Object idValue = table.getValueAt(selectedRow, 0);
        if (!(idValue instanceof Number)) {
            JOptionPane.showMessageDialog(this, "Invalid transaction ID.");
            return;
        }

        int transactionId = ((Number) idValue).intValue();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete transaction ID " + transactionId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String deleteSql = "DELETE FROM guest_activity_transaction WHERE activity_transaction_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(deleteSql)) {
            statement.setInt(1, transactionId);
            int deleted = statement.executeUpdate();
            if (deleted > 0) {
                loadDynamicTable(model, reloadQuery);
                JOptionPane.showMessageDialog(this, "Transaction deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Transaction not found.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting transaction: " + ex.getMessage());
        }
    }

    private JPanel reportPanel() {
        return createReadOnlyPanel(
                "Load Booking History",
                "SELECT t.activity_transaction_id AS transaction_id, " +
                        "CONCAT(g.first_name, ' ', g.last_name) AS guest_name, " +
                        "t.service_type, t.activity_start_date, t.activity_end_date, t.final_amount, " +
                        "t.payment_confirmation_status AS payment_status " +
                        "FROM guest_activity_transaction t " +
                        "JOIN guest g ON g.guest_id = t.guest_id " +
                "ORDER BY t.activity_start_date ASC, t.activity_transaction_id ASC",
            true
        );
    }

    private JPanel createReadOnlyPanel(String buttonText, String query) {
        return createReadOnlyPanel(buttonText, query, false);
        }

        private JPanel createReadOnlyPanel(String buttonText, String query, boolean autoLoad) {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        JButton loadBtn = new JButton(buttonText);
        loadBtn.addActionListener(e -> loadDynamicTable(model, query));

        JPanel top = new JPanel();
        top.add(loadBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        if (autoLoad) {
            loadDynamicTable(model, query);
        }
        return panel;
    }

    private void loadDynamicTable(DefaultTableModel model, String query) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            String[] columns = new String[columnCount];
            for (int index = 1; index <= columnCount; index++) {
                columns[index - 1] = metaData.getColumnLabel(index);
            }
            model.setColumnIdentifiers(columns);
            model.setRowCount(0);

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int index = 1; index <= columnCount; index++) {
                    row[index - 1] = rs.getObject(index);
                }
                model.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage());
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomestayBooking::new);
    }
}




