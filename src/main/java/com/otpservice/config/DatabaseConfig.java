package com.otpservice.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
/**
 * Класс для управления подключениями к базе данных
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final Properties properties = new Properties();
    private static final BlockingQueue<Connection> connectionPool = new ArrayBlockingQueue<>(10);
    private static final List<Connection> usedConnections = new ArrayList<>();
    private static final int MAX_POOL_SIZE;
    private static final String DB_URL;
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;
    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    static {
        try (InputStream inputStream = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("Property file not found in classpath");
            }
            properties.load(inputStream);
            DB_URL = properties.getProperty("db.url");
            DB_USERNAME = properties.getProperty("db.username");
            DB_PASSWORD = properties.getProperty("db.password");
            MAX_POOL_SIZE = Integer.parseInt(properties.getProperty("db.pool.size", "10"));
            Class.forName("org.postgresql.Driver");
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                connectionPool.offer(createConnection());
            }
            logger.info("Database connection pool initialized with {} connections", MAX_POOL_SIZE);
        } catch (IOException | ClassNotFoundException | SQLException e) {
            logger.error("Error initializing database config", e);
            throw new RuntimeException("Error initializing database config", e);
        }
    }
    /**
     * Создает новое соединение с базой данных
     * @return новое соединение
     * @throws SQLException если произошла ошибка при создании соединения
     */
    private static Connection createConnection() throws SQLException {
        logger.debug("Creating new database connection");
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
    /**
     * Получает соединение из пула
     * @return соединение с базой данных
     * @throws SQLException если произошла ошибка при получении соединения
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = connectionPool.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (connection == null) {
                logger.warn("Connection pool exhausted, waited {} seconds. Creating emergency connection", CONNECTION_TIMEOUT_SECONDS);
                connection = createConnection();
            } else if (connection.isClosed()) {
                logger.warn("Connection was closed, creating new one");
                connection = createConnection();
            }
            synchronized (usedConnections) {
                usedConnections.add(connection);
            }
            logger.debug("Connection acquired from pool, available: {}, used: {}", connectionPool.size(), usedConnections.size());
            return new ConnectionWrapper(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", e);
        }
    }
    /**
     * Возвращает соединение в пул
     * @param connection соединение, которое нужно вернуть
     */
    public static void releaseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        if (connection instanceof ConnectionWrapper) {
            connection = ((ConnectionWrapper) connection).getWrappedConnection();
        }
        synchronized (usedConnections) {
            usedConnections.remove(connection);
        }
        boolean added = connectionPool.offer(connection);
        if (!added) {
            closeConnection(connection);
            logger.warn("Connection pool full, closing connection instead of returning to pool");
        } else {
            logger.debug("Connection returned to pool, available: {}, used: {}", connectionPool.size(), usedConnections.size());
        }
    }
    /**
     * Закрывает все соединения
     */
    public static void closeAllConnections() {
        logger.info("Closing all database connections");
        List<Connection> connectionsToClose;
        synchronized (usedConnections) {
            connectionsToClose = new ArrayList<>(usedConnections);
            usedConnections.clear();
        }
        connectionsToClose.forEach(DatabaseConfig::closeConnection);
        Connection connection;
        while ((connection = connectionPool.poll()) != null) {
            closeConnection(connection);
        }
    }
    /**
     * Закрывает отдельное соединение
     * @param connection соединение для закрытия
     */
    private static void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
    /**
     * Обертка для соединения, которая автоматически возвращает его в пул при закрытии
     */
    private static class ConnectionWrapper implements Connection {
        private final Connection wrappedConnection;
        private boolean isClosed;
        public ConnectionWrapper(Connection wrappedConnection) {
            this.wrappedConnection = wrappedConnection;
            this.isClosed = false;
        }
        public Connection getWrappedConnection() {
            return wrappedConnection;
        }
        @Override
        public void close() throws SQLException {
            if (!isClosed) {
                isClosed = true;
                releaseConnection(wrappedConnection);
            }
        }
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return wrappedConnection.unwrap(iface);
        }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return wrappedConnection.isWrapperFor(iface);
        }
        @Override
        public java.sql.Statement createStatement() throws SQLException {
            return wrappedConnection.createStatement();
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            return wrappedConnection.prepareStatement(sql);
        }
        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
            return wrappedConnection.prepareCall(sql);
        }
        @Override
        public String nativeSQL(String sql) throws SQLException {
            return wrappedConnection.nativeSQL(sql);
        }
        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            wrappedConnection.setAutoCommit(autoCommit);
        }
        @Override
        public boolean getAutoCommit() throws SQLException {
            return wrappedConnection.getAutoCommit();
        }
        @Override
        public void commit() throws SQLException {
            wrappedConnection.commit();
        }
        @Override
        public void rollback() throws SQLException {
            wrappedConnection.rollback();
        }
        @Override
        public boolean isClosed() throws SQLException {
            return isClosed || wrappedConnection.isClosed();
        }
        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException {
            return wrappedConnection.getMetaData();
        }
        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            wrappedConnection.setReadOnly(readOnly);
        }
        @Override
        public boolean isReadOnly() throws SQLException {
            return wrappedConnection.isReadOnly();
        }
        @Override
        public void setCatalog(String catalog) throws SQLException {
            wrappedConnection.setCatalog(catalog);
        }
        @Override
        public String getCatalog() throws SQLException {
            return wrappedConnection.getCatalog();
        }
        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            wrappedConnection.setTransactionIsolation(level);
        }
        @Override
        public int getTransactionIsolation() throws SQLException {
            return wrappedConnection.getTransactionIsolation();
        }
        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException {
            return wrappedConnection.getWarnings();
        }
        @Override
        public void clearWarnings() throws SQLException {
            wrappedConnection.clearWarnings();
        }
        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }
        @Override
        public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
            return wrappedConnection.getTypeMap();
        }
        @Override
        public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
            wrappedConnection.setTypeMap(map);
        }
        @Override
        public void setHoldability(int holdability) throws SQLException {
            wrappedConnection.setHoldability(holdability);
        }
        @Override
        public int getHoldability() throws SQLException {
            return wrappedConnection.getHoldability();
        }
        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException {
            return wrappedConnection.setSavepoint();
        }
        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            return wrappedConnection.setSavepoint(name);
        }
        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
            wrappedConnection.rollback(savepoint);
        }
        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
            wrappedConnection.releaseSavepoint(savepoint);
        }
        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return wrappedConnection.prepareStatement(sql, columnIndexes);
        }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return wrappedConnection.prepareStatement(sql, columnNames);
        }
        @Override
        public java.sql.Clob createClob() throws SQLException {
            return wrappedConnection.createClob();
        }
        @Override
        public java.sql.Blob createBlob() throws SQLException {
            return wrappedConnection.createBlob();
        }
        @Override
        public java.sql.NClob createNClob() throws SQLException {
            return wrappedConnection.createNClob();
        }
        @Override
        public java.sql.SQLXML createSQLXML() throws SQLException {
            return wrappedConnection.createSQLXML();
        }
        @Override
        public boolean isValid(int timeout) throws SQLException {
            return wrappedConnection.isValid(timeout);
        }
        @Override
        public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {
            wrappedConnection.setClientInfo(name, value);
        }
        @Override
        public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException {
            wrappedConnection.setClientInfo(properties);
        }
        @Override
        public String getClientInfo(String name) throws SQLException {
            return wrappedConnection.getClientInfo(name);
        }
        @Override
        public java.util.Properties getClientInfo() throws SQLException {
            return wrappedConnection.getClientInfo();
        }
        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return wrappedConnection.createArrayOf(typeName, elements);
        }
        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return wrappedConnection.createStruct(typeName, attributes);
        }
        @Override
        public void setSchema(String schema) throws SQLException {
            wrappedConnection.setSchema(schema);
        }
        @Override
        public String getSchema() throws SQLException {
            return wrappedConnection.getSchema();
        }
        @Override
        public void abort(java.util.concurrent.Executor executor) throws SQLException {
            wrappedConnection.abort(executor);
        }
        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
            wrappedConnection.setNetworkTimeout(executor, milliseconds);
        }
        @Override
        public int getNetworkTimeout() throws SQLException {
            return wrappedConnection.getNetworkTimeout();
        }
    }
} 