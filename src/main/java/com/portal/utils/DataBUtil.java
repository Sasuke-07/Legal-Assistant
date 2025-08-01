package com.portal.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for database connection management.
 */
public class DataBUtil {
    private static final Logger LOGGER = Logger.getLogger(DataBUtil.class.getName());
    
    // Database connection parameters
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/legalassist";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // Set your database password here
    
    // Connection pool size parameters
    private static final int MAX_POOL_SIZE = 20;
    private static int currentConnections = 0;
    
    static {
        try {
            // Load the JDBC driver
            Class.forName(JDBC_DRIVER);
            LOGGER.info("MySQL JDBC Driver registered successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }
    
    /**
     * Get a database connection from the pool
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (currentConnections >= MAX_POOL_SIZE) {
            LOGGER.warning("Connection pool limit reached. Waiting for available connection.");
            try {
                Thread.sleep(1000); // Wait before trying again
                return getConnection();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for connection", e);
            }
        }
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            currentConnections++;
            LOGGER.fine("Database connection established. Active connections: " + currentConnections);
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish database connection", e);
            throw e;
        }
    }
    
    /**
     * Release a connection back to the pool
     * @param conn The connection to release
     */
    public static synchronized void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                currentConnections--;
                LOGGER.fine("Connection released. Active connections: " + currentConnections);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
    
    /**
     * Test database connectivity
     * @return true if connection can be established, false otherwise
     */
    public static boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            return conn.isValid(2); // Check if connection is valid with 2 second timeout
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }
    
    /**
     * Get the number of active connections
     * @return current number of connections
     */
    public static int getActiveConnectionCount() {
        return currentConnections;
    }
}