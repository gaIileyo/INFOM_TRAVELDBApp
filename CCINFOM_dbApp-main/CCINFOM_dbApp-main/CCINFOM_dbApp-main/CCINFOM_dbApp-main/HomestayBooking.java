import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_CONNECTOR_JAR = "mysql-connector-j-9.5.0.jar";

    public HomestayBooking() {

        connectDatabase();

        setTitle("Homestay & Tour Management System");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Guest Management", guestPanel());
        tabs.add("Guest Transaction", transactionPanel());
        tabs.add("Booking History Report", reportPanel());

        add(tabs);
        setVisible(true);
    }

    private void connectDatabase() {
        if (!ensureMySqlDriverAvailable()) {
            JOptionPane.showMessageDialog(this,
                    "MySQL JDBC driver not found.\n\n" +
                    "If you run from VS Code, use run.ps1 or run.bat so the connector JAR\n" +
                    "is included in the classpath (lib/mysql-connector-j-9.5.0.jar).",
                    "Missing JDBC Driver",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String dbName = getEnvOrDefault("DB_NAME", DEFAULT_DB_NAME);
        String host = getEnvOrDefault("DB_HOST", DEFAULT_DB_HOST);
        String port = getEnvOrDefault("DB_PORT", DEFAULT_DB_PORT);
        String user = getEnvOrDefault("DB_USER", DEFAULT_DB_USER);
        String envPassword = System.getenv("DB_PASSWORD");
        String initialPassword = (envPassword == null) ? "" : envPassword;
        Exception lastException = null;
        try {
            conn = DriverManager.getConnection(buildJdbcUrl(host, port, dbName), user, initialPassword);
            System.out.println("Database Connected Successfully with " +
                    (envPassword == null ? "empty password" : "env password") + ".");
            return;
        } catch (Exception e) {
            // fall through to prompt
            lastException = e;
        }

        String promptPassword = JOptionPane.showInputDialog(
                this,
                "XAMPP MySQL password for user '" + user + "' (leave blank if none):",
                "Database Password",
                JOptionPane.QUESTION_MESSAGE
        );
        if (promptPassword != null) {
            try {
                conn = DriverManager.getConnection(buildJdbcUrl(host, port, dbName), user, promptPassword);
                System.out.println("Database Connected Successfully with prompt password.");
                return;
            } catch (Exception e) {
                // fall through to error message
                lastException = e;
            }
        }
        
        // If all passwords failed
        System.err.println("Cannot connect to " + dbName + " with the provided password");
        System.err.println("\nTo fix this:");
        System.err.println("1. Start MySQL in XAMPP Control Panel");
        System.err.println("2. Open phpMyAdmin (http://localhost/phpmyadmin)");
        System.err.println("3. Import file: infom_db.sql from your project");
        System.err.println("\nOr run in terminal (replace PASSWORD with your MySQL root password):");
        System.err.println("mysql -u root -pPASSWORD < infom_db.sql");
        
        String errorDetails = (lastException == null) ? "(no details)" : lastException.getMessage();
        JOptionPane.showMessageDialog(this,
                "Database Connection Failed.\n\n" +
                "The provided password did not work.\n" +
                "You may need to start MySQL in XAMPP and import the schema first.\n" +
                "Open phpMyAdmin and import infom_db.sql.\n\n" +
                "Error: " + errorDetails,
                "Database Setup Required",
                JOptionPane.WARNING_MESSAGE);
    }

    private static boolean isMySqlDriverAvailable() {
        try {
            Class.forName(MYSQL_DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean ensureMySqlDriverAvailable() {
        if (isMySqlDriverAvailable()) {
            return true;
        }

        String[] candidatePaths = {
                "lib/" + MYSQL_CONNECTOR_JAR,
                "CCINFOM_dbApp-main/lib/" + MYSQL_CONNECTOR_JAR,
                "InfomA/lib/" + MYSQL_CONNECTOR_JAR
        };

        for (String path : candidatePaths) {
            File jar = new File(path);
            if (!jar.exists()) {
                continue;
            }
            try {
                URL jarUrl = jar.toURI().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, HomestayBooking.class.getClassLoader());
                Class<?> driverClass = Class.forName(MYSQL_DRIVER_CLASS, true, loader);
                Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
                return true;
            } catch (Exception e) {
                // try next path
            }
        }

        return false;
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static String buildJdbcUrl(String host, String port, String dbName) {
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC";
    }

    private JPanel guestPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        model.setColumnIdentifiers(new String[]{
                "ID", "First Name", "Last Name", "Address",
                "Contact", "Email", "ID Status", "Trust Rating"
        });

        JButton loadBtn = new JButton("Load Guests");

        loadBtn.addActionListener(e -> {

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No database connection.");
                return;
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM guest")) {

                model.setRowCount(0);

                while (rs.next()) {
                    model.addRow(new Object[]{
                            getGuestId(rs),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("address"),
                            rs.getString("contact_number"),
                            getGuestEmail(rs),
                            rs.getString("valid_id_status"),
                            rs.getInt("trust_rating")
                    });
                }

                if (model.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this,
                            "No guests found in database.");
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error loading guests:\n" + ex.getMessage());
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(loadBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel transactionPanel() {

        JPanel panel = new JPanel(new GridLayout(14, 2, 5, 5));

        JTextField guestID = new JTextField();
        JComboBox<String> serviceType =
                new JComboBox<>(new String[]{"Accommodation", "Tour", "Combined"});
        JTextField propertyID = new JTextField();
        JTextField guideID = new JTextField();
        JTextField startDate = new JTextField("2025-05-01");
        JTextField endDate = new JTextField("2025-05-02");
        JTextField baseAmount = new JTextField();
        JTextField additional = new JTextField("0");
        JTextField discount = new JTextField("0");
        JTextField finalAmount = new JTextField();
        finalAmount.setEditable(false);

        JComboBox<String> paymentMethod =
                new JComboBox<>(new String[]{"Cash", "GCash", "Credit Card", "Bank Transfer"});
        JComboBox<String> paymentStatus =
                new JComboBox<>(new String[]{"Paid", "Pending", "Refunded"});

        JButton computeBtn = new JButton("Compute Final Amount");
        JButton saveBtn = new JButton("Save Transaction");

        panel.add(new JLabel("Guest ID:")); panel.add(guestID);
        panel.add(new JLabel("Service Type:")); panel.add(serviceType);
        panel.add(new JLabel("Property ID:")); panel.add(propertyID);
        panel.add(new JLabel("Guide ID:")); panel.add(guideID);
        panel.add(new JLabel("Start Date (YYYY-MM-DD):")); panel.add(startDate);
        panel.add(new JLabel("End Date:")); panel.add(endDate);
        panel.add(new JLabel("Base Amount:")); panel.add(baseAmount);
        panel.add(new JLabel("Additional Charges:")); panel.add(additional);
        panel.add(new JLabel("Discount:")); panel.add(discount);
        panel.add(new JLabel("Final Amount:")); panel.add(finalAmount);
        panel.add(new JLabel("Payment Method:")); panel.add(paymentMethod);
        panel.add(new JLabel("Payment Status:")); panel.add(paymentStatus);
        panel.add(computeBtn); panel.add(saveBtn);

        computeBtn.addActionListener(e -> {
            try {
                double base = Double.parseDouble(baseAmount.getText());
                double add = Double.parseDouble(additional.getText());
                double dis = Double.parseDouble(discount.getText());

                finalAmount.setText(String.valueOf(base + add - dis));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid amount values.");
            }
        });

        saveBtn.addActionListener(e -> {

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No database connection.");
                return;
            }

            try {

                if (guestID.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Guest ID is required.");
                    return;
                }

                String gid = guestID.getText().trim();

                String checkSQL =
                        "SELECT valid_id_status, trust_rating FROM guest WHERE guest_id=?";
                try (PreparedStatement check =
                             conn.prepareStatement(checkSQL)) {

                    check.setString(1, gid);
                    ResultSet rs = check.executeQuery();

                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this,
                                "Guest not found.");
                        return;
                    }

                    String status = rs.getString("valid_id_status");
                    int rating = rs.getInt("trust_rating");

                    if (!("Verified".equalsIgnoreCase(status) || rating >= 4)) {
                        JOptionPane.showMessageDialog(this,
                                "Guest NOT eligible.");
                        return;
                    }
                }

                // Compute automatically if empty
                if (finalAmount.getText().isEmpty()) {
                    computeBtn.doClick();
                }

                String insertSQL =
                        "INSERT INTO guest_activity_transaction " +
                        "(guest_id, service_type, reference_id, " +
                        "activity_start_date, activity_end_date, base_amount, " +
                        "additional_charges, discount_applied, final_amount, " +
                        "payment_method, payment_confirmation_status) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

                try (PreparedStatement ps =
                             conn.prepareStatement(insertSQL)) {

                    ps.setString(1, gid);
                    ps.setString(2, serviceType.getSelectedItem().toString());
                    ps.setInt(3, 1);
                    ps.setDate(4, Date.valueOf(startDate.getText()));
                    ps.setDate(5, Date.valueOf(endDate.getText()));
                    ps.setDouble(6, Double.parseDouble(baseAmount.getText()));
                    ps.setDouble(7, Double.parseDouble(additional.getText()));
                    ps.setDouble(8, Double.parseDouble(discount.getText()));
                    ps.setDouble(9, Double.parseDouble(finalAmount.getText()));
                    ps.setString(10, paymentMethod.getSelectedItem().toString());
                    ps.setString(11, paymentStatus.getSelectedItem().toString());

                    ps.executeUpdate();
                }

                JOptionPane.showMessageDialog(this,
                        "Transaction Saved Successfully!");

                // Clear form
                guestID.setText("");
                propertyID.setText("");
                guideID.setText("");
                baseAmount.setText("");
                additional.setText("0");
                discount.setText("0");
                finalAmount.setText("");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving transaction:\n" + ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel reportPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel();

        JTextField monthField = new JTextField(5);
        JTextField yearField = new JTextField(5);
        JButton generateBtn = new JButton("Generate Report");

        top.add(new JLabel("Month:"));
        top.add(monthField);
        top.add(new JLabel("Year:"));
        top.add(yearField);
        top.add(generateBtn);

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        model.setColumnIdentifiers(new String[]{
                "Guest Name", "Service Type",
                "Check In", "Check Out", "Status", "Amount"
        });

        generateBtn.addActionListener(e -> {

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No database connection.");
                return;
            }

            try {

                model.setRowCount(0);

                String sql =
                        "SELECT CONCAT(g.first_name,' ',g.last_name) as guest_name, " +
                        "gat.service_type, " +
                        "gat.activity_start_date, gat.activity_end_date, " +
                        "gat.payment_confirmation_status, gat.final_amount " +
                        "FROM guest g " +
                        "JOIN guest_activity_transaction gat ON g.guest_id=gat.guest_id " +
                        "WHERE MONTH(gat.activity_start_date)=? " +
                        "AND YEAR(gat.activity_start_date)=?";

                try (PreparedStatement ps =
                             conn.prepareStatement(sql)) {

                    ps.setInt(1, Integer.parseInt(monthField.getText()));
                    ps.setInt(2, Integer.parseInt(yearField.getText()));

                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        model.addRow(new Object[]{
                                rs.getString(1),
                                rs.getString(2),
                                rs.getDate(3),
                                rs.getDate(4),
                                rs.getString(5),
                                rs.getDouble(6)
                        });
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error generating report:\n" + ex.getMessage());
            }
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomestayBooking::new);
    }

    private static String getGuestEmail(ResultSet rs) throws Exception {
        if (hasColumn(rs, "email")) {
            return rs.getString("email");
        }
        if (hasColumn(rs, "email_address")) {
            return rs.getString("email_address");
        }
        return "";
    }

    private static String getGuestId(ResultSet rs) throws Exception {
        Object value = rs.getObject("guest_id");
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean hasColumn(ResultSet rs, String columnName) throws Exception {
        int count = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= count; i++) {
            if (columnName.equalsIgnoreCase(rs.getMetaData().getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    private static final class DriverShim implements Driver {
        private final Driver driver;

        private DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}



