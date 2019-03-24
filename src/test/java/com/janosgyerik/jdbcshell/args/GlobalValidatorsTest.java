package com.janosgyerik.jdbcshell.args;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class GlobalValidatorsTest {
  @Test
  @UseDataProvider("eitherName1OrName2IsPresent")
  public void eitherIsPresent_passes_when_either_is_present(Map<String, String> rawOptions) {
    // expected to run without exceptions
    GlobalValidators.eitherIsPresent("name1", "name2").validate(rawOptions);
  }

  @DataProvider
  public static Object[][] eitherName1OrName2IsPresent() {
    return new Object[][]{
      {Collections.singletonMap("name1", "foo")},
      {Collections.singletonMap("name2", "bar")},
    };
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("neitherOrBothName1Name2ArePresent")
  public void eitherIsPresent_fails_when_neither_or_both_present(Map<String, String> rawOptions) {
    GlobalValidators.eitherIsPresent("name1", "name2").validate(rawOptions);
  }

  @DataProvider
  public static Object[][] neitherOrBothName1Name2ArePresent() {
    Map<String, String> bothPresent = new HashMap<>();
    bothPresent.put("name1", "foo");
    bothPresent.put("name2", "bar");

    return new Object[][]{
      {Collections.emptyMap()},
      {bothPresent},
    };
  }
}
