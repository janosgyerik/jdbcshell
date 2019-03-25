package com.janosgyerik.jdbcshell.cli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

class JdbcShell {

  private final System2 system2;
  private final ConnectionConfig config;

  public JdbcShell(System2 system2, ConnectionConfig config) {
    this.system2 = system2;
    this.config = config;
  }

  void testConnection() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", config.username);
    properties.setProperty("password", config.password);
    try (Connection unused = DriverManager.getConnection(config.url, properties)) {
      system2.printlnOut("Connection test successful!");
    } catch (SQLException e) {
      system2.printlnErr("Connection test failed!");
      throw e;
    }
  }
}
