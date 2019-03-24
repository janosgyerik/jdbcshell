package com.janosgyerik.jdbcshell.cli;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static com.janosgyerik.jdbcshell.cli.CliApplication.DRIVER_CLASS_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(DataProviderRunner.class)
public class CliApplicationTest {

  private static final String USAGE_MESSAGE = "Usage: jdbcshell [-help] [OPTIONS...]\n" +
    "\n" +
    "Options:\n" +
    "\n" +
    "-url URL\n" +
    "  Jdbc Url; supported drivers: [mysql, postgresql, oracle, sqlserver, h2, derby]\n" +
    "-config CONFIG\n" +
    "  Path to config.properties file\n" +
    "-help\n" +
    "  Print this help\n";

  private final System2 system2 = mock(System2.class);
  private final CliApplication.ConnectionConfigConsumer connectionConfigConsumer = mock(CliApplication.ConnectionConfigConsumer.class);

  private final CliApplication underTest = new CliApplication(system2, connectionConfigConsumer);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void print_error_when_args_empty() {
    underTest.run(new String[0]);
    verify(system2).printlnErr("One (and only one) of these options is required: -config, -url");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_unexpected_args_present() {
    underTest.run(new String[]{"unexpected"});
    verify(system2).printlnErr("Unexpected arguments: unexpected");
    verify(system2).exit(1);
  }

  @Test
  @UseDataProvider("validOptionNames")
  public void print_error_when_option_parameter_missing(String name) {
    underTest.run(new String[]{name});
    verify(system2).printlnErr("Option " + name + " requires a parameter");
    verify(system2).exit(1);
  }

  @DataProvider
  public static Object[][] validOptionNames() {
    return new Object[][]{
      {"-url"},
      {"-config"},
    };
  }

  @Test
  @UseDataProvider("argsIncludingHelp")
  public void print_help_if_requested_and_skip_validation(String[] args) {
    underTest.run(args);
    verify(system2).printlnOut(USAGE_MESSAGE);
    verify(system2).exit(0);
  }

  @DataProvider
  public static Object[][] argsIncludingHelp() {
    return new Object[][]{
      {new String[]{"-help"}},
      {new String[]{"foo", "bar", "-help"}},
      {new String[]{"-foo", "bar", "-help"}},
      {new String[]{"-foo", "-bar", "-help"}},
      {new String[]{"-url", "url", "-help"}},
      {new String[]{"-config", "path", "-help"}},
    };
  }

  @Test
  public void print_error_when_url_doesnt_start_with_jdbc() {
    underTest.run(new String[]{"-url", "foo"});
    verify(system2).printlnErr("Expected JDBC URL to start with 'jdbc:', got: foo");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_url_malformed() {
    underTest.run(new String[]{"-url", "jdbc:"});
    verify(system2).printlnErr("Expected JDBC URL in the form 'jdbc:driverClassName:url', got: jdbc:");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_jdbc_driver_unsupported() {
    underTest.run(new String[]{"-url", "jdbc:foo:bar"});
    verify(system2).printlnErr("Unsupported driver: foo; supported drivers: [mysql, postgresql, oracle, sqlserver, h2, derby]");
    verify(system2).exit(1);
  }

  @Test
  public void run_main_job_with_successfully_parsed_configuration() throws Exception {
    String validUrl = "jdbc:mysql:bar";
    underTest.run(new String[]{"-url", validUrl});

    ArgumentCaptor<ConnectionConfig> connectionConfigArgumentCaptor = ArgumentCaptor.forClass(ConnectionConfig.class);
    verify(connectionConfigConsumer).execute(same(system2), connectionConfigArgumentCaptor.capture());
    ConnectionConfig config = connectionConfigArgumentCaptor.getValue();
    assertThat(config.url).isEqualTo(validUrl);
    assertThat(config.driverClassName).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(config.username).isNull();
    assertThat(config.password).isNull();

    verify(system2).exit(0);
  }

  @Test
  public void print_error_when_config_file_nonexistent() throws IOException {
    String nonexistent = temporaryFolder.newFolder().toPath().resolve("nonexistent").toString();
    underTest.run(new String[]{"-config", nonexistent});
    verify(system2).printlnErr("Could not read configuration file: " + nonexistent + " (No such file or directory)");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_jdbc_url_missing_from_config() throws IOException {
    String path = temporaryFolder.newFile().toString();
    underTest.run(new String[]{"-config", path});
    verify(system2).printlnErr("Missing required configuration: jdbc.url");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_url_doesnt_start_with_jdbc_in_config_file() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    Files.write(path, "jdbc.url = foo".getBytes());
    underTest.run(new String[]{"-config", path.toString()});
    verify(system2).printlnErr("Expected JDBC URL to start with 'jdbc:', got: foo");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_url_malformed_in_config_file() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    Files.write(path, "jdbc.url = jdbc:".getBytes());
    underTest.run(new String[]{"-config", path.toString()});
    verify(system2).printlnErr("Expected JDBC URL in the form 'jdbc:driverClassName:url', got: jdbc:");
    verify(system2).exit(1);
  }

  @Test
  public void print_error_when_jdbc_driver_unsupported_in_config_file() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    Files.write(path, "jdbc.url = jdbc:foo:bar".getBytes());
    underTest.run(new String[]{"-config", path.toString()});
    verify(system2).printlnErr("Unsupported driver: foo; supported drivers: [mysql, postgresql, oracle, sqlserver, h2, derby]");
    verify(system2).exit(1);
  }

  @Test
  public void run_main_job_with_successfully_parsed_configuration_from_file() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.write(path, "jdbc.url = jdbc:mysql:bar".getBytes());
    underTest.run(new String[]{"-config", path.toString()});

    ArgumentCaptor<ConnectionConfig> connectionConfigArgumentCaptor = ArgumentCaptor.forClass(ConnectionConfig.class);
    verify(connectionConfigConsumer).execute(same(system2), connectionConfigArgumentCaptor.capture());
    ConnectionConfig config = connectionConfigArgumentCaptor.getValue();
    assertThat(config.url).isEqualTo("jdbc:mysql:bar");
    assertThat(config.driverClassName).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(config.username).isNull();
    assertThat(config.password).isNull();

    verify(system2).exit(0);
  }

  @Test
  public void run_main_job_with_successfully_parsed_configuration_from_file_with_username_and_password() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.write(path, "jdbc.url = jdbc:mysql:bar\njdbc.username = foouser\njdbc.password=barpass\n".getBytes());
    underTest.run(new String[]{"-config", path.toString()});

    ArgumentCaptor<ConnectionConfig> connectionConfigArgumentCaptor = ArgumentCaptor.forClass(ConnectionConfig.class);
    verify(connectionConfigConsumer).execute(same(system2), connectionConfigArgumentCaptor.capture());
    ConnectionConfig config = connectionConfigArgumentCaptor.getValue();
    assertThat(config.url).isEqualTo("jdbc:mysql:bar");
    assertThat(config.driverClassName).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(config.username).isEqualTo("foouser");
    assertThat(config.password).isEqualTo("barpass");

    verify(system2).exit(0);
  }

  @Test
  public void print_error_when_execution_fails() throws Exception {
    String message = "failed because...";
    doThrow(new Exception(message)).when(connectionConfigConsumer).execute(same(system2), any());

    String validUrl = "jdbc:mysql:bar";
    underTest.run(new String[]{"-url", validUrl});

    verify(system2).printlnErr(message);
    verify(system2).exit(1);
  }

  @Test
  public void print_stdout_and_stderr_of_consumer() throws Exception {
    doAnswer(invocation -> {
      system2.printlnOut("some output");
      system2.printlnErr("some error");
      return null;
    }).when(connectionConfigConsumer).execute(same(system2), any());

    String validUrl = "jdbc:mysql:bar";
    underTest.run(new String[]{"-url", validUrl});

    verify(system2).printlnOut("some output");
    verify(system2).printlnErr("some error");
    verify(system2).exit(0);
  }

  @Test
  @UseDataProvider("supportedDriversAndClasses")
  public void run_main_job_for_any_supported_driver(String driverName, String driverClassName) throws Exception {
    String validUrl = "jdbc:" + driverName + ":bar";
    underTest.run(new String[]{"-url", validUrl});

    ArgumentCaptor<ConnectionConfig> connectionConfigArgumentCaptor = ArgumentCaptor.forClass(ConnectionConfig.class);
    verify(connectionConfigConsumer).execute(same(system2), connectionConfigArgumentCaptor.capture());
    ConnectionConfig config = connectionConfigArgumentCaptor.getValue();
    assertThat(config.url).isEqualTo(validUrl);
    assertThat(config.driverClassName).isEqualTo(driverClassName);
    assertThat(config.username).isNull();
    assertThat(config.password).isNull();

    verify(system2).exit(0);
  }

  @DataProvider
  public static Object[][] supportedDriversAndClasses() {
    return DRIVER_CLASS_NAMES.entrySet()
      .stream()
      .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
      .toArray(Object[][]::new);
  }
}
