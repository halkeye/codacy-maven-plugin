[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gavinmogan/codacy-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gavinmogan/codacy-maven-plugin/)
# codacy-maven-plugin

Create and upload coverage report to https://codacy.com

## Commandline Usage

`mvn com.gavinmogan:codacy-maven-plugin:coverage -DcoverageReportFile=target/site/jacoco/jacoco.xml -DprojectToken=blah -DapiToken=blah`

where: 

* *coverageReportFile* is either a Jacoco or Cobertura file
* *projectToken* is your project token
* *apiToken* is your api token


**Enterprise**

To send coverage in the enterprise version you should:
```
export CODACY_API_BASE_URL=<Codacy_instance_URL>:16006
```

## POM Usage
```xml
<profiles>
      <profile>
          <id>codecoverage</id>
          <activation>
              <property><name>env.TRAVIS</name></property>
          </activation>
          <build>
              <plugins>
                  <plugin>
                      <groupId>com.gavinmogan</groupId>
                      <artifactId>codacy-maven-plugin</artifactId>
                      <version>1.0.3</version>
                      <configuration>
                          <apiToken>${env.CODACY_API_TOKEN}</apiToken>
                          <projectToken>${env.CODACY_PROJECT_TOKEN}</projectToken>
                          <coverageReportFile>${project.reporting.outputDirectory}/jacoco.xml</coverageReportFile>
                          <commit>${env.TRAVIS_COMMIT}</commit>
                          <codacyApiBaseUrl>https://api.codacy.com</codacyApiBaseUrl>
                          <failOnMissingReportFile>false</failOnMissingReportFile>
                      </configuration>
                      <executions>
                          <execution>
                              <id>post-test</id>
                              <phase>post-integration-test</phase>
                              <goals>
                                  <goal>coverage</goal>
                              </goals>
                          </execution>
                      </executions>
                  </plugin>
              </plugins>
          </build>
      </profile>
  </profiles>
  ```

## License

MIT

## Contributing

I'm open to any and all forms of contribution. Documentation improvements, issues, pull requests, patches, test cases, etc.
