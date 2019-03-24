package com.janosgyerik.jdbcshell.cli;

class System2 {
  void printlnOut(String s) {
    System.out.println(s);
  }

  void printlnErr() {
    printlnErr("");
  }

  void printlnErr(String s) {
    System.err.println(s);
  }

  void exit(int status) {
    System.exit(status);
  }
}
