package com.gavinmogan;

import codacy.api.CodacyClient;
import codacy.api.services.CommitService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created by halkeye on 8/27/16.
 */
@Mojo( name = "coverage", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class CodacyCoverageReporterMojo     extends AbstractMojo
{
    private final Logger logger = Logger.getLogger(CodacyCoverageReporterMojo.class.getName());

    @Parameter( defaultValue="${env.CODACY_PROJECT_TOKEN}", property = "apiToken", required = true )
    private String apiToken;

    @Parameter( defaultValue="${project..directory}${File.seperator}codacy-coverage.json", property = "codacyReportFilename", required = true )
    private String codacyReportFilename;

    public void execute() throws MojoExecutionException
    {
        CodacyClient client = new CodacyClient(apiToken);
        CommitService codacyService = new CommitService(client);

        logger.info("Uploading coverage data..." + codacyReportFilename);

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
