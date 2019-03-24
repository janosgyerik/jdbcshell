package com.janosgyerik.jdbcshell.args;

import java.util.function.Function;

/**
 * Validate the value of a command line argument.
 *
 * @see Validators#create(Function) utility class to create custom validators
 */
public interface Validator<T> {
  /**
   * @return true if the validator was actually used
   */
  boolean used();

  /**
   * @return the validated value
   * @throws IllegalStateException if the validator was not actually used
   */
  T value();

  /**
   * @throws IllegalStateException with helpful message if the value is invalid
   */
  void validate(String rawValue);
}
