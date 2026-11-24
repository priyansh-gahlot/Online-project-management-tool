package com.smartprojectmanager;

import static org.junit.Assert.*;

import com.smartprojectmanager.util.Csv;
import org.junit.Test;

public class CsvTest {
  @Test
  public void escapesQuotesCommasAndMultilineText() {
    assertEquals(
        "\"a,b\",\"say \"\"hello\"\"\",\"line1\nline2\",\"\"\r\n",
        Csv.row("a,b", "say \"hello\"", "line1\nline2", null));
  }

  @Test
  public void neutralizesFormulaCellsIncludingLeadingWhitespace() {
    assertEquals(
        "\"'=SUM(1,2)\",\"' +2\",\"'@X\",\"'-1\",\"ordinary\"\r\n",
        Csv.row("=SUM(1,2)", " +2", "@X", "-1", "ordinary"));
  }
}
