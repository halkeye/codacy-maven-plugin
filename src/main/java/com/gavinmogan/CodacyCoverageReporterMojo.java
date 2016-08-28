package com.gavinmogan;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Created by halkeye on 8/27/16.
 */
@Mojo( name = "coverage", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class CodacyCoverageReporterMojo extends AbstractMojo
{
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    @Parameter( defaultValue="${env.CODACY_PROJECT_TOKEN}", property = "apiToken", required = true )
    private String apiToken;

    @Parameter( defaultValue="${project.basedir}/codacy-coverage.json", property = "codacyReportFilename", required = true )
    private File codacyReportFilename;

    @Parameter( defaultValue = "Hello, world." )
    private String greeting;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        //File basedir = project.getBasedir();

        //CodacyClient client = new CodacyClient(apiToken);
        //CommitService codacyService = new CommitService(client);

        getLog().info(greeting);
        getLog().info("Uploading coverage data..." + codacyReportFilename);

        /*
        coverageService.sendReport(commitUUID, config.language, report) match {
        case requestResponse if requestResponse.hasError =>
            Left(s"Failed to upload report: ${requestResponse.message}")
        case requestResponse =>
            Right(s"Coverage data uploaded. ${requestResponse.message}")

        Commit commit = service.getCommit(username, projectName, commitUUID);
        */
    }
}
