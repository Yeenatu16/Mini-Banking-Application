package com.banking;

import java.math.BigDecimal;
import java.sql.*;

public class TransactionLogger {
    private final DBConnection dbConn;

    public TransactionLogger(DBConnection dbConn) {
        this.dbConn = dbConn;
    }

    public long log(Connection con,
                    String tranRef,
                    String tranType,
                    String acFrom,
                    String acTo,
                    BigDecimal amount,
                    BigDecimal balanceBefore,
                    BigDecimal balanceAfter,
                    String status,
                    String note,
                    String createdBy) throws SQLException {

        String sql = """
            INSERT INTO transactions
              (tran_ref, tran_type, ac_no_from, ac_no_to, amount, balance_before, balance_after, status, note, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tranRef);
            ps.setString(2, tranType);
            ps.setString(3, acFrom);
            ps.setString(4, acTo);
            ps.setBigDecimal(5, amount.setScale(2, BigDecimal.ROUND_HALF_EVEN));
            ps.setBigDecimal(6, balanceBefore == null ? BigDecimal.ZERO : balanceBefore.setScale(2, BigDecimal.ROUND_HALF_EVEN));
            ps.setBigDecimal(7, balanceAfter == null ? BigDecimal.ZERO : balanceAfter.setScale(2, BigDecimal.ROUND_HALF_EVEN));
            ps.setString(8, status);
            ps.setString(9, note);
            ps.setString(10, createdBy);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }
}
