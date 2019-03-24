package com.janosgyerik.jdbcshell.cli;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ConnectionConfig {
  final String url;
  final String driverClassName;

  @Nullable
  final String username;

  @Nullable
  final String password;

  public ConnectionConfig(String url, String driverClassName, @Nullable String username, @Nullable String password) {
    this.url = url;
    this.driverClassName = driverClassName;
    this.username = username;
    this.password = password;
  }
}
