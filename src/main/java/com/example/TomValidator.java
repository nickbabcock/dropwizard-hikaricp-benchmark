package com.example;

import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.Validator;

import java.sql.Connection;
import java.sql.SQLException;

public class TomValidator implements Validator {
    @Override
    public boolean validate(Connection connection, int validateAction) {
        try {
            return (validateAction != PooledConnection.VALIDATE_BORROW || connection.isValid(0));
        } catch (SQLException e) {
            return false;
        }
    }
}
