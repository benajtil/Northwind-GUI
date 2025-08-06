import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JList;

/**
 *
 * @author benza
 */
public class northwindGUI extends javax.swing.JFrame {
    DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    ResultSet rs;
    PreparedStatement pst;
    Connection conn;
    private DefaultListModel<String> searchListModel;
    private double discountedValue;
    private static String driver = "com.mysql.jdbc.Driver";
    private static String domain = "jdbc:mysql://localhost/northwind";
    private static String user = "root";
    private static String pass= "Til091002";

    
    /**
     * Creates new form gui_test
     */
    public northwindGUI() {
        initComponents();
        connect();
        loadContactNames();
        searchListModel = new DefaultListModel<>();
        searchList.setModel(searchListModel);
        
        // Initialize productTable
        comboBoxContactNames.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        JComboBox<String> comboBox = (JComboBox<String>) e.getSource();
        String selectedContactName = (String) comboBox.getSelectedItem();
        displayCustomerDetails(selectedContactName);
    }
});
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String keyword = searchTextField.getText().trim();
                if (!keyword.isEmpty()) {
                    searchProducts(keyword);
                } else {
                    JOptionPane.showMessageDialog(null, "Please enter a search keyword.");
                }
            }
        });
        searchList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JList<String> list = (JList<String>) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        String selectedProduct = searchListModel.getElementAt(index);
                        populateProductTable(selectedProduct);
                    }
                }
            }
        });
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteProduct();
            }
        });
        
        paymentBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calculatePayment();
            }
        });
        
    
    }
    
 private void connect(){
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(domain, user, pass);
            JOptionPane.showMessageDialog(null, "Connection Successful");

        } catch(Exception e){
            JOptionPane.showMessageDialog(null, "Connection error");
        }
    }
 

 private void loadContactNames() {
    try {
        String query = "SELECT contact_name FROM customers ORDER BY contact_name ASC";
        pst = conn.prepareStatement(query);
        rs = pst.executeQuery();

        int rowCount = 0; // Track the number of rows retrieved
        while (rs.next()) {
            String contactName = rs.getString("contact_name");
            comboBoxContactNames.addItem(contactName);
            rowCount++; // Increment the row count
        }
        System.out.println("Number of contact names retrieved: " + rowCount); // Debug statement
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error loading contact names: " + e.getMessage());
    }
}


 private void displayCustomerDetails(String selectedContactName) {
    try {
        String query = "SELECT customer_id, company_name, country FROM customers WHERE contact_name = ?";
        pst = conn.prepareStatement(query);
        pst.setString(1, selectedContactName);
        rs = pst.executeQuery();

        if (rs.next()) {
            String customerId = rs.getString("customer_id");
            String companyName = rs.getString("company_name");
            String country = rs.getString("country");
            
            // Display the customer details (e.g., set text of JLabels)
            custIDTextField.setText(customerId);
            companyNameTextField.setText(companyName);
            countryTextField.setText(country);
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error retrieving customer details: " + e.getMessage());
    }
}
 private void searchProducts(String keyword) {
    try {
        // Query to search for matches in both product_id and product_name
        String query = "SELECT product_id, product_name FROM products WHERE product_name LIKE ? OR product_id = ?";
        pst = conn.prepareStatement(query);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, keyword);
        rs = pst.executeQuery();

        // Clear previous search results
        searchListModel.clear();

        boolean hasResults = false; // Track if any results found
        while (rs.next()) {
            int productId = rs.getInt("product_id");
            String productName = rs.getString("product_name");
            searchListModel.addElement(productId + " - " + productName);
            hasResults = true; // Set flag to indicate results found
        }

        if (!hasResults) {
            JOptionPane.showMessageDialog(null, "No products found matching the search criteria.");
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error searching products: " + e.getMessage());
    }
}

private void populateProductTable(String selectedProduct) {
        // Split the selected item to extract product ID and name
        String[] parts = selectedProduct.split(" - ");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid selected product.");
            return;
        }

        // Extract product ID and name
        int productId = Integer.parseInt(parts[0].trim());
        String productName = parts[1].trim();

        try {
            String query = "SELECT * FROM products WHERE product_id = ?";
            pst = conn.prepareStatement(query);
            pst.setInt(1, productId);
            rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) productTable.getModel();

            while (rs.next()) {
                int quantity = rs.getInt("units_in_stock");
                double price = rs.getDouble("unit_price");
                double amount = quantity * price;
                double discount = 0.0; // Default discount
                double discountedValue = amount - (amount * discount);

                // Add row to the table
                model.addRow(new Object[]{productId, productName, quantity, price, amount, discount, discountedValue});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving product details: " + e.getMessage());
        }
    }

    private void deleteProduct() {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        int selectedIndex = productTable.getSelectedRow();
        if (selectedIndex != -1) {
            model.removeRow(selectedIndex);
        } else {
            JOptionPane.showMessageDialog(null, "Please select a product to delete.");
        }
    }
private void calculatePayment() {
    DefaultTableModel model = (DefaultTableModel) productTable.getModel();
    double subtotal = 0;

    // Calculate subtotal
    for (int i = 0; i < model.getRowCount(); i++) {
        double amount = (double) model.getValueAt(i, 4);
        subtotal += amount;
    }

    // Apply additional discount if provided
    double additionalDiscount = 0;
    if (!discountTxtField.getText().isEmpty()) {
        try {
            additionalDiscount = Double.parseDouble(discountTxtField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid discount percentage.");
            return;
        }
    }

    // Calculate total
    double total = subtotal - (subtotal * (additionalDiscount / 100)); // Subtract additional discount from subtotal

    // Format values to display only two decimal places
    DecimalFormat df = new DecimalFormat("#.##");
    String formattedSubtotal = df.format(subtotal);
    String formattedTotalDiscount = df.format(subtotal - total);
    String formattedTotal = df.format(total);

    // Update JLabels with calculated values
    subtotalJlabel.setText(formattedSubtotal);
    discountJlabel.setText(formattedTotalDiscount);
    totalJlabel.setText(formattedTotal);
}

private void updateDiscountedValues() {
    DefaultTableModel model = (DefaultTableModel) productTable.getModel();

    for (int i = 0; i < model.getRowCount(); i++) {
        double quantity = (double) model.getValueAt(i, 2);
        double price = (double) model.getValueAt(i, 3);
        double discount = Double.parseDouble(discountTxtField.getText()) / 100; // Convert percentage to decimal

        double amount = quantity * price;
        double discountedValue = amount - (amount * discount);

        // Update discounted value in the table
        model.setValueAt(discountedValue, i, 6);
    }
}





 
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jToggleButton1 = new javax.swing.JToggleButton();
        jToggleButton2 = new javax.swing.JToggleButton();
        searchTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        productTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        totalJlabel = new javax.swing.JLabel();
        discountJlabel = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        subtotalJlabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        comboBoxContactNames = new javax.swing.JComboBox<>();
        custIDTextField = new javax.swing.JTextField();
        companyNameTextField = new javax.swing.JTextField();
        countryTextField = new javax.swing.JTextField();
        searchButton = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        deleteBtn = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        paymentBtn = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        searchList = new javax.swing.JList<>();
        discountTxtField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jToggleButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jToggleButton1.setText("123");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        jToggleButton2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jToggleButton2.setText("ABC");
        jToggleButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton2ActionPerformed(evt);
            }
        });

        searchTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTextFieldActionPerformed(evt);
            }
        });

        productTable.setAutoCreateRowSorter(true);
        productTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product ID", "Product Name", "Quantity", "Price", "Amount", "% Discount", "Discounted Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        productTable.setShowHorizontalLines(true);
        productTable.setShowVerticalLines(true);
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                productTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(productTable);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel1.setText("Discount:");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Total:");

        totalJlabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        totalJlabel.setText("0.00");

        discountJlabel.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        discountJlabel.setText("0.00");

        jLabel5.setText("___________________");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel6.setText("Subtotal:");

        subtotalJlabel.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        subtotalJlabel.setText("0.00");

        jLabel8.setText("Contact Name:");

        jLabel11.setText("Customer ID:");

        jLabel10.setText("Company Name:");

        jLabel9.setText("Country:");

        comboBoxContactNames.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {}));
        comboBoxContactNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxContactNamesActionPerformed(evt);
            }
        });

        custIDTextField.setEditable(false);
        custIDTextField.setBackground(new java.awt.Color(204, 204, 204));

        companyNameTextField.setEditable(false);
        companyNameTextField.setBackground(new java.awt.Color(204, 204, 204));

        countryTextField.setEditable(false);
        countryTextField.setBackground(new java.awt.Color(204, 204, 204));

        searchButton.setText("Search");

        jButton4.setBackground(new java.awt.Color(255, 51, 51));
        jButton4.setText("LOCK");

        deleteBtn.setText("DELETE");

        jButton1.setText("NEW SALE");

        jButton6.setBackground(new java.awt.Color(255, 255, 102));
        jButton6.setText("VOID ORDER");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        paymentBtn.setBackground(new java.awt.Color(102, 255, 102));
        paymentBtn.setText("PAYMENT");
        paymentBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentBtnActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jButton2.setText("...");

        searchList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(searchList);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("%Discount:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 763, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(27, 27, 27)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel10)
                                            .addComponent(jLabel11)
                                            .addComponent(jLabel9))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(companyNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(countryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(custIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(comboBoxContactNames, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(284, 284, 284)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel2))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(totalJlabel)
                                    .addComponent(jLabel5)
                                    .addComponent(discountJlabel)
                                    .addComponent(subtotalJlabel))
                                .addGap(45, 45, 45)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(deleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jButton2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(paymentBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton6)))
                        .addGap(37, 37, 37))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jToggleButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(searchButton)
                        .addGap(42, 42, 42)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(discountTxtField, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jToggleButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(searchButton)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(discountTxtField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3))))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6)
                                    .addComponent(subtotalJlabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(discountJlabel)
                                    .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(totalJlabel)
                                    .addComponent(jLabel2))
                                .addGap(8, 8, 8))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(comboBoxContactNames, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel8))
                                .addGap(11, 11, 11)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel11)
                                    .addComponent(custIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel10)
                                    .addComponent(companyNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel9)
                                    .addComponent(countryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(73, 73, 73)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(deleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(paymentBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(869, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(246, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 12, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton6ActionPerformed

    private void comboBoxContactNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxContactNamesActionPerformed

    }//GEN-LAST:event_comboBoxContactNamesActionPerformed

    private void productTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_productTableMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_productTableMouseClicked

    private void searchTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTextFieldActionPerformed

    }//GEN-LAST:event_searchTextFieldActionPerformed

    private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void paymentBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_paymentBtnActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(northwindGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(northwindGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(northwindGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(northwindGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new northwindGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> comboBoxContactNames;
    private javax.swing.JTextField companyNameTextField;
    private javax.swing.JTextField countryTextField;
    private javax.swing.JTextField custIDTextField;
    private javax.swing.JButton deleteBtn;
    private javax.swing.JLabel discountJlabel;
    private javax.swing.JTextField discountTxtField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JButton paymentBtn;
    private javax.swing.JTable productTable;
    private javax.swing.JButton searchButton;
    private javax.swing.JList<String> searchList;
    private javax.swing.JTextField searchTextField;
    private javax.swing.JLabel subtotalJlabel;
    private javax.swing.JLabel totalJlabel;
    // End of variables declaration//GEN-END:variables
}
