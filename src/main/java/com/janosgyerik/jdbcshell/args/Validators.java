package com.janosgyerik.jdbcshell.args;

import java.util.function.Function;

public class Validators {
  private Validators() {
    // utility class, forbidden constructor
  }

  /**
   * Create a validator using the specified function to perform the validation.
   * <p>
   * Check with .used() if the validator was actually used,
   * and then .value() to get the valid value.
   */
  public static <T> Validator<T> create(Function<String, T> validator) {
    return new Validator<T>() {
      boolean used = false;
      T value = null;

      @Override
      public boolean used() {
        return used;
      }

      @Override
      public T value() {
        if (!used) {
          throw new IllegalStateException("Validator was not used");
        }
        return value;
      }

      @Override
      public void validate(String rawValue) {
        used = true;
        value = validator.apply(rawValue);
      }
    };
  }
}
