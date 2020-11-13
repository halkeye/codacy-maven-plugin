package com.gavinmogan;

import com.codacy.api.CoverageReport;
import com.codacy.api.helpers.vcs.GitClient;
import com.codacy.parsers.implementation.CoberturaParser;
import com.codacy.parsers.implementation.JacocoParser;
import com.codacy.transformation.PathPrefixer;
import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import play.api.libs.json.Json;
import scala.util.Either;

@Mojo(name = "coverage", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class CodacyCoverageReporterMojo extends AbstractMojo {
    private static Class[] parsers = new Class[]{CoberturaParser.class, JacocoParser.class};

    /**
     * set to {@code true} if you want coverage upload, otherwise {@code false}.
     */
    @Parameter(defaultValue = "false", property = "codacy.skip", required = true)
    private boolean skip;

    /**
     * your project API token
     */
    @Parameter(defaultValue = "${env.CODACY_PROJECT_TOKEN}", property = "projectToken", required = true)
    private String projectToken;

    /**
     * your API token
     */
    @Parameter(defaultValue = "${env.CODACY_API_TOKEN}", property = "apiToken", required = true)
    private String apiToken;

    /**
     * your project language
     */
    @Parameter(defaultValue = "Java", property = "language", required = true)
    private String language;

    /**
     * your project commit revision for which to upload the coverage data
     * (default: the latest commit of the current git branch)
     */
    @Parameter(defaultValue = "${env.CI_COMMIT}", property = "commit", required = false)
    private String commit;

    /**
     * your project coverage file name
     */
    @Parameter(defaultValue = "", property = "coverageReportFile", required = true)
    private File coverageReportFile;


    @Parameter(defaultValue = "${project.basedir}", property = "rootProjectDir", required = true)
    private File rootProjectDir;

    /**
     * the project path prefix
     */
    @Parameter(defaultValue = "", property = "prefix", required = false)
    private String prefix;

    /**
     * the base URL for the Codacy API
     */
    @Parameter(defaultValue = "${env.CODACY_API_BASE_URL}", property = "codacyApiBaseUrl", required = false)
    private String codacyApiBaseUrl;

    /**
     * should reporter fail on missing report file
     */
    @Parameter(defaultValue = "true", property = "failOnMissingReportFile", required = false)
    private boolean failOnMissingReportFile;

    /**
     * is coverage partial
     */
    @Parameter(defaultValue = "false", property = "partial", required = false)
    private boolean partial;

    /**
     * should reporter trust self signed certificates when uploading the coverage data
     */
    @Parameter(defaultValue = "false", property = "codacy.trustSelfSignedCerts", required = false)
    private boolean trustSelfSignedCerts;

    @Override
    public void execute() throws MojoFailureException {
        if(!skip) {
            if (!coverageReportFile.exists()) {
                if (failOnMissingReportFile) {
                    throw new MojoFailureException("Report file does not exist");
                } else {
                    getLog().warn("Report file does not exist");
                    return;
                }
            }

            /* TODO
             * loop through ${project.basedir}/target/jacoco/jacoco.xml and ${project.basedir}/target/cobertura/coverage.xml
             * to see which one is available by default?
             */
            if (Strings.isNullOrEmpty(commit)) {
                GitClient gitClient = new GitClient(rootProjectDir);
                commit = gitClient.latestCommitUuid().get();
            }

            if (Strings.isNullOrEmpty(codacyApiBaseUrl)) {
                codacyApiBaseUrl = "https://api.codacy.com";
            }

            getLog().debug("Project token: " + projectToken);

            getLog().info("Parsing coverage data... " + coverageReportFile);

            CoverageReport report = processReport(rootProjectDir, coverageReportFile);

            if (!Strings.isNullOrEmpty(prefix)) {
                final PathPrefixer pathPrefixer = new PathPrefixer(prefix);
                report = pathPrefixer.execute(report);
            }

            try {
                getLog().info("Uploading coverage data...");
                postReport(report);
                getLog().info("Coverage data uploaded");
            } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
                getLog().error("Failed to upload coverage data.", e);
                throw new MojoFailureException("Failed to upload coverage data. Reason: " + e.getMessage(), e);
            }
        }
    }

    public String postReport(CoverageReport report) throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        try (CloseableHttpClient httpclient = createHttpClient()) {
            URIBuilder builder = new URIBuilder(codacyApiBaseUrl + "/2.0/coverage/" + commit + "/" + language.toLowerCase());
            builder.setParameter("partial", String.valueOf(partial));

            HttpPost httppost = new HttpPost(builder.build());
            httppost.setHeader("api_token", apiToken);
            httppost.setHeader("project_token", projectToken);
            httppost.setHeader("Content-Type", "application/json");

            final String json = Json.toJson(report, CoverageReport.coverageReportWrites()).toString();

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
    }

    private CloseableHttpClient createHttpClient() throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (trustSelfSignedCerts) {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
            return HttpClients.custom().setSSLSocketFactory(
                sslsf).build();
        }

        HttpClientBuilder builder = HttpClients.custom();
        return builder.build();
    }

    private SSLContext createSSLContext() throws IOException {
        try {
            return SSLContextBuilder.create()
                .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
                .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new IOException("Could not create SSL context: " + e.getMessage(), e);
        }
    }

    /**
     * Given a report file, find the parser that works for this
     *
     * @param rootProject Where is the project located
     * @param reportFile  Where is the coverage report file
     * @return Completed report, or null if nothing matches
     */
    public static CoverageReport processReport(File rootProject, File reportFile) {

        for (Class clazz : parsers) {
            try {
                Either<String, CoverageReport> result = (Either<String, CoverageReport>)
                    clazz.getMethod("parse", File.class, File.class).invoke(null, rootProject, reportFile);

                if (result.isRight()) {
                    return result.right().get();
                }
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return null;
    }

    public void setCoverageReportFile(File coverageReportFile) {
        this.coverageReportFile = coverageReportFile;
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

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public void setFailOnMissingReportFile(boolean failOnMissingReportFile) {
        this.failOnMissingReportFile = failOnMissingReportFile;
    }

    public void setTrustSelfSignedCerts(boolean trustSelfSignedCerts) {
        this.trustSelfSignedCerts = trustSelfSignedCerts;
    }
}
