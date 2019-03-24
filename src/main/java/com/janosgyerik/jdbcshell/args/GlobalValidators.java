package com.janosgyerik.jdbcshell.args;

public class GlobalValidators {
  private GlobalValidators() {
    // utility class, forbidden constructor
  }

  public static GlobalValidator eitherIsPresent(String name1, String name2) {
    return rawOptions -> {
      if (rawOptions.containsKey(name1) == rawOptions.containsKey(name2)) {
        fail(String.format("One (and only one) of these options is required: %s, %s", name1, name2));
      }
    };
  }

  public static void fail(String message) {
    throw new IllegalStateException(message);
  }
}
