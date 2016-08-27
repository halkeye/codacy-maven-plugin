package com.gavinmogan;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by halkeye on 2016-08-27.
 */
public class CodacyCoverageReporterMojoTest extends AbstractMojoTestCase {
    /** {@inheritDoc} */
    protected void setUp() throws Exception
    {
        // required
        super.setUp();
    }

    /** {@inheritDoc} */
    protected void tearDown() throws Exception
    {
        // required
        super.tearDown();
    }

    @Test
    public void execute() throws Exception {
        File pom = getTestFile( "src/test/resources/unit/project-to-test/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        CodacyCoverageReporterMojo codacyCoverageReporterMojo = (CodacyCoverageReporterMojo) lookupMojo( "coverage", pom );
        assertNotNull( codacyCoverageReporterMojo );
        codacyCoverageReporterMojo.execute();
    }

}