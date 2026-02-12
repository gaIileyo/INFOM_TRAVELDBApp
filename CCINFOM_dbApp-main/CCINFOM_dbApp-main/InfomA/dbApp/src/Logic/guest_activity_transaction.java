package Logic;

import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class guest_activity_transaction {

    private DefaultTableModel model = new DefaultTableModel(
        new Object[]{
            "Transaction ID",
            "Guest ID",
            "Service Type",
            "Reference ID",
            "Start Date",
            "End Date",
            "Base Amount",
            "Additional Charges",
            "Discount",
            "Final Amount",
            "Payment Method",
            "Payment Status"
        }, 0
    );

    public void viewAllTransactions(Connection conn) throws SQLException {

        String query = "SELECT * FROM guest_activity_transaction";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        model.setRowCount(0);

        while (rs.next()) {

            model.addRow(new Object[]{
                rs.getInt("activity_transaction_id"),
                rs.getInt("guest_id"),
                rs.getString("service_type"),
                rs.getInt("reference_id"),
                rs.getDate("activity_start_date"),
                rs.getDate("activity_end_date"),
                rs.getDouble("base_amount"),
                rs.getDouble("additional_charges"),
                rs.getDouble("discount_applied"),
                rs.getDouble("final_amount"),
                rs.getString("payment_method"),
                rs.getString("payment_confirmation_status")
            });
        }
    }

    public DefaultTableModel getModel() {
        return model;
    }
}
