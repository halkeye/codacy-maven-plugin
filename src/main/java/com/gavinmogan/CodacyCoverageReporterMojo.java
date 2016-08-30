package com.gavinmogan;

import com.codacy.api.CoverageReport;
import com.codacy.api.Language;
import com.codacy.api.client.CodacyClient;
import com.codacy.api.helpers.FileHelper;
import com.codacy.api.helpers.vcs.GitClient;
import com.codacy.api.service.CoverageServices;
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
    @Parameter( defaultValue = "${env.CI_COMMIT}", property = "commit", required = true )
    private String commit;

    /**
     * your project coverage file name
     */
    @Parameter( defaultValue="${project.basedir}/jacoco.exec", property = "coverageReport", required = true )
    private File coverageReport;


    @Parameter( defaultValue="${project.basedir}", property = "rootProjectDir", required = true )
    private File rootProjectDir;

    /**
     * the project path prefix
     */
    @Parameter( defaultValue="", property = "prefix", required = true )
    private String prefix;

    /**
     * the base URL for the Codacy API
     */
    @Parameter( defaultValue="https://api.codacy.com", property = "codacyApiBaseUrl", required = true )
    private String codacyApiBaseUrl;

    @Parameter( defaultValue="${project.basedir}/codacy-coverage.json", property = "codacyReportFilename", required = true )
    private File codacyReportFilename;

    private final ObjectMapper mapper = new ObjectMapper();


    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        if (Strings.isNullOrEmpty(commit)) {
            GitClient gitClient = new GitClient(rootProjectDir);
            commit = gitClient.latestCommitUuid().get();
        }
        final CodacyClient client = new CodacyClient(
                scala.Option.apply(codacyApiBaseUrl),
                scala.Option.apply(apiToken),
                scala.Option.apply(projectToken)
        );
        final CoverageServices coverageServices = new CoverageServices(client);

        getLog().debug("Project token: " + projectToken);

        getLog().info("Parsing coverage data... " + coverageReport);

        CoverageParserFactory.withCoverageReport(Language.Java(), rootProjectDir, coverageReport, new AbstractFunction1<CoverageReport, Object>() {
            public Object apply(CoverageReport report) {
                /*
                val transformations = Set(new PathPrefixer(config.prefix))
                val transformedReport = transformations.foldLeft(report) {
                    (report, transformation) => transformation.execute(report)
                }

                return transformedReport;
                */

                getLog().debug("Saving parsed report to " + codacyReportFilename);

                getLog().debug(report.toString());
                FileHelper.writeJsonToFile(codacyReportFilename, report, report.codacyCoverageReportFmt());

                getLog().info("Uploading coverage data...");

                if (!Strings.isNullOrEmpty(prefix)) {
                    final PathPrefixer pathPrefixer = new PathPrefixer(prefix);
                    report = pathPrefixer.execute(report);
                }

                CloseableHttpClient httpclient = HttpClients.createDefault();
                try {
                    HttpPost httppost = new HttpPost(codacyApiBaseUrl + "/2.0/coverage/" + commit + "/" + language.toLowerCase());
                    httppost.setHeader("api_token", apiToken);
                    httppost.setHeader("project_token", projectToken);
                    httppost.setHeader("Content-Type", "application/json");

                    final String json = StringUtils.join(FileUtils.readLines(codacyReportFilename, "UTF-8"), "");

                    StringEntity input = new StringEntity(json);
                    input.setContentType("application/json");
                    httppost.setEntity(input);

                    getLog().info("Executing request " + httppost.getRequestLine());

                    // Create a custom response handler
                    ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                        @Override
                        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                            int status = response.getStatusLine().getStatusCode();
                            if (status >= 200 && status < 300) {
                                HttpEntity entity = response.getEntity();
                                return entity != null ? EntityUtils.toString(entity) : null;
                            } else {
                                throw new ClientProtocolException("Unexpected response status: " + status);
                            }
                        }

                    };
                    String responseBody = httpclient.execute(httppost, responseHandler);
                    getLog().info("----------------------------------------");
                    getLog().info(responseBody);

                    getLog().info("Coverage data uploaded. Reason: " + responseBody);

                } catch (ClientProtocolException e) {
                    getLog().error("Failed to upload data.", e);
                    throw new RuntimeException("Failed to upload data. Reason: " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
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
        //final CoberturaParser reader = new CoberturaParser(Language.Java(), rootProjectDir, coverageReport);
        //final JacocoParser reader = new JacocoParser(Language.Java(), rootProjectDir, coverageReport);
        //final CoverageReport report = reader.generateReport();

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
