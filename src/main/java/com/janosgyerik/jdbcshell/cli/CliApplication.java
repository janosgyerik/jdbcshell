package com.janosgyerik.jdbcshell.cli;

import com.janosgyerik.jdbcshell.args.ArgumentsParser;
import com.janosgyerik.jdbcshell.args.GlobalValidators;
import com.janosgyerik.jdbcshell.args.Validator;
import com.janosgyerik.jdbcshell.args.Validators;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class CliApplication {

  private static final String JDBC_URL_PROPERTY = "jdbc.url";
  private static final String JDBC_USERNAME_PROPERTY = "jdbc.username";
  private static final String JDBC_PASSWORD_PROPERTY = "jdbc.password";

  static final Map<String, String> DRIVER_CLASS_NAMES = new LinkedHashMap<>();
  private static final String SUPPORTED_DRIVERS;

  static {
    DRIVER_CLASS_NAMES.put("mysql", "com.mysql.jdbc.Driver");
    DRIVER_CLASS_NAMES.put("postgresql", "org.postgresql.Driver");
    DRIVER_CLASS_NAMES.put("oracle", "oracle.jdbc.OracleDriver");
    DRIVER_CLASS_NAMES.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    DRIVER_CLASS_NAMES.put("h2", "org.h2.Driver");
    DRIVER_CLASS_NAMES.put("derby", "org.apache.derby.jdbc.EmbeddedDriver");

    SUPPORTED_DRIVERS = DRIVER_CLASS_NAMES.keySet().toString();
  }

  private final System2 system2;
  private final ConnectionConfigConsumer connectionConfigConsumer;

  private CliApplication() {
    this(new System2(), (s, config) -> {
      JdbcShell jdbcShell = new JdbcShell(s, config);
      jdbcShell.testConnection();
    });
  }

  CliApplication(System2 system2, ConnectionConfigConsumer connectionConfigConsumer) {
    this.system2 = system2;
    this.connectionConfigConsumer = connectionConfigConsumer;
  }

  public static void main(String[] args) {
    new CliApplication().run(args);
  }

  void run(String[] args) {
    Validator<ConnectionConfig> configValidator = Validators.create(this::configFromPath);
    Validator<ConnectionConfig> urlValidator = Validators.create(this::configFromJdbcUrl);

    ArgumentsParser parser = ArgumentsParser.newBuilder()
      .setUsageLine("Usage: jdbcshell [-help] [OPTIONS...]")
      .addOption("-url", "Jdbc Url; supported drivers: " + SUPPORTED_DRIVERS, urlValidator)
      .addOption("-config", "Path to config.properties file", configValidator)
      .addGlobalValidator(GlobalValidators.eitherIsPresent("-config", "-url"))
      .build();

    ArgumentsParser.Result result = parser.parseArgs(args);

    if (result.isHelpRequested()) {
      system2.printlnOut(parser.usageString());
      system2.exit(0);
    } else if (!result.isValid()) {
      system2.printlnErr(result.errorString());
      system2.exit(1);
    } else {
      ConnectionConfig connectionConfig = findConnectionConfig(urlValidator, configValidator);
      try {
        connectionConfigConsumer.execute(system2, connectionConfig);
        system2.exit(0);
      } catch (Exception e) {
        system2.printlnErr(e.getMessage());
        system2.exit(1);
      }
    }
  }

  private ConnectionConfig findConnectionConfig(Validator<ConnectionConfig> urlValidator, Validator<ConnectionConfig> configValidator) {
    if (urlValidator.used()) {
      return urlValidator.value();
    }
    return configValidator.value();
  }

  private Properties loadProperties(String configPath) throws IOException {
    try (Reader reader = new FileReader(configPath)) {
      Properties properties = new Properties();
      properties.load(reader);
      return properties;
    }
  }

  private ConnectionConfig configFromPath(String path) {
    Properties properties;
    try {
      properties = loadProperties(path);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read configuration file: " + e.getMessage());
    }

    String url = properties.getProperty(JDBC_URL_PROPERTY);
    if (url == null) {
      throw new IllegalArgumentException("Missing required configuration: " + JDBC_URL_PROPERTY);
    }

    String driverClassName = computeJdbcDriverClassName(url);
    String username = properties.getProperty(JDBC_USERNAME_PROPERTY);
    String password = properties.getProperty(JDBC_PASSWORD_PROPERTY);
    return new ConnectionConfig(url, driverClassName, username, password);
  }

  private ConnectionConfig configFromJdbcUrl(String url) {
    String driverClassName = computeJdbcDriverClassName(url);
    return new ConnectionConfig(url, driverClassName, null, null);
  }

  private static String computeJdbcDriverClassName(String url) {
    if (!url.startsWith("jdbc:")) {
      throw new IllegalArgumentException("Expected JDBC URL to start with 'jdbc:', got: " + url);
    }

    String[] parts = url.split(":");
    if (parts.length < 3) {
      throw new IllegalArgumentException("Expected JDBC URL in the form 'jdbc:driverClassName:url', got: " + url);
    }

    String driverName = parts[1];
    String driverClassName = DRIVER_CLASS_NAMES.get(driverName);
    if (driverClassName == null) {
      throw new IllegalArgumentException("Unsupported driver: " + driverName + "; supported drivers: " + SUPPORTED_DRIVERS);
    }

    return driverClassName;
  }

  interface ConnectionConfigConsumer {
    void execute(System2 system2, ConnectionConfig connectionConfig) throws Exception;
  }
}
