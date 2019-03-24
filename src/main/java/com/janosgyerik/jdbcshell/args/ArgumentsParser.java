package com.janosgyerik.jdbcshell.args;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

public class ArgumentsParser {

  private final String usageString;
  private final Map<String, Option<?>> options;
  private final List<GlobalValidator> globalValidators;

  private ArgumentsParser(Builder builder) {
    usageString = builder.formatUsageString();
    options = builder.options;
    globalValidators = builder.globalValidators;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String usageString() {
    return usageString;
  }

  public static class Builder {
    private static final String NEWLINE = "\n";

    private String usageLine = "Usage: java -jar path/to/jar [-help] [OPTIONS...] [ARGS...]";
    private Map<String, Option<?>> options = new LinkedHashMap<>();
    private List<GlobalValidator> globalValidators = new ArrayList<>();

    public Builder setUsageLine(String usageLine) {
      this.usageLine = usageLine;
      return this;
    }

    public <T> Builder addOption(String name, String description, Validator<T> validator) {
      if (options.containsKey(name)) {
        throw new IllegalArgumentException("Option already defined: " + name);
      }
      this.options.put(name, new Option<>(name, description, validator));
      return this;
    }

    public Builder addGlobalValidator(GlobalValidator globalValidator) {
      this.globalValidators.add(globalValidator);
      return this;
    }

    public ArgumentsParser build() {
      return new ArgumentsParser(this);
    }

    private String formatUsageString() {
      StringBuilder sb = new StringBuilder(usageLine);
      sb.append(NEWLINE)
        .append(NEWLINE)
        .append("Options:")
        .append(NEWLINE)
        .append(NEWLINE);

      options.values().forEach(option -> {
        sb.append(option.name)
          .append(' ')
          .append(option.name.substring(1).toUpperCase(Locale.ENGLISH))
          .append(NEWLINE)
          .append("  ")
          .append(option.description)
          .append(NEWLINE);
      });

      sb.append("-help")
        .append(NEWLINE)
        .append("  Print this help")
        .append(NEWLINE);

      return sb.toString();
    }
  }

  public Result parseArgs(String[] args) {
    Result.Builder builder = Result.builder();

    if (Arrays.asList(args).contains("-help")) {
      builder.helpRequested();
      return builder.build();
    }

    Map<String, String> rawOptions = new HashMap<>();
    List<String> rest = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (options.containsKey(arg)) {
        if (rawOptions.containsKey(arg)) {
          builder.addError("Option " + arg + " was already specified");
          return builder.build();
        }

        i++;
        if (i == args.length) {
          builder.addError("Option " + arg + " requires a parameter");
          return builder.build();
        }

        rawOptions.put(arg, args[i]);
        continue;
      }

      if (arg.startsWith("-")) {
        builder.addError("Unknown option: " + arg);
        return builder.build();
      }

      rest.add(arg);
    }

    if (!rest.isEmpty()) {
      builder.addError("Unexpected arguments: " + String.join(", ", rest));
      return builder.build();
    }

    try {
      globalValidators.forEach(validator -> validator.validate(rawOptions));
      rawOptions.forEach((name, value) -> {
        Option option = options.get(name);
        option.validate(value);
      });
    } catch (RuntimeException e) {
      builder.addError(e.getMessage());
      return builder.build();
    }

    return builder.build();
  }

  @Immutable
  private static class Option<T> {
    private final String name;
    private final String description;
    private final Validator<T> validator;

    private Option(String name, String description, Validator<T> validator) {
      this.name = name;
      this.description = description;
      this.validator = validator;
    }

    void validate(String rawValue) {
      validator.validate(rawValue);
    }
  }

  public static class Result {

    private final List<String> errors;
    private final boolean helpRequested;

    private Result(Builder builder) {
      errors = builder.errors;
      helpRequested = builder.helpRequested;
    }

    static Builder builder() {
      return new Builder();
    }

    public boolean isValid() {
      if (isHelpRequested()) {
        throw new IllegalStateException("Unexpected call: validation is skipped when help is requested");
      }
      return errors.isEmpty();
    }

    public String errorString() {
      if (isValid()) {
        throw new IllegalStateException("Unexpected call: there should not be any errors when the parse result is valid");
      }
      return String.join("\n", errors);
    }

    public boolean isHelpRequested() {
      return helpRequested;
    }

    static class Builder {
      private boolean helpRequested;
      private final List<String> errors = new ArrayList<>();

      Result build() {
        return new Result(this);
      }

      void helpRequested() {
        this.helpRequested = true;
      }

      void addError(String message) {
        this.errors.add(message);
      }
    }
  }
}
