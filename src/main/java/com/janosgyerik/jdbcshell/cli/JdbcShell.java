package com.janosgyerik.jdbcshell.cli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcShell {

  private final System2 system2;
  private final ConnectionConfig config;
  private final JdbcTemplate jdbcTemplate;

  public JdbcShell(System2 system2, ConnectionConfig config) {
    this.system2 = system2;
    this.config = config;

    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(config.driverClassName);
    dataSource.setUrl(config.url);
    dataSource.setUsername(config.username);
    dataSource.setPassword(config.password);

    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  void testConnection() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", config.username);
    properties.setProperty("password", config.password);
    try (Connection connection = DriverManager.getConnection(config.url, properties)) {
      system2.printlnOut("Connection test successful!");
    } catch (SQLException e) {
      system2.printlnErr("Connection test failed!");
      throw e;
    }
  }
}
