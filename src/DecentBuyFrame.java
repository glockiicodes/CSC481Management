import javax.swing.*;
import javax.swing.table.*;
import com.formdev.flatlaf.FlatDarculaLaf;
import java.awt.BorderLayout;
import java.sql.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;


@SuppressWarnings("unused")
public class DecentBuyFrame extends JFrame{
    DecentBuyOrderData DBDB_OrderData = new DecentBuyOrderData();
    DatabaseConn Conn = new DatabaseConn();
    
    LocalDate currentDate = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Color primaryColor = new Color(0, 122, 204);   // Blue
    private final Color secondaryColor = new Color(98, 114, 164); // Magenta

    public DecentBuyFrame() throws SQLException {
        // Set FlatDarcula Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        Connection dbConn = Conn.getConnection();
        setTitle("DecentBuy Inventory Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        add(createTabbedPane(dbConn));
        setVisible(true);
    }

    public JTabbedPane createTabbedPane(Connection dbConn) {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Customize tabbedPane appearance
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setForeground(primaryColor);

        tabbedPane.addTab("Product Inventory", createInventoryPanel(dbConn));
        tabbedPane.addTab("Orders", createOrdersPanel(dbConn));
        //tabbedPane.addTab("Cancelled Orders", createCompletedOrdersPanel(dbConn));
        //tabbedPane.addTab("Current Orders", createCanceledOrdersPanel(dbConn));
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
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Use LinkedHashMap to maintain insertion order
        Map<String, String> columnMapping = new LinkedHashMap<>();
        columnMapping.put("idProducts", "ID");
        columnMapping.put("productName", "Name");
        columnMapping.put("productCategory", "Category");
        columnMapping.put("productBrand", "Brand");
        columnMapping.put("productPrice", "Price");
        columnMapping.put("productStock", "Stock");

        // Extract the user-friendly names for the dropdown in order
        List<String> userFriendlyNames = new ArrayList<>(columnMapping.values());
        JComboBox<String> searchDropdown = new JComboBox<>(userFriendlyNames.toArray(new String[0]));

        searchDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchDropdown.setForeground(Color.WHITE);
        searchDropdown.setBackground(Color.GRAY);

        JTextField searchTextField = new JTextField(15);
        searchTextField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton searchButton = new JButton("Search");
        styleButton(searchButton, primaryColor, secondaryColor);

        // Create instance of DecentBuyOrderData to call searchBarInventory
        DecentBuyOrderData orderData = new DecentBuyOrderData();

        searchButton.addActionListener(e -> {
            try {
                // Map user-friendly name back to database column
                String selectedName = (String) searchDropdown.getSelectedItem();
                String searchCriterion = columnMapping.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().equals(selectedName))
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Selection"));

                // Perform the search
                orderData.searchBarInventory(dbConn, table, searchCriterion, searchTextField.getText());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        searchPanel.add(searchDropdown);
        searchPanel.add(searchTextField);
        searchPanel.add(searchButton);
        return searchPanel;
    }
    public JPanel createOrderSearchPanel(Connection dbConn, JTable table) {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DecentBuyOrderData orderData = new DecentBuyOrderData();

        // Define user-friendly search options
        String[] searchOptions = {"Order ID", "Order Date", "Customer First Name", "Customer Last Name", "Order Status", "Product Name", "Current", "Pending", "Cancelled", "Supplier"};
        JComboBox<String> searchDropdown = new JComboBox<>(searchOptions);
        searchDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchDropdown.setForeground(Color.WHITE);
        searchDropdown.setBackground(Color.GRAY);

        JTextField searchTextField = new JTextField(15);
        searchTextField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton searchButton = new JButton("Search");
        styleButton(searchButton, primaryColor, secondaryColor);

        searchButton.addActionListener(e -> {
            String searchInput = searchTextField.getText();
            String searchCriterion = (String) searchDropdown.getSelectedItem();

            if (searchInput.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a search query.");
                return;
            }

            try {
                // Call the existing searchBarOrder function
                orderData.searchBarOrder(dbConn, table, searchCriterion, searchInput);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error searching for data: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        });

        searchPanel.add(searchDropdown);
        searchPanel.add(searchTextField);
        searchPanel.add(searchButton);
        return searchPanel;
    }


    public JPanel createInventoryButtonPanel(Connection dbConn, JTable table) {
        JPanel buttonPanel = new JPanel();

        // Refresh Button
        JButton inventoryRefreshButton = new JButton("Refresh Inventory");
        inventoryRefreshButton.addActionListener(e -> {
            try {
                DBDB_OrderData.loadInventoryData(dbConn, table);
                System.out.println("Inventory refreshed successfully.");
            } catch (SQLException ex) {
                System.out.println("Error refreshing inventory:");
                ex.printStackTrace();
            }
        });
        buttonPanel.add(inventoryRefreshButton);



        // Edit Button
        JButton editButton = new JButton("Edit Stock");
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Please select a row to edit.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Open edit dialog
            openEditDialog(dbConn, table, selectedRow);
        });
        buttonPanel.add(editButton);

        // Add Product Button
        JButton addButton = new JButton("Add Product");
        addButton.addActionListener(e -> openAddProductDialog(dbConn, table));
        buttonPanel.add(addButton);

         // **New: Print Stock Report Button**
        JButton printReportButton = new JButton("Print Stock Report");
        styleButton(printReportButton, secondaryColor, secondaryColor);
        printReportButton.addActionListener(e -> {
            try {
                boolean complete = table.print(JTable.PrintMode.FIT_WIDTH, 
                    new MessageFormat("DecentBuy Inventory - Stock Report\nDate: " + LocalDate.now()), 
                    new MessageFormat("Page - {0}"));

                if (complete) {
                    JOptionPane.showMessageDialog(this, "Printing Complete", "Print", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Printing Cancelled", "Print", JOptionPane.WARNING_MESSAGE);
                }
            } catch (PrinterException pe) {
                pe.printStackTrace();
                JOptionPane.showMessageDialog(this, "Printing Failed: " + pe.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(printReportButton);

        

            

            return buttonPanel;
    }

    private void openAddProductDialog(Connection dbConn, JTable table) {
        JDialog dialog = new JDialog(this, "Add New Product", true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input Fields
        JTextField nameField = new JTextField(20);
        JTextField categoryField = new JTextField(20);
        JTextField brandField = new JTextField(20);
        JTextField priceField = new JTextField(20);
        JTextField stockField = new JTextField(20);

        panel.add(new JLabel("Product Name:"));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JLabel("Stock:"));
        panel.add(stockField);
        panel.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        styleButton(saveButton, primaryColor, secondaryColor);
        styleButton(cancelButton, primaryColor, secondaryColor);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel);
        dialog.add(panel);

        // Action Listeners
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            String brand = brandField.getText().trim();
            String priceText = priceField.getText().trim();
            String stockText = stockField.getText().trim();

            // Validation
            if (name.isEmpty() || category.isEmpty() || brand.isEmpty() || priceText.isEmpty() || stockText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields must be filled!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double price;
            int stock;

            try {
                price = Double.parseDouble(priceText);
                stock = Integer.parseInt(stockText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Price must be a number and Stock must be an integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Call method to add product
                DBDB_OrderData.addProduct(dbConn, name, category, brand, price, stock);

                // Refresh table
                DBDB_OrderData.loadInventoryData(dbConn, table);
                JOptionPane.showMessageDialog(dialog, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Failed to add product. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void openEditDialog(Connection dbConn, JTable table, int selectedRow) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Edit Product");
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);

        // Get current data from the table
        Object id = table.getValueAt(selectedRow, 0);
        String currentName = table.getValueAt(selectedRow, 1).toString();
        String currentCategory = table.getValueAt(selectedRow, 2).toString();
        String currentBrand = table.getValueAt(selectedRow, 3).toString();
        String currentPrice = table.getValueAt(selectedRow, 4).toString().replace("$", "");
        int currentStock = Integer.parseInt(table.getValueAt(selectedRow, 5).toString());

        JPanel panel = new JPanel();
        // Create input fields pre-filled with current data
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JTextField nameField = new JTextField(currentName, 20);
        JTextField categoryField = new JTextField(currentCategory, 20);
        JTextField brandField = new JTextField(currentBrand, 20);
        JTextField priceField = new JTextField(currentPrice, 20);
        JTextField stockField = new JTextField(String.valueOf(currentStock), 20);

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Stock:"));
        panel.add(stockField);

        // Buttons
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String updatedName = nameField.getText();
            String updatedCategory = categoryField.getText();
            String updatedBrand = brandField.getText();
            String updatedPrice = priceField.getText();
            String updatedStock = stockField.getText();

            // Validate inputs
            if (updatedName.isEmpty() || updatedCategory.isEmpty() || updatedBrand.isEmpty() ||
                    updatedPrice.isEmpty() || updatedStock.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields must be filled!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double price = Double.parseDouble(updatedPrice);
                int stock = Integer.parseInt(updatedStock);

                // Call method in Order Data class
                DBDB_OrderData.updateProductInDatabase(dbConn, id, updatedName, updatedCategory, updatedBrand, price, stock);

                // Refresh table
                DBDB_OrderData.loadInventoryData(dbConn, table);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Price must be a number and Stock must be an integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Failed to update product. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // Add components to dialog
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }


    private void updateInventoryQuantities(Connection dbConn) throws SQLException {
        String updateInventorySQL =
                "UPDATE Products AS inventory " +
                "JOIN " +
                    "(SELECT ProductsName, SUM(ProductsStock) AS total_ordered " +
                    " FROM PendingOrders WHERE processed = FALSE" +
                    " GROUP BY ProductsName ) As o" +
                    " ON i.ProductsName = o.ProductsName " +
                    " SET i.ProductsStock = i.ProductsStock - o.total_ordered " +
                    " WHERE i.ProductsStock >= o.total_ordered";

        try (PreparedStatement pstmt = dbConn.prepareStatement(updateInventorySQL)) {
            int rowsUpdated = pstmt.executeUpdate();
            System.out.println(rowsUpdated + " inventory items updated based on pending orders.");
        }

        String markedProcessedSQL =
            "UPDATE DBOrder SET processed = TRUE WHERE processed = FALSE";
        try (PreparedStatement pstmt = dbConn.prepareStatement(markedProcessedSQL)) {
            pstmt.executeUpdate();
        }
    }

    public JPanel createOrdersPanel(Connection dbConn) {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        JTable ordersTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);

        // Add the search panel at the top
        JPanel searchPanel = createOrderSearchPanel(dbConn, ordersTable);
        ordersPanel.add(searchPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        JButton refreshOrdersButton = new JButton("Refresh Orders");

        try {
            DBDB_OrderData.loadOrdersData(dbConn, ordersTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        refreshOrdersButton.addActionListener(e -> {
            try {
                DBDB_OrderData.loadOrdersData(dbConn, ordersTable);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        buttonPanel.add(refreshOrdersButton);

        JButton addOrderButton = new JButton("Add Order");
        addOrderButton.addActionListener(e -> {
            try {
                new AddOrder(dbConn, ordersTable);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        });
        buttonPanel.add(addOrderButton);

        JButton editOrderButton = new JButton("Edit Order");
        editOrderButton.addActionListener(e -> {
            int selectedRow = ordersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Please select an order to edit.");
                return;
            }
            // Call the function to open the Edit Order dialog
            openEditOrderDialog(dbConn, selectedRow, ordersTable);
        });
        buttonPanel.add(editOrderButton);

        ordersPanel.add(buttonPanel, BorderLayout.SOUTH);
        return ordersPanel;
    }


    public void openAddOrderDialog(Connection dbConn, JTable ordersTable) {
    // Create a dialog window
    JDialog dialog = new JDialog();
    dialog.setTitle("Add New Order");
    dialog.setSize(400, 400);
    dialog.setLocationRelativeTo(null);
    dialog.setModal(true);

    // Create input fields
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    JTextField OrderID = new JTextField(20);
    JTextField DBO_Quantity = new JTextField(20);
    JTextField DBO_Type = new JTextField(20);
    JTextField DBO_Date = new JTextField(20);

    JComboBox<String> itemComboBox = new JComboBox<>(); // Populates products
    try {
        Statement stmt = dbConn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT productName FROM Products");
        while (rs.next()) {
            itemComboBox.addItem(rs.getString("productName"));
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
    JComboBox<String> customerComboBox = new JComboBox<>(); // Populates products
    try {
        Statement stmt = dbConn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT concat(customerFirst, ' ', customerLast) As Name FROM Customer");
        while (rs.next()) {
            customerComboBox.addItem(rs.getString("Name"));
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
    JComboBox<String> supplierComboBox = new JComboBox<>(); // Populates products
    try {
        Statement stmt = dbConn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SupplierName FROM Suppliers");
        while (rs.next()) {
            supplierComboBox.addItem(rs.getString("productName"));
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }

    // Add fields to the panel

    
    panel.add(new JLabel("Order ID:"));
    panel.add(OrderID);
    panel.add(new JLabel("Date (YYYY-MM-DD):"));
    panel.add(DBO_Date);
    panel.add(new JLabel("Quantity:"));
    panel.add(DBO_Quantity);
    panel.add(new JLabel("Type (Customer or Supplier):"));
    panel.add(DBO_Type);
    panel.add(new JLabel("Product:"));
    panel.add(itemComboBox);
    panel.add(new JLabel("Customer Name:"));
    panel.add(customerComboBox);
    panel.add(new JLabel("Supplier Name:"));
    panel.add(supplierComboBox);

    // Buttons
    JButton submitButton = new JButton("Submit");
    JButton cancelButton = new JButton("Cancel");

    // Submit button action
    submitButton.addActionListener(e -> {
        String id = OrderID.getText();
        String formattedDate = DBO_Date.getText();
        String quantityText = DBO_Quantity.getText();
        String type = DBO_Type.getText();
        String productName = (String) itemComboBox.getSelectedItem();
        String CustomerName = (String) customerComboBox.getSelectedItem();
        String SupplierName = (String) supplierComboBox.getSelectedItem();

        if (id.isEmpty() || formattedDate.isEmpty() || quantityText.isEmpty() || type.isEmpty() || productName == null) {
            JOptionPane.showMessageDialog(dialog, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int orderId = Integer.parseInt(id);
            int quantity = Integer.parseInt(quantityText);
            double price = 0.0;

            // Fetch product price
            String selectPriceSQL = "SELECT ProductsPrice FROM Products WHERE productName = ?";
            try (PreparedStatement pstmt = dbConn.prepareStatement(selectPriceSQL)) {
                pstmt.setString(1, productName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    price = rs.getDouble("ProductsPrice");
                } else {
                    JOptionPane.showMessageDialog(dialog, "Product not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Calculate total cost
            double totalCost = price * quantity;

            // Insert the new order
           // addOrderToDatabase(dbConn, orderId, formattedDate, quantity, totalCost, "Pending", type);

            // Refresh the orders table
            DBDB_OrderData.loadOrdersData(dbConn, ordersTable);
            dialog.dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dialog, "Order ID and Quantity must be numbers!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Failed to add order. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Cancel button action
    cancelButton.addActionListener(e -> dialog.dispose());

    // Add buttons to a panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(submitButton);
    buttonPanel.add(cancelButton);

    // Add components to the dialog
    dialog.add(panel, BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
}

    // successfully adds orders, but new orders are not being added to the database, josh look into this 
    private void addOrderToDatabase(Connection dbConn, int id, String date, int quantity, double totalCost, 
                                 String supplierName, String productName, String customerFirst, String customerLast, 
                                 String status, String type) throws SQLException {
    // SQL query to insert a new order with subqueries for foreign keys
    String insertSQL = "INSERT INTO decentbuy3.DBOrder " + 
                       "(idDBOrder, DBOrderDate, DBOrderQuantity, DBOrderTotalCost, " +
                       "Supplier_idSupplier, Products_idProducts, Customer_idCustomer, " +
                       "DBOrderStatus, DBOrderType) " +
                       "VALUES (?, ?, ?, ?, " +
                       "(SELECT idSupplier FROM decentbuy3.Supplier WHERE SupplierName = ? LIMIT 1), " +
                       "(SELECT idProducts FROM decentbuy3.Products WHERE productName = ? LIMIT 1), " +
                       "(SELECT idCustomer FROM decentbuy3.Customer WHERE customerFirst = ? AND customerLast = ? LIMIT 1), " +
                       "?, ?)";

    try (PreparedStatement pstmt = dbConn.prepareStatement(insertSQL)) {
        // Set placeholders for the main table values
        pstmt.setInt(1, id);
        pstmt.setString(2, date);
        pstmt.setInt(3, quantity);
        pstmt.setDouble(4, totalCost);

        // Set placeholders for the subqueries
        pstmt.setString(5, supplierName); // SupplierName for Supplier_idSupplier
        pstmt.setString(6, productName); // productName for Products_idProducts
        pstmt.setString(7, customerFirst); // customerFirst for Customer_idCustomer
        pstmt.setString(8, customerLast); // customerLast for Customer_idCustomer

        // Set placeholders for order status and type
        pstmt.setString(9, status);
        pstmt.setString(10, type);

        // Execute the query
        int rowsInserted = pstmt.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println("Order added successfully.");
        } else {
            System.out.println("Failed to add the order. Check foreign key references.");
        }
    }
}

    private void openEditOrderDialog(Connection dbConn, int selectedRow, JTable ordersTable)
    {

        // Get the selected order data
        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        String fullName = (String) ordersTable.getValueAt(selectedRow, 1);  // Full name from table
        String[] nameParts = fullName.split(" ");  // Split the full name into first and last name
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        String orderDate = (String) ordersTable.getValueAt(selectedRow, 2);
        int quantity = (int) ordersTable.getValueAt(selectedRow, 3);
        String productName = (String) ordersTable.getValueAt(selectedRow, 4);
        double productPrice = (double) ordersTable.getValueAt(selectedRow, 5);
        double totalCost = (double) ordersTable.getValueAt(selectedRow, 6);
        String orderStatus = (String) ordersTable.getValueAt(selectedRow, 7);
        DecentBuyOrderData decentBuyOrderData = new DecentBuyOrderData();

        // Create the Edit Order dialog
        JDialog editDialog = new JDialog();
        editDialog.setTitle("Edit Order");
        editDialog.setSize(400, 300);
        editDialog.setLocationRelativeTo(null);

        // Create textfields for editing
        JTextField firstNameField = new JTextField(firstName);
        JTextField lastNameField = new JTextField(lastName);
        JTextField orderDateField = new JTextField(orderDate);
        JTextField quantityField = new JTextField(String.valueOf(quantity));
        JTextField productNameField = new JTextField(productName);
        JTextField productPriceField = new JTextField(String.valueOf(productPrice));
        JTextField totalCostField = new JTextField(String.valueOf(totalCost));
        JTextField orderStatusField = new JTextField(orderStatus);

        // Create buttons
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.setBounds(150, 250, 95, 30);
        cancelButton.setBounds(255, 250, 95, 30);

        editDialog.setLayout(null);  // No layout manager, absolute positioning

        // Create labels
        JLabel firstNameLabel = new JLabel("First Name:");
        JLabel lastNameLabel = new JLabel("Last Name:");
        JLabel orderDateLabel = new JLabel("Order Date:");
        JLabel quantityLabel = new JLabel("Quantity:");
        JLabel productNameLabel = new JLabel("Product Name:");
        JLabel productPriceLabel = new JLabel("Product Price:");
        JLabel totalCostLabel = new JLabel("Total Cost:");
        JLabel orderStatusLabel = new JLabel("Order Status:");

        // Position labels and textfields
        firstNameLabel.setBounds(30, 10, 100, 25);
        lastNameLabel.setBounds(30, 40, 100, 25);
        orderDateLabel.setBounds(30, 70, 100, 25);
        quantityLabel.setBounds(30, 100, 100, 25);
        productNameLabel.setBounds(30, 130, 100, 25);
        productPriceLabel.setBounds(30, 160, 100, 25);
        totalCostLabel.setBounds(30, 190, 100, 25);
        orderStatusLabel.setBounds(30, 220, 100, 25);

        firstNameField.setBounds(150, 10, 200, 25);
        lastNameField.setBounds(150, 40, 200, 25);
        orderDateField.setBounds(150, 70, 200, 25);
        quantityField.setBounds(150, 100, 200, 25);
        productNameField.setBounds(150, 130, 200, 25);
        productPriceField.setBounds(150, 160, 200, 25);
        totalCostField.setBounds(150, 190, 200, 25);
        orderStatusField.setBounds(150, 220, 200, 25);

        saveButton.setBounds(150, 250, 95, 30);
        cancelButton.setBounds(255, 250, 95, 30);

        // Add labels next to the textfield
        editDialog.add(firstNameLabel);
        editDialog.add(lastNameLabel);
        editDialog.add(orderDateLabel);
        editDialog.add(quantityLabel);
        editDialog.add(productNameLabel);
        editDialog.add(productPriceLabel);
        editDialog.add(totalCostLabel);
        editDialog.add(orderStatusLabel);

        // Add textfields
        editDialog.add(firstNameField);
        editDialog.add(lastNameField);
        editDialog.add(orderDateField);
        editDialog.add(quantityField);
        editDialog.add(productNameField);
        editDialog.add(productPriceField);
        editDialog.add(totalCostField);
        editDialog.add(orderStatusField);
        editDialog.add(saveButton);
        editDialog.add(cancelButton);

        // Create Save button action
        saveButton.addActionListener(saveEvent -> {
            try {
                decentBuyOrderData.saveEditedOrder(dbConn, orderId, firstNameField.getText(), lastNameField.getText(),
                        orderDateField.getText(), Integer.parseInt(quantityField.getText()),
                        productNameField.getText(), Double.parseDouble(productPriceField.getText()),
                        Double.parseDouble(totalCostField.getText()), orderStatusField.getText());
                editDialog.dispose();
                decentBuyOrderData.loadOrdersData(dbConn, ordersTable); // Refresh the table after saving
                JOptionPane.showMessageDialog(null, "Order updated successfully.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error saving order: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Cancel button setup
        cancelButton.addActionListener(cancelEvent -> editDialog.dispose());

        // Show the dialog
        editDialog.setVisible(true);
    }

    private JPanel createFieldPanel(String label, JTextField textField) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(label));  // Add the label
        panel.add(textField);          // Add the text field
        return panel;
    }

    public static void main(String[] args) {
        try {
            new DecentBuyFrame();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void styleButton(JButton button, Color bgColor, Color hoverColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createLineBorder(bgColor));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createLineBorder(hoverColor));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createLineBorder(bgColor));
            }
        });
    }
}
