package com.banking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankManagement {

    private final DBConnection dbConn;
    private final TransactionLogger tranLogger;

    public BankManagement(DBConnection dbConn, TransactionLogger tranLogger) {
        this.dbConn = dbConn;
        this.tranLogger = tranLogger;
    }

    // -------------------- Generate 14-digit account number --------------------
    private String generateAccountNumber(Connection con) throws SQLException {
        String prefix = "1000-";
        String sql = "SELECT ac_no FROM customer ORDER BY ac_no DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            long last = 0;
            if (rs.next()) {
                String lastAc = rs.getString("ac_no").replace("1000-", "");
                last = Long.parseLong(lastAc);
            }
            long next = last + 1;
            return prefix + String.format("%09d", next);
        }
    }

    // -------------------- Create Account --------------------
    public String createAccount(String username, String passCode, BigDecimal initialDeposit) {
        Connection con = dbConn.getConnection();
        try {
            con.setAutoCommit(false);

            String acNo = generateAccountNumber(con);

            String insert = "INSERT INTO customer (ac_no, cname, pass_code, balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                ps.setString(1, acNo);
                ps.setString(2, username);
                ps.setString(3, passCode);
                ps.setBigDecimal(4, initialDeposit.setScale(2, RoundingMode.HALF_EVEN));
                ps.executeUpdate();
            }

            tranLogger.log(con, UUID.randomUUID().toString(), "DEPOSIT", null, acNo, initialDeposit,
                    BigDecimal.ZERO, initialDeposit, "SUCCESS", "Initial deposit", "system");

            con.commit();
            return acNo;

        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.err.println("createAccount error: " + e.getMessage());
            return null;
        }
    }

    // -------------------- Login --------------------
    public String login(String formattedAcNo, String passCode) {
        try {
            Connection con = dbConn.getConnection();
            String sql = "SELECT pass_code FROM customer WHERE ac_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, formattedAcNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String dbPass = rs.getString("pass_code");
                        if (dbPass.equals(passCode)) return formattedAcNo;
                        else System.out.println("❌ Incorrect password.");
                    } else {
                        System.out.println("❌ Account not found.");
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("login error: " + e.getMessage());
            return null;
        }
    }

    // -------------------- Deposit --------------------
    public boolean deposit(String acNo, BigDecimal amount, String performedBy) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) { System.out.println("❌ Deposit must be positive."); return false; }

        Connection con = dbConn.getConnection();
        try {
            con.setAutoCommit(false);

            BigDecimal before;
            try (PreparedStatement ps = con.prepareStatement("SELECT balance FROM customer WHERE ac_no = ? FOR UPDATE")) {
                ps.setString(1, acNo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); before = rs.getBigDecimal("balance"); }
            }

            try (PreparedStatement ps = con.prepareStatement("UPDATE customer SET balance = balance + ? WHERE ac_no = ?")) {
                ps.setBigDecimal(1, amount); ps.setString(2, acNo); ps.executeUpdate();
            }

            BigDecimal after = before.add(amount);
            tranLogger.log(con, UUID.randomUUID().toString(), "DEPOSIT", null, acNo, amount, before, after, "SUCCESS", "Deposit", performedBy);

            con.commit();
            System.out.println("✅ Deposit successful. New balance: " + after);
            return true;

        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.err.println("deposit error: " + e.getMessage());
            return false;
        }
    }

    // -------------------- Withdraw --------------------
    public boolean withdraw(String acNo, BigDecimal amount, String performedBy) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) { System.out.println("❌ Withdraw must be positive."); return false; }

        Connection con = dbConn.getConnection();
        try {
            con.setAutoCommit(false);

            BigDecimal before;
            try (PreparedStatement ps = con.prepareStatement("SELECT balance FROM customer WHERE ac_no = ? FOR UPDATE")) {
                ps.setString(1, acNo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); before = rs.getBigDecimal("balance"); }
            }

            if (before.compareTo(amount) < 0) { System.out.println("❌ Insufficient funds."); return false; }

            try (PreparedStatement ps = con.prepareStatement("UPDATE customer SET balance = balance - ? WHERE ac_no = ?")) {
                ps.setBigDecimal(1, amount); ps.setString(2, acNo); ps.executeUpdate();
            }

            BigDecimal after = before.subtract(amount);
            tranLogger.log(con, UUID.randomUUID().toString(), "WITHDRAW", acNo, null, amount, before, after, "SUCCESS", "Withdraw", performedBy);

            con.commit();
            System.out.println("✅ Withdrawal successful. New balance: " + after);
            return true;

        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.err.println("withdraw error: " + e.getMessage());
            return false;
        }
    }

    // -------------------- Transfer --------------------
    public boolean transfer(String fromAcNo, String toAcNo, BigDecimal amount, String performedBy) {
        if (fromAcNo.equals(toAcNo)) { System.out.println("❌ Cannot transfer to same account."); return false; }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) { System.out.println("❌ Transfer must be positive."); return false; }

        Connection con = dbConn.getConnection();
        try {
            con.setAutoCommit(false);

            BigDecimal fromBal, toBal;
            try (PreparedStatement ps = con.prepareStatement("SELECT ac_no, balance FROM customer WHERE ac_no IN (?, ?) FOR UPDATE")) {
                ps.setString(1, fromAcNo); ps.setString(2, toAcNo);
                try (ResultSet rs = ps.executeQuery()) {
                    fromBal = toBal = null;
                    while (rs.next()) {
                        String ac = rs.getString("ac_no");
                        BigDecimal bal = rs.getBigDecimal("balance");
                        if (ac.equals(fromAcNo)) fromBal = bal;
                        if (ac.equals(toAcNo)) toBal = bal;
                    }
                }
            }

            if (fromBal == null || toBal == null) { System.out.println("❌ Account not found."); return false; }
            if (fromBal.compareTo(amount) < 0) { System.out.println("❌ Insufficient funds."); return false; }

            try (PreparedStatement ps = con.prepareStatement("UPDATE customer SET balance = ? WHERE ac_no = ?")) {
                ps.setBigDecimal(1, fromBal.subtract(amount)); ps.setString(2, fromAcNo); ps.executeUpdate();
                ps.setBigDecimal(1, toBal.add(amount)); ps.setString(2, toAcNo); ps.executeUpdate();
            }

            BigDecimal fromAfter = fromBal.subtract(amount);
            BigDecimal toAfter = toBal.add(amount);
            String txRef = UUID.randomUUID().toString();
            tranLogger.log(con, txRef, "TRANSFER", fromAcNo, toAcNo, amount, fromBal, fromAfter, "SUCCESS", "Transfer debit", performedBy);
            tranLogger.log(con, txRef, "TRANSFER", fromAcNo, toAcNo, amount, toBal, toAfter, "SUCCESS", "Transfer credit", performedBy);

            con.commit();
            System.out.println("✅ Transfer successful. From: " + fromAfter + ", To: " + toAfter);
            return true;

        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.err.println("transfer error: " + e.getMessage());
            return false;
        }
    }

    // -------------------- Check Balance --------------------
    public BigDecimal getBalance(String acNo) {
        try (PreparedStatement ps = dbConn.getConnection().prepareStatement("SELECT balance FROM customer WHERE ac_no = ?")) {
            ps.setString(1, acNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("balance");
                System.out.println("❌ Account not found."); return null;
            }
        } catch (SQLException e) { System.err.println("getBalance error: " + e.getMessage()); return null; }
    }

    // -------------------- Transaction History --------------------
    public List<String> getTransactionHistory(String acNo) {
        List<String> history = new ArrayList<>();
        String sql = """
            SELECT tran_type, ac_no_from, ac_no_to, amount, balance_before, balance_after, status, created_at, note
            FROM transactions
            WHERE ac_no_from = ? OR ac_no_to = ?
            ORDER BY created_at DESC
            """;

        try (PreparedStatement ps = dbConn.getConnection().prepareStatement(sql)) {
            ps.setString(1, acNo); ps.setString(2, acNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String formatted = String.format("[%s] Type: %s | From: %s | To: %s | Amount: %s | Before: %s | After: %s | Status: %s | Note: %s",
                            rs.getTimestamp("created_at"),
                            rs.getString("tran_type"),
                            rs.getString("ac_no_from") == null ? "-" : rs.getString("ac_no_from"),
                            rs.getString("ac_no_to") == null ? "-" : rs.getString("ac_no_to"),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_before"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("status"),
                            rs.getString("note"));
                    history.add(formatted);
                }
            }
        } catch (SQLException e) { System.err.println("getTransactionHistory error: " + e.getMessage()); }
        return history;
    }
}
