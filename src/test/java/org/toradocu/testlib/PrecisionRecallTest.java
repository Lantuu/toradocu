package org.toradocu.testlib;

import org.toradocu.Toradocu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class PrecisionRecallTest {

  public static TestCaseStats test(String targetClass, String srcPath, String expectedOutputDir) {
    String className = getClassName(targetClass);
    String actualOutputFile = "tmp" + File.separator + className + "_out.txt";
    String expectedOutputFile = expectedOutputDir + className + "_expected.txt";
    String message = "=== Test " + targetClass + " ===";

    Toradocu.main(
        new String[] {
          "--targetClass",
          targetClass,
          "--saveConditionTranslatorOutput",
          actualOutputFile,
          "--oracleGeneration", "false",
          "--aspectTemplate",
          "/Users/alberto/git/toradocu/src/main/resources/AspectTemplate.java",
          "--testClass",
          "foo",
          // "--debug",
          "-J-sourcepath=" + srcPath,
          "-J-docletpath=build/classes/main",
          "-J-d=tmp",
          "-J-quiet="
        });
    return compare(actualOutputFile, expectedOutputFile, message);
  }

  private static String getClassName(String qualifiedClassName) {
    return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
  }

  private static TestCaseStats compare(
      String outputFile, String expectedOutputFile, String message) {
    StringBuilder report = new StringBuilder();

    report.append(message + "\n");
    try (BufferedReader outFile = Files.newBufferedReader(Paths.get(outputFile));
        BufferedReader expFile = Files.newBufferedReader(Paths.get(expectedOutputFile))) {
      List<String> output = outFile.lines().collect(Collectors.toList());
      List<String> expected = expFile.lines().collect(Collectors.toList());
      Map<String, String> expectedTranslations = new HashMap<>();
      for (String line : expected) {
        final String[] tokens = line.split("==>");
        expectedTranslations.put(tokens[0], tokens[1]);
      }

      TestCaseStats result = new TestCaseStats();
      for (String line : output) {
        final String[] tokens = line.split("==>");
        final String condition = tokens[0];
        final String translation = tokens[1];

        final String expectedTranslation = expectedTranslations.get(condition);
        // Ignore results when expected translation is empty.
        if (!expectedTranslation.endsWith(" []") && !expectedTranslation.endsWith(" [???]")) {
          if (translation.equals(expectedTranslation)) {
            result.incrementCorrect();
          } else {
            if (translation.equals(" []")) {
              result.incrementMissig();
              report.append("Missing condition:" + line + "\n");
            } else {
              result.incrementWrong();
              report.append("Wrong condition: " + line + "\n");
            }
          }
        }
      }
//      float precision = result.getPrecision();
//      float recall = result.getRecall();
//      report.append("Conditions: " + expected.size() + "\n");
//      report.append("Precision: " + precision + "\n");
//      report.append("Recall: " + recall + "\n");
//      System.out.println(report);
      return result;
    } catch (IOException e) {
      fail(e.getMessage());
      return null;
    }
  }
}
