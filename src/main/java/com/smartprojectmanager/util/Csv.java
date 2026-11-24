package com.smartprojectmanager.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/** RFC 4180 escaping with spreadsheet formula neutralization. */
public final class Csv {
  private Csv() {}

  public static String row(Object... values) {
    return Arrays.stream(values).map(Csv::cell).collect(Collectors.joining(",")) + "\r\n";
  }

  private static String cell(Object value) {
    String v = value == null ? "" : value.toString();
    if (v.matches("(?s)^[\\s]*[=+@-].*") || v.startsWith("\t") || v.startsWith("\r")) v = "'" + v;
    return "\"" + v.replace("\"", "\"\"") + "\"";
  }
}
