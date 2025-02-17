/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.pitest.mutationtest;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.pitest.mutationtest.DetectionStatus.KILLED;
import static org.pitest.mutationtest.DetectionStatus.NO_COVERAGE;
import static org.pitest.mutationtest.DetectionStatus.RUN_ERROR;
import static org.pitest.mutationtest.DetectionStatus.SURVIVED;
import static org.pitest.mutationtest.DetectionStatus.TIMED_OUT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.pitest.SystemTest;
import org.pitest.classpath.ClassPath;
import org.pitest.help.PitHelpError;
import org.pitest.mutationtest.engine.gregor.Generated;
import org.pitest.util.FileUtil;
import org.pitest.util.IsolationUtils;

import com.example.BeforeAfterClassTest;
import com.example.CoveredByABeforeAfterClass;
import com.example.CoveredByEasyMock;
import com.example.CoveredByJUnitThreeSuite;
import com.example.CrashesJVMWhenMutated;
import com.example.FailsTestWhenEnvVariableSetTestee;
import com.example.FullyCoveredTestee;
import com.example.FullyCoveredTesteeTest;
import com.example.HasMutationInFinallyBlockNonTest;
import com.example.HasMutationInFinallyBlockTest;
import com.example.HasMutationsInFinallyBlock;
import com.example.JUnitThreeSuite;
import com.example.KeepAliveThread;
import com.example.MultipleMutations;
import com.example.coverage.execute.samples.mutationMatrix.TestsForSimpleCalculator;
import org.pitest.util.PitError;
import org.pitest.util.Verbosity;
import org.pitest.mutationtest.build.PercentAndConstantTimeoutStrategy;

@Category(SystemTest.class)
public class MutationCoverageReportSystemTest extends ReportTestBase {

  private static final int ONE_MINUTE = 60000;

  @Before
  public void excludeTests() {
    this.data.setExcludedClasses(asList("*Test"));
  }

  @Test
  public void shouldPickRelevantTestsAndKillMutationsBasedOnCoverageData() {
    this.data.setTargetClasses(asList("com.example.FullyCovered*"));
    this.data.setVerbosity(Verbosity.VERBOSE);
    createAndRun();
    verifyResults(KILLED);
  }

  @Test
  public void shouldReportUnCoveredMutations() {
    this.data.setTargetClasses(asList("com.example.PartiallyCovered*"));
    createAndRun();
    verifyResults(KILLED, NO_COVERAGE);
  }

  @Test
  public void shouldReportSurvivingMutations() {
    this.data
    .setTargetClasses(asList("com.example.CoveredButOnlyPartiallyTested*"));
    createAndRun();
    verifyResults(KILLED, SURVIVED);
  }


  @Test(expected = PitHelpError.class)
  public void shouldFailRunWithHelpfulMessageIfTestsNotGreen() {
    setMutators("MATH");
    this.data
    .setTargetClasses(asList("com.example.FailsTestWhenEnvVariableSet*"));
    this.data.addChildJVMArgs(Arrays.asList("-D"
        + FailsTestWhenEnvVariableSetTestee.class.getName() + "=true"));
    createAndRun();
    // should not get here
  }

  @Test
  public void shouldNotFailRunIfSkipFailedTests() {
    setMutators("MATH");
    this.data
    .setTargetClasses(asList("com.example.FailsTestWhenEnvVariableSet*"));
    this.data.addChildJVMArgs(Arrays.asList("-D"
        + FailsTestWhenEnvVariableSetTestee.class.getName() + "=true"));
    this.data.setSkipFailingTests(true);
    createAndRun();
    verifyResults(NO_COVERAGE);
  }

  @Test
  public void shouldLoadResoucesOffClassPathFromFolderWithSpaces() {
    setMutators("FALSE_RETURNS");
    this.data
    .setTargetClasses(asList("com.example.LoadsResourcesFromClassPath*"));
    this.data.setVerbosity(Verbosity.VERBOSE);
    createAndRun();
    verifyResults(KILLED);
  }

  @Test
  public void shouldPickRelevantTestsFromSuppliedTestSuites() {
    this.data.setTargetClasses(asList("com.example.FullyCovered*"));
    this.data
    .setTargetTests(predicateFor(com.example.SuiteForFullyCovered.class));
    createAndRun();
    verifyResults(KILLED);
  }

  @Test
  public void shouldNotMutateMethodsMatchingExclusionPredicate() {
    this.data.setTargetClasses(asList("com.example.HasExcludedMethods*"));
    this.data.setExcludedMethods(Arrays.asList("excludeMe"));
    createAndRun();
    verifyResults();
  }

  @Test
  public void shouldLimitNumberOfMutationsPerClass() {
    this.data.setTargetClasses(asGlobs(MultipleMutations.class));
    this.data
    .setTargetTests(predicateFor(com.example.FullyCoveredTesteeTest.class));
    this.data.setFeatures(Collections.singletonList("+CLASSLIMIT(limit[1])"));
    createAndRun();
    verifyResults(NO_COVERAGE);
  }

  @Test
  public void shouldWorkWithEasyMock() {
    this.data.setTargetClasses(asGlobs(CoveredByEasyMock.class));
    this.data.setTargetTests(predicateFor(com.example.EasyMockTest.class));
    createAndRun();
    verifyResults(KILLED, KILLED);
  }

  @Test
  public void shouldWorkWithMockitoJUnitRunner() {
    this.data.setTargetClasses(asList("com.example.MockitoCallFoo"));
    this.data.setTargetTests(predicateFor(com.example.MockitoRunnerTest.class));
    this.data.setVerbosity(Verbosity.VERBOSE);
    createAndRun();
    verifyResults(KILLED);
  }

  @Test(expected = PitHelpError.class)
  public void shouldReportHelpfulErrorIfNoMutationsFounds() {
    this.data.setFailWhenNoMutations(true);
    this.data.setTargetClasses(asList("foo"));
    createAndRun();
  }

  @Test
  public void shouldExcludeFilteredTests() {
    this.data.setTargetTests(predicateFor("com.example.*FullyCoveredTestee*"));
    this.data.setTargetClasses(asList("com.example.FullyCovered*"));
    this.data.setExcludedTestClasses(predicateFor(FullyCoveredTesteeTest.class));
    createAndRun();
    verifyResults(NO_COVERAGE);
  }

  @Test
  public void willAllowExcludedClassesToBeReIncludedViaSuite() {
    this.data
    .setTargetTests(predicateFor("com.example.*SuiteForFullyCovered*"));
    this.data.setTargetClasses(asList("com.example.FullyCovered*"));
    this.data.setExcludedClasses(asGlobs(FullyCoveredTesteeTest.class));
    createAndRun();
    verifyResults(KILLED);
  }
  
  @Test
  public void computesFullMutationMatrix() {
    this.data
    .setTargetTests(predicateFor("com.example.coverage.execute.samples.mutationMatrix.*"));
    this.data.setTargetClasses(asList("com.example.coverage.execute.samples.mutationMatrix.*"));
    this.data.setExcludedMethods(asList("crash"));
    this.data.setExcludedClasses(asGlobs(TestsForSimpleCalculator.class));
    this.data.setFullMutationMatrix(true);
    this.data.addOutputFormats(Arrays.asList("XML"));
    this.data.setMutators(Arrays.asList("MATH"));
    this.data.setTimeoutConstant(PercentAndConstantTimeoutStrategy.DEFAULT_CONSTANT);
    this.data.setTimeoutFactor(PercentAndConstantTimeoutStrategy.DEFAULT_FACTOR);
    createAndRun();
    List<MutationResult> resultData = this.metaDataExtractor.getData();
    assertEquals(1, resultData.size());
    
    MutationResult mutation = resultData.get(0);
    assertEquals(KILLED, mutation.getStatus());
    assertEquals(5, mutation.getNumberOfTestsRun());
    //assertEquals(3, mutation.getNumberOfTestsRun());
    assertEquals(2, mutation.getKillingTests().size());
    assertEquals(1, mutation.getSucceedingTests().size());
    assertEquals(0, mutation.getMemoryErrorTests().size());
    assertEquals(1, mutation.getTimeoutTests().size());
    assertEquals(1, mutation.getRunErrorTests().size());
  }

  @Test(expected = PitHelpError.class)
  public void shouldExcludeFilteredClasses() {
    this.data.setFailWhenNoMutations(true);
    this.data.setTargetClasses(asGlobs(FullyCoveredTestee.class));
    this.data.setExcludedClasses(asGlobs(FullyCoveredTestee.class));
    createAndRun();
  }

  @Test
  public void shouldMutateClassesSuppliedToAlternateClassPath()
      throws IOException {
    // yes, this is horrid
    final String location = FileUtil.randomFilename() + ".jar";
    try {
      try (FileOutputStream fos = new FileOutputStream(location)) {
        final InputStream stream = IsolationUtils.getContextClassLoader()
             .getResourceAsStream("outofcp.jar");
        copy(stream, fos);
      }

      this.data.setTargetClasses(asList("com.outofclasspath.*Mutee*"));
      this.data.setTargetTests(predicateFor("com.outofclasspath.*"));

      final List<String> cp = new ArrayList<>();
      cp.addAll(ClassPath.getClassPathElementsAsPaths());
      cp.add(location);

      this.data.setClassPathElements(cp);
      this.data.setExcludedClasses(asList("*Power*", "*JMockit*"));
      createAndRun();
      verifyResults(KILLED);
    } finally {
      new File(location).delete();
    }
  }

  @Test(timeout = ONE_MINUTE)
  public void shouldTerminateWhenThreadpoolCreated() {
    this.data.setTargetClasses(asGlobs(KeepAliveThread.class));
    this.data
    .setTargetTests(predicateFor(com.example.KeepAliveThreadTest.class));
    createAndRun();
    verifyResults(SURVIVED);
  }

  @Test
  public void shouldMarkChildJVMCrashesAsRunErrors() {
    setMutators("NEGATE_CONDITIONALS");
    this.data.setTargetClasses(asGlobs(CrashesJVMWhenMutated.class));
    this.data
    .setTargetTests(predicateFor(com.example.TestCrashesJVMWhenMutated.class));
    createAndRun();

    verifyResults(RUN_ERROR);

  }

  @Test
  public void shouldCombineAndKillInlinedMutationsInFinallyBlocks() {
    setMutators("INCREMENTS");
    this.data.setTargetClasses(asGlobs(HasMutationsInFinallyBlock.class));
    this.data.setTargetTests(predicateFor(HasMutationInFinallyBlockTest.class));
    this.data.setDetectInlinedCode(true);
    createAndRun();

    verifyResults(KILLED);
  }

  @Test
  public void shouldUseTestsDefinedInASuppliedJUnitThreeSuite() {
    setMutators("PRIMITIVE_RETURNS");
    this.data.setTargetClasses(asGlobs(CoveredByJUnitThreeSuite.class));
    this.data.setTargetTests(predicateFor(JUnitThreeSuite.class));
    this.data.setVerbosity(Verbosity.VERBOSE);
    createAndRun();

    verifyResults(KILLED);
  }

  @Test
  public void shouldReportCombinedCoveredButNotTestedMutationsInFinallyBlocksAsSurvived() {
    setMutators("INCREMENTS");
    this.data.setTargetClasses(asGlobs(HasMutationsInFinallyBlock.class));
    this.data
    .setTargetTests(predicateFor(HasMutationInFinallyBlockNonTest.class));
    this.data.setDetectInlinedCode(true);
    createAndRun();

    verifyResults(SURVIVED);
  }

  @Test
  public void shouldExitAfterFirstFailureWhenTestClassAnnotatedWithBeforeClass() {
    setMutators("PRIMITIVE_RETURNS");
    this.data
    .setTargetClasses(asGlobs(CoveredByABeforeAfterClass.class));
    this.data.setTargetTests(predicateFor(BeforeAfterClassTest.class));

    createAndRun();

    verifyResults(KILLED);
    assertEquals(1, this.metaDataExtractor.getNumberOfTestsRun());
  }

  @Test
  public void shouldKillMutationsWhenMutationsPreventsConstructionOfTestClass() {
    setMutators("PRIMITIVE_RETURNS");

    this.data
    .setTargetClasses(asGlobs(com.example.mutatablecodeintest.Mutee.class));
    this.data
    .setTargetTests(predicateFor(com.example.mutatablecodeintest.MuteeTest.class));

    createAndRun();

    verifyResults(KILLED);
  }

  @Test
  public void shouldKillMutationsWhenKillingTestClassContainsAnIgnoreOnAnotherMethod() {
    setMutators("PRIMITIVE_RETURNS");

    this.data
    .setTargetClasses(asGlobs(com.example.testhasignores.Mutee.class));
    this.data
    .setTargetTests(predicateFor(com.example.testhasignores.MuteeTest.class));

    createAndRun();

    verifyResults(KILLED);
  }


  @Test
  public void shouldNotMutateStaticMethodsOnlyCalledFromInitializer() {
    setMutators("VOID_METHOD_CALLS");

    this.data
    .setTargetClasses(asGlobs(com.example.staticinitializers.MethodsCalledOnlyFromInitializer.class));

    createAndRun();

    verifyResults();
  }

  @Test
  public void willMutateStaticMethodsCalledFromInitializerAndElsewhere() {
    setMutators("VOID_METHOD_CALLS");

    this.data
    .setTargetClasses(asGlobs(com.example.staticinitializers.MethodsCalledFromInitializerAndElseWhere.class));

    createAndRun();

    // would prefer NO_COVERAGE here
    verifyResults();
  }

  @Test
  public void shouldMutateNonPrivateStaticMethodsCalledFromInitializerOnly() {
    setMutators("VOID_METHOD_CALLS");

    this.data
    .setTargetClasses(asGlobs(com.example.staticinitializers.NonPrivateMethodsCalledFromInitializerOnly.class));

    createAndRun();

    verifyResults(NO_COVERAGE,NO_COVERAGE,NO_COVERAGE);
  }

  @Test
  public void doesNotMutatePrivateMethodsCalledInChainFromInitializer() {
    setMutators("VOID_METHOD_CALLS");

    this.data
    .setTargetClasses(asGlobs(com.example.staticinitializers.MethodsCalledInChainFromStaticInitializer.class));

    createAndRun();

    verifyResults();
  }

  @Test
  public void shouldNotMutateClassesAnnotatedWithGenerated() {
    setMutators("PRIMITIVE_RETURNS");
    this.data
    .setTargetClasses(asGlobs(AnnotatedToAvoidAtClassLevel.class));

    createAndRun();

    verifyResults();
  }

  @Test(timeout = 20000, expected = PitError.class)
  public void shouldRecoverFromMinionThatDoesNotStart() {
    setMutators("PRIMITIVE_RETURNS");

    this.data
            .setTargetClasses(asGlobs(com.example.testhasignores.Mutee.class));
    this.data
            .setTargetTests(predicateFor(com.example.testhasignores.MuteeTest.class));

    this.data.addChildJVMArgs(asList("-Xmx1k"));

    createAndRun();

    // just running without a hang then erroring is success
  }

  @Generated
  public static class AnnotatedToAvoidAtClassLevel {
    public int mutateMe() {
      return 42;
    }
  }


  private static void copy(final InputStream in, final OutputStream out)
      throws IOException {
    // Read bytes and write to destination until eof

    final byte[] buf = new byte[1024];
    int len = 0;
    while ((len = in.read(buf)) >= 0) {
      out.write(buf, 0, len);
    }
  }

  private static Collection<String> asGlobs(Class<?> clazz) {
    return Collections.singleton(clazz.getName());
  }

}
