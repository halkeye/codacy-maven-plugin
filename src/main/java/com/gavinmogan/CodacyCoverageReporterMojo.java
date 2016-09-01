package com.gavinmogan;

import com.codacy.api.CoverageReport;
import com.codacy.api.Language;
import com.codacy.api.helpers.FileHelper;
import com.codacy.api.helpers.vcs.GitClient;
import com.codacy.parsers.CoverageParserFactory;
import com.codacy.transformation.PathPrefixer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import scala.runtime.AbstractFunction1;

import java.io.File;
import java.io.IOException;

@Mojo( name = "coverage", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class CodacyCoverageReporterMojo extends AbstractMojo
{

    /**
     * your project API token
     */
    @Parameter( defaultValue="${env.CODACY_PROJECT_TOKEN}", property = "projectToken", required = true )
    private String projectToken;

    /**
     * your API token
     */
    @Parameter( defaultValue="${env.CODACY_API_TOKEN}", property = "apiToken", required = true )
    private String apiToken;

    /**
     * your project language
     */
    @Parameter( defaultValue="JAVA", property = "language", required = true )
    private String language;

    /**
     * your project coverage file name
     */
    @Parameter( defaultValue = "${env.CI_COMMIT}", property = "commit", required = false )
    private String commit;

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
    @Parameter( defaultValue="", property = "prefix", required = false )
    private String prefix;

    /**
     * the base URL for the Codacy API
     */
    @Parameter( defaultValue="https://api.codacy.com", property = "codacyApiBaseUrl", required = true )
    private String codacyApiBaseUrl;

    @Parameter( defaultValue="${project.basedir}/codacy-coverage.json", property = "codacyReportFilename", required = true )
    private File codacyReportFilename;

    private final ObjectMapper mapper = new ObjectMapper();

    class RuntimeMojoFailureException extends RuntimeException {
        public RuntimeMojoFailureException(String message, Throwable e) {
            super(message, e);
        }
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        /* TODO
         * loop through ${project.basedir}/target/jacoco/jacoco.xml and ${project.basedir}/target/cobertura/coverage.xml
         * to see which one is available by default?
         */
        if (Strings.isNullOrEmpty(commit)) {
            GitClient gitClient = new GitClient(rootProjectDir);
            commit = gitClient.latestCommitUuid().get();
        }

        getLog().debug("Project token: " + projectToken);

        getLog().info("Parsing coverage data... " + coverageReport);

        try {
            CoverageParserFactory.withCoverageReport(Language.Java(), rootProjectDir, coverageReport, new AbstractFunction1<CoverageReport, Object>() {
                public Object apply(CoverageReport report) {
                    if (!Strings.isNullOrEmpty(prefix)) {
                        final PathPrefixer pathPrefixer = new PathPrefixer(prefix);
                        report = pathPrefixer.execute(report);
                    }

                    getLog().debug("Saving parsed report to " + codacyReportFilename);
                    FileHelper.writeJsonToFile(codacyReportFilename, report, CoverageReport.codacyCoverageReportFmt());

                    getLog().info("Uploading coverage data...");
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    try {
                        String responseBody = postReport(httpclient);
                        getLog().info("Coverage data uploaded");
                    } catch (IOException e) {
                        getLog().error("Failed to upload data.", e);
                        throw new RuntimeMojoFailureException(e.getMessage(), e);
                    } finally {
                        try {
                            httpclient.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return null;
                }
            });
        } catch (RuntimeMojoFailureException e) {
            throw new MojoFailureException("Failed to upload data. Reason: " + e.getMessage(), e);
        }
    }

    private String postReport(CloseableHttpClient httpclient) throws IOException {
        HttpPost httppost = new HttpPost(codacyApiBaseUrl + "/2.0/coverage/" + commit + "/" + language.toLowerCase());
        httppost.setHeader("api_token", apiToken);
        httppost.setHeader("project_token", projectToken);
        httppost.setHeader("Content-Type", "application/json");

        final String json = StringUtils.join(FileUtils.readLines(codacyReportFilename, "UTF-8"), "");

        StringEntity input = new StringEntity(json);
        input.setContentType("application/json");
        httppost.setEntity(input);

        // Create a custom response handler
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(final HttpResponse response) throws IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }

        };
        return httpclient.execute(httppost, responseHandler);
    }

    public void setCoverageReport(File coverageReport) {
        this.coverageReport = coverageReport;
    }

    public void setProjectToken(String projectToken) {
        this.projectToken = projectToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setRootProjectDir(File rootProjectDir) {
        this.rootProjectDir = rootProjectDir;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCodacyApiBaseUrl(String codacyApiBaseUrl) {
        this.codacyApiBaseUrl = codacyApiBaseUrl;
    }

    public void setCodacyReportFilename(File codacyReportFilename) {
        this.codacyReportFilename = codacyReportFilename;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }
}
