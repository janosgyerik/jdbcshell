package com.janosgyerik.jdbcshell.args;

import java.util.Map;

/**
 * Perform global validation of command line arguments,
 * for example that there are no conflicting arguments.
 *
 * @see GlobalValidators utility class for existing validators
 */
public interface GlobalValidator {
  /**
   * @throws IllegalStateException when invalid options are found
   */
  void validate(Map<String, String> rawOptions);
}
