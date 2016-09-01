package com.gavinmogan;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.PlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class CodacyCoverageReporterMojoTest {
    private File pom;

    @Rule
    public MojoRule rule = new MojoRule();

    @Before
    public void setUp() {
        this.pom = PlexusTestCase.getTestFile( "src/test/resources/unit/project-to-test" );
        assertNotNull( this.pom );
        assertTrue( this.pom.exists() );
    }

    @Test
    public void testJacoco() throws Exception {
        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) rule.lookupConfiguredMojo( this.pom, "coverage" );
        assertNotNull( codacyCoverageReporterMojo );
        codacyCoverageReporterMojo.setCoverageReport(new File(this.pom, "jacoco/jacoco.xml"));
        codacyCoverageReporterMojo.execute();
    }

    @Test
    public void testCobertura() throws Exception {

        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) rule.lookupConfiguredMojo( this.pom, "coverage" );
        assertNotNull( codacyCoverageReporterMojo );
        codacyCoverageReporterMojo.setCoverageReport(new File(pom, "cobertura/coverage.xml"));
        codacyCoverageReporterMojo.execute();
    }
}