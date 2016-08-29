package com.gavinmogan;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.PlexusTestCase;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class CodacyCoverageReporterMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Test
    public void testJacoco() throws Exception {
        File pom = PlexusTestCase.getTestFile( "src/test/resources/unit/jacoco" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) rule.lookupConfiguredMojo( pom, "coverage" );
        assertNotNull( codacyCoverageReporterMojo );
        codacyCoverageReporterMojo.setCoverageReport(new File(pom, "jacoco/jacoco.xml"));
        codacyCoverageReporterMojo.execute();
    }

    @Test
    public void testCobertura() throws Exception {
        File pom = PlexusTestCase.getTestFile( "src/test/resources/unit/cobertura" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) rule.lookupConfiguredMojo( pom, "coverage" );
        assertNotNull( codacyCoverageReporterMojo );
        codacyCoverageReporterMojo.setCoverageReport(new File(pom, "cobertura/coverage.xml"));
        codacyCoverageReporterMojo.execute();
    }

}