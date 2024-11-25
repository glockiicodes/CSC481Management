import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
//import javax.swing.SwingUtilities;
//import javax.swing.table.DefaultTableModel;
//import java.sql.PreparedStatement;

import java.awt.BorderLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.Statement;

public class DecentBuyFrame {
    DecentBuyOrderData DBDB_OrderData = new DecentBuyOrderData();
    
    public void dbFrame(Connection dbConn) {
        JFrame frame = new JFrame("DecentBuy Inventory Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(createTabbedPane(dbConn));
        frame.setVisible(true);
    }

    public JTabbedPane createTabbedPane(Connection dbConn) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Inventory", createInventoryPanel(dbConn));
        tabbedPane.addTab("Pending Orders", createOrdersPanel(dbConn));
        tabbedPane.addTab("Canceled Orders", createCanceledOrdersPanel(dbConn));
        tabbedPane.addTab("Completed Orders", createCompletedOrdersPanel(dbConn));
        return tabbedPane;
    }

    public JPanel createInventoryPanel(Connection dbConn) {
        JPanel inventoryPanel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        inventoryPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel searchPanel = createSearchPanel(dbConn, table);
        inventoryPanel.add(searchPanel, BorderLayout.NORTH);

        JPanel buttonPanel = createInventoryButtonPanel(dbConn, table);
        inventoryPanel.add(buttonPanel, BorderLayout.SOUTH);

        try {
            DBDB_OrderData.loadInventoryData(dbConn, table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inventoryPanel;
    }

    public JPanel createSearchPanel(Connection dbConn, JTable table) {
        JPanel searchPanel = new JPanel();
        String[] searchOptions = {"Item Name", "Category"};
        JComboBox<String> searchDropdown = new JComboBox<>(searchOptions);
        JTextField searchTextField = new JTextField(15);
        JButton searchButton = new JButton("Search");

        searchButton.addActionListener(e -> {
            try {
                DBDB_OrderData.searchBarInventory(dbConn, table, searchDropdown.getSelectedItem().toString(), searchTextField.getText());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        searchPanel.add(searchDropdown);
        searchPanel.add(searchTextField);
        searchPanel.add(searchButton);
        return searchPanel;
    }

    public JPanel createInventoryButtonPanel(Connection dbConn, JTable table) {
        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("Refresh Inventory");
        refreshButton.addActionListener(e -> {
            try {
                updateInventoryQuantities(dbConn);
                DBDB_OrderData.loadInventoryData(dbConn, table);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        buttonPanel.add(refreshButton);
        return buttonPanel;
    }

    private void updateInventoryQuantities(Connection dbConn) throws SQLException {
        String updateInventorySQL = 
            "UPDATE Inventory AS i " +
            "JOIN ( " +
            "    SELECT item_name, SUM(quantity) AS total_ordered " +
            "    FROM PendingOrders " +
            "    WHERE processed = FALSE " +
            "    GROUP BY item_name " +
            ") AS o " +
            "ON i.item_name = o.item_name " +
            "SET i.quantity = i.quantity - o.total_ordered " +
            "WHERE i.quantity >= o.total_ordered";
    
        try (PreparedStatement pstmt = dbConn.prepareStatement(updateInventorySQL)) {
            int rowsUpdated = pstmt.executeUpdate();
            System.out.println(rowsUpdated + " inventory items updated based on pending orders.");
        }
    
        String markedProcessedSQL = 
            "UPDATE PendingOrders SET processed = TRUE WHERE processed = FALSE";
        try (PreparedStatement pstmt = dbConn.prepareStatement(markedProcessedSQL)) {
            pstmt.executeUpdate();
        }
    }

    public JPanel createOrdersPanel(Connection dbConn) {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        JTable ordersTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton refreshOrdersButton = new JButton("Refresh Orders");
        refreshOrdersButton.addActionListener(e -> {
            try {
                DBDB_OrderData.loadPendingOrdersData(dbConn, ordersTable);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        buttonPanel.add(refreshOrdersButton);

        JButton addOrderButton = new JButton("Add Order");
        addOrderButton.addActionListener(e -> openAddOrderDialog(dbConn, ordersTable)); {
        buttonPanel.add(addOrderButton);

        ordersPanel.add(buttonPanel, BorderLayout.SOUTH);
        return ordersPanel;

        }
    }

    private void openAddOrderDialog(Connection dbConn, JTable ordersTable) {
        // Create a dialog window
        JDialog dialog = new JDialog();
        dialog.setTitle("Add New Order");
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);

        // Create input fields
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField itemNameField = new JTextField(20);
        JTextField quantityField = new JTextField(20);

        panel.add(new JLabel("First Name:"));
        panel.add(firstNameField);
        panel.add(new JLabel("Last Name:"));
        panel.add(lastNameField);
        panel.add(new JLabel("Item Name:"));
        panel.add(itemNameField);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantityField);

        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");

        // Add action listener to Submit button
        submitButton.addActionListener(e -> {
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();
            String itemName = itemNameField.getText();
            String quantityText = quantityField.getText();

            if (firstName.isEmpty() || lastName.isEmpty() || itemName.isEmpty() || quantityText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityText);
                addOrderToDatabase(dbConn, firstName, lastName, itemName, quantity);
                DBDB_OrderData.loadPendingOrdersData(dbConn, ordersTable); // Refresh the table
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Quantity must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Failed to add order. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addOrderToDatabase(Connection dbConn, String firstName, String lastName, String itemName, int quantity) throws SQLException {
        String selectSQL = "SELECT price, quantity FROM Inventory WHERE item_name = ?";
        double price = 0.0;
        int availableQuantity = 0;

        try (PreparedStatement selectStmt = dbConn.prepareStatement(selectSQL)) {
            selectStmt.setString(1, itemName);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                price = rs.getDouble("price");
                availableQuantity = rs.getInt("quantity");

                if (availableQuantity < quantity) {
                    throw new SQLException("Insufficient inventory for item: " + itemName);
                }
            } else {
                throw new SQLException("Item not found in inventory: " + itemName);
            }
        }

        double totalPrice = price * quantity;
        String insertOrderSQL = "INSERT INTO PendingOrders (first_name, last_name, item_name, quantity, price, status, processed) VALUES (?, ?, ?, ?, ?, 'Pending', FALSE)";
        try (PreparedStatement insertStmt = dbConn.prepareStatement(insertOrderSQL)) {
            insertStmt.setString(1, firstName);
            insertStmt.setString(2, lastName);
            insertStmt.setString(3, itemName);
            insertStmt.setInt(4, quantity);
            insertStmt.setDouble(5, totalPrice);
            insertStmt.executeUpdate();
            System.out.println("Order added successfully.");
        }
    }

    public JPanel createCanceledOrdersPanel(Connection dbConn) {
        JPanel canceledPanel = new JPanel(new BorderLayout());
        JTable canceledTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(canceledTable);
        canceledPanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshCanceledButton = new JButton("Refresh Canceled Orders");
        refreshCanceledButton.addActionListener(e -> {
            try {
                DBDB_OrderData.loadCanceledOrdersData(dbConn, canceledTable);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshCanceledButton);
        canceledPanel.add(buttonPanel, BorderLayout.SOUTH);

        return canceledPanel;
    }

    public JPanel createCompletedOrdersPanel(Connection dbConn) {
        JPanel completedPanel = new JPanel(new BorderLayout());
        JTable completedTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(completedTable);
        completedPanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshCompletedButton = new JButton("Refresh Completed Orders");
        refreshCompletedButton.addActionListener(e -> {
            try {
                DBDB_OrderData.loadCompletedOrdersData(dbConn, completedTable);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshCompletedButton);
        completedPanel.add(buttonPanel, BorderLayout.SOUTH);

        return completedPanel;
    }
}
