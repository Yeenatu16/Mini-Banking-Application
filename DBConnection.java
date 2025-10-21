package com.banking;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private final String url;
    private final String user;
    private final String password;

    public DBConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            return null;
        }
    }
}
