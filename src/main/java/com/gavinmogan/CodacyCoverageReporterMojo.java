package com.gavinmogan;

import com.codacy.api.CoverageReport;
import com.codacy.api.Language;
import com.codacy.api.client.CodacyClient;
import com.codacy.api.client.RequestResponse;
import com.codacy.api.client.RequestSuccess;
import com.codacy.api.helpers.FileHelper;
import com.codacy.api.service.CoverageServices;
import com.codacy.parsers.implementation.CoberturaParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.URL;

@Mojo( name = "coverage", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class CodacyCoverageReporterMojo extends AbstractMojo
{

    /**
     * your project API token
     */
    @Parameter( defaultValue="${env.CODACY_PROJECT_TOKEN}", property = "projectToken", required = true )
    private String projectToken;

    /**
     * your project API token
     */
    @Parameter( defaultValue="${env.CODACY_PROJECT_TOKEN}", property = "apiToken", required = true )
    private String apiToken;

    /**
     * your project language
     */
    @Parameter( defaultValue="java", property = "language", required = true )
    private String language;

    /**
     * your project coverage file name
     */
    @Parameter( property = "currentCommitUUID", required = true )
    private String currentCommitUUID ;

    /**
     * your project coverage file name
     */
    @Parameter( defaultValue="", property = "coverageReport", required = true )
    private File coverageReport;


    @Parameter( defaultValue="${project.basedir}", property = "rootProjectDir", required = true )
    private File rootProjectDir;

    /**
     * the project path prefix
     */
    @Parameter( defaultValue="", property = "prefix", required = true )
    private URL prefix;

    /**
     * the base URL for the Codacy API
     */
    @Parameter( defaultValue="https://api.codacy.com", property = "codacyApiBaseUrl", required = true )
    private String codacyApiBaseUrl;

    @Parameter( defaultValue="${project.basedir}/codacy-coverage.json", property = "codacyReportFilename", required = true )
    private File codacyReportFilename;

    public File getCodacyApiBaseUrl() { return codacyReportFilename; }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        CodacyClient client = new CodacyClient(
                scala.Option.apply(codacyApiBaseUrl),
                scala.Option.apply(apiToken),
                scala.Option.apply(projectToken)
        );
        CoverageServices coverageServices = new CoverageServices(client);

        getLog().debug("Project token: " + projectToken);

        getLog().info("Parsing coverage data...");

        final CoberturaParser reader = new CoberturaParser(Language.Java(), rootProjectDir, coverageReport);
        final CoverageReport report = reader.generateReport();

        FileHelper.writeJsonToFile(codacyReportFilename, report, report.codacyCoverageReportFmt());

        getLog().info("Uploading coverage data...");

        final RequestResponse<RequestSuccess> requestResponse = coverageServices.sendReport(currentCommitUUID, Language.Java(), report);
        if (requestResponse.hasError()) {
            getLog().error("Failed to upload data. Reason: " + requestResponse.message());
            throw new MojoFailureException("Failed to upload data. Reason: " + requestResponse.message());
        }
        getLog().info("Coverage data uploaded. Reason: " + requestResponse.message());
    }
}
