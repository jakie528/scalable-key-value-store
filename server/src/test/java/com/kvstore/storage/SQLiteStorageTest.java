package com.kvstore.storage;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SQLiteStorageTest {
    private SQLiteStorage storage;
    private Connection mockConnection;
    private Statement mockStatement;
    private PreparedStatement mockPreparedStatement;

    @Before
    public void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        mockPreparedStatement = mock(PreparedStatement.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);

        storage = new SQLiteStorage("jdbc:sqlite::memory:") {
            @Override
            protected Connection createConnection(String dbUrl) {
                return mockConnection;
            }
        };
    }

    @Test
    public void testPutAndGet() throws Exception {
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("value")).thenReturn("testValue");

        storage.put("testKey", "testValue");
        String result = storage.get("testKey");

        assertEquals("testValue", result);

        // Verify setString is called twice with the key (once for put, once for get)
        verify(mockPreparedStatement, times(2)).setString(1, "testKey");

        // Verify setString is called once with the value (for put)
        verify(mockPreparedStatement, times(1)).setString(2, "testValue");

        // Verify executeUpdate is called once (for put)
        verify(mockPreparedStatement, times(1)).executeUpdate();

        // Verify executeQuery is called once (for get)
        verify(mockPreparedStatement, times(1)).executeQuery();
    }

    // Add more test methods here...
}