import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class HomestayBookingGUI extends JFrame {

    private JTextField bookingIDField, guestIDField, propertyIDField;
    private JTextField checkInField, checkOutField, totalCostField, statusField;

    private static final double PRICE_PER_NIGHT = 1500.00; // Example fixed rate

    public HomestayBookingGUI() {

        setTitle("Homestay Booking Transaction - CCINFOM Project");
        setSize(550, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));

        bookingIDField = addField(formPanel, "Booking Name:");
        guestIDField = addField(formPanel, "Guest ID:");
        propertyIDField = addField(formPanel, "Property ID:");
        checkInField = addField(formPanel, "Check-in Date (MM-DD-YYYY):");
        checkOutField = addField(formPanel, "Check-out Date (MM-DD-YYYY):");
        totalCostField = addField(formPanel, "Total Stay Cost:");
        totalCostField.setEditable(false); // auto-computed
        statusField = addField(formPanel, "Status (Confirmed/Pending):");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 15, 15));

        JButton computeBtn = new JButton("Compute Cost");
        JButton saveBtn = new JButton("Save Booking");
        JButton viewBtn = new JButton("View Details");

        computeBtn.addActionListener(e -> computeTotalCost());
        saveBtn.addActionListener(e -> saveBooking());
        viewBtn.addActionListener(e -> viewDetails());

        buttonPanel.add(computeBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(viewBtn);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JTextField addField(JPanel panel, String label) {
        panel.add(new JLabel(label));
        JTextField field = new JTextField();
        panel.add(field);
        return field;
    }

    // Service: Property Availability & Cost Computation
    private void computeTotalCost() {
        try {
            LocalDate checkIn = LocalDate.parse(checkInField.getText());
            LocalDate checkOut = LocalDate.parse(checkOutField.getText());

            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

            if (nights <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Invalid dates selected!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            double totalCost = nights * PRICE_PER_NIGHT;
            totalCostField.setText(String.valueOf(totalCost));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date format! Use YYYY-MM-DD.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Service: Booking Finalization
    private void saveBooking() {
        if (bookingIDField.getText().isEmpty() ||
            guestIDField.getText().isEmpty() ||
            propertyIDField.getText().isEmpty() ||
            statusField.getText().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Please complete all required fields.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Booking Successfully Saved!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewDetails() {

        String info =
                "Booking ID: " + bookingIDField.getText() + "\n" +
                "Guest ID: " + guestIDField.getText() + "\n" +
                "Property ID: " + propertyIDField.getText() + "\n" +
                "Check-in Date: " + checkInField.getText() + "\n" +
                "Check-out Date: " + checkOutField.getText() + "\n" +
                "Total Stay Cost: " + totalCostField.getText() + "\n" +
                "Status: " + statusField.getText();

        JOptionPane.showMessageDialog(this, info, "Booking Details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new HomestayBookingGUI().setVisible(true));
    }
}
