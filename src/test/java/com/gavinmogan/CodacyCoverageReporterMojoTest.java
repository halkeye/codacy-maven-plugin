package com.gavinmogan;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.PlexusTestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class CodacyCoverageReporterMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Test
    public void testExecute() throws Exception {
        File pom = PlexusTestCase.getTestFile( "src/test/resources/unit/project-to-test" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) rule.lookupConfiguredMojo( pom, "coverage" );
        assertNotNull( codacyCoverageReporterMojo );
        assertThat(codacyCoverageReporterMojo.getCodacyApiBaseUrl().getCanonicalPath(), CoreMatchers.containsString("target\\gavin.json"));
        codacyCoverageReporterMojo.execute();
    }

}