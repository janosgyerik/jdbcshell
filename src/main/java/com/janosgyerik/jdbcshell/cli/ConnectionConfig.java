package com.janosgyerik.jdbcshell.cli;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
class ConnectionConfig {
  final String url;

  @Nullable
  final String username;

  @Nullable
  final String password;

  ConnectionConfig(String url, @Nullable String username, @Nullable String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }
}
