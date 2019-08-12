# Cloud Native Deployment - Ballerina vs Camel+SpringBoot

With the emergence of microservice architecture, software architects and developers adapt microservice development for fast innovation. Smaller teams, agile software development life cycles, freedom to use heterogeneous technologies, early feedback cycles are the key drivers for success stories. 

To get all these benefits, devOps have to play a major role. They have to tune their deployment engine to roll out frequent releases without disrupting the end users. Cloud native deployment play a key role. The Importance of this aspect, cloud native deployment should not be an afterthought. This article mainly focus on how Camel+SpringBoot and Ballerina provide cloud native deployment support.

[Docker](https://www.docker.com) and [Kubernetes](https://kubernetes.io/) are playing a major role in cloud native deployment. Docker is a tool designed to make it easier to create, deploy, and run applications by using containers. Containers allow a developer to package up an application with all of the parts it needs, such as libraries and other dependencies, and ship it all out as one package. Kubernetes (K8s) is an open-source system for automating deployment, scaling, and management of containerized applications.

## What we’ll build
We will use the same two applications we have implemented in [RESTFul microservice] (https://github.com/lakwarus/ballerina-camel-springboot-restful-microservice) and extend to deploy in Kubernetes.

## Apache Camel+SpringBoot

### Prerequisites
- JDK 1.8+
- Maven 3.2+
- Your favorite IDE, I have used Spring Tool Suite (STS)
- Docker
- Kubernetes

### Setting up the project
Please refer https://github.com/lakwarus/ballerina-camel-springboot-restful-microservice

### Implementation

Please refer https://github.com/lakwarus/ballerina-camel-springboot-restful-microservice

### Deployment

In this section we will look at how we can deploy our Camel+SpringBoot application into Kubernetes platform. 

#### Apache Camel-K

After Googling I was find out Apache Camel-K project. Apache Camel K is a lightweight integration platform, born on Kubernetes, with serverless superpowers. To run our application with Camel-K, first we need to install/setup our Kubernetes cluster with Camel-K operators. I have spent a significant amount of time on this stage. I have tried multiple Camel-K released versions, 1.0.0-M1, 0.3.4, 0.3.3, 0.3.2, 0.3.1, 0.2.1, but I could not make properly working setup in my Kubernetes cluster. I have tried both docker-for-mac (with k8s enabled) and minikube environments. Current releases on Camel-K are not stable enough and community support and guides are not enough me to complete the task.

#### Fabric8-Maven-Plugin

Research on further, I found out a mature, stable way to deploy our application on Kubernetes cluster. It is Fabric8 Maven plugin, a one-stop-shop for building and deploying Java applications for Docker, Kubernetes and OpenShift.  It provides a tight integration into maven and benefits from the build configuration already provided. It is capable of:
- Building Docker images
- Creating OpenShift and Kubernetes resources
- Deploy application on Kubernetes and OpenShift

Fabric8-maven-plugin has 3 various configuration styles:
- Zero Configuration for a quick ramp-up where opinionated defaults will be pre-selected.
- Inline Configuration within the plugin configuration in an XML syntax.
- External Configuration templates of the real deployment descriptors which are enriched by the plugin.

I tried out all 3 options but Inline XML based configuration is only partially implemented and is not recommended for use right now. 

First we need to configure our pom.xml with dependencies. Here I had to do a couple of changes compared to our original pom.xml we used in restful-microservice sample.

As a first step, I have introduced dependency management section below.

```xml
<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-dependencies</artifactId>
				<version>2.1.7.RELEASE</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<dependency>
				<groupId>org.apache.camel</groupId>
				<artifactId>camel-spring-boot-dependencies</artifactId>
				<version>2.24.0</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```
Then added fabric8 dependency in the dependency section additional to previous dependencies. 

```xml
		<dependency>
			<groupId>io.fabric8</groupId>
			<artifactId>kubernetes-assertions</artifactId>
			<version>2.3.7</version>
			<scope>test</scope>
		</dependency>
```

Alter build section by introducing `spring-boot:run` in default goal and add execution goals.

```
	<build>
		<defaultGoal>spring-boot:run</defaultGoal>

		<plugins>
			<plugin>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<groupId>org.springframework.boot</groupId>
				<executions>
					<execution>
						<goals>
							<goal>repackage</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

Finally added `k8s` profile section below.

```xml
	<profiles>
		<profile>
			<id>k8s</id>
			<build>
				<plugins>

					<plugin>
						<artifactId>fabric8-maven-plugin</artifactId>
						<groupId>io.fabric8</groupId>
						<version>4.2.0</version>
						<executions>
							<execution>
								<goals>
									<goal>resource</goal>
									<goal>build</goal>
								</goals>
							</execution>
						</executions>
					</plugin>

				</plugins>
			</build>
		</profile>
	</profiles>
```

Before create docker images with our application, we need to create an Uber.jar (fat jar)  with all dependencies and should be able to execute with `java -jar <application>.jar`. But our previous pom.xml is correctly configured and give some dependency issues. After spending sometimes, finally figured it out that we need to add another dependency in the dependency section. 


```xml
		<dependency>
			<groupId>javax.xml.bind</groupId>
			<artifactId>jaxb-api</artifactId>
		</dependency>
``` 
 Here you can find complete [pom.xml](https://github.com/lakwarus/ballerina-springboot-cloud-native-deployment/blob/master/springBoot/springboot-camel-restdsl/pom.xml).

Now lets do a few testing before going into Docker and Kubernetes deployment. Lets use `Zero Configuration` option first.

#### Zero Configuration 

First do a clean build.

```bash
$> mvn clean install
```

Next, create single executable jar.

```bash
$> mvn clean package
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Building springboot-camel-restdsl 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:3.1.0:clean (default-clean) @ springboot-camel-restdsl ---
[INFO] Deleting /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:resources (default-resources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] Copying 0 resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) @ springboot-camel-restdsl ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 4 source files to /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:testResources (default-testResources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:testCompile (default-testCompile) @ springboot-camel-restdsl ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.2:test (default-test) @ springboot-camel-restdsl ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-jar-plugin:3.1.2:jar (default-jar) @ springboot-camel-restdsl ---
[INFO] Building jar: /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/springboot-camel-restdsl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.1.7.RELEASE:repackage (repackage) @ springboot-camel-restdsl ---
[INFO] Replacing main artifact with repackaged archive
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.1.7.RELEASE:repackage (default) @ springboot-camel-restdsl ---
[INFO] Replacing main artifact with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 4.077 s
[INFO] Finished at: 2019-08-11T16:49:03-07:00
[INFO] Final Memory: 34M/120M
[INFO] ------------------------------------------------------------------------
```

It will create `springboot-camel-restdsl-0.0.1-SNAPSHOT.jar` in the target folder. Now you can use either `java - jar target/springboot-camel-restdsl-0.0.1-SNAPSHOT.jar` or `mvn spring-boot:run` to try out the application in your machine.

```bash
$> mvn spring-boot:run
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Building springboot-camel-restdsl 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] >>> spring-boot-maven-plugin:2.1.7.RELEASE:run (default-cli) > test-compile @ springboot-camel-restdsl >>>
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:resources (default-resources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] Copying 0 resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) @ springboot-camel-restdsl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:testResources (default-testResources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:testCompile (default-testCompile) @ springboot-camel-restdsl ---
[INFO] No sources to compile
[INFO] 
[INFO] <<< spring-boot-maven-plugin:2.1.7.RELEASE:run (default-cli) < test-compile @ springboot-camel-restdsl <<<
[INFO] 
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.1.7.RELEASE:run (default-cli) @ springboot-camel-restdsl ---

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.1.7.RELEASE)

2019-08-11 16:52:25.601  INFO 32626 --- [           main] .l.s.p.SpringbootCamelRestdslApplication : Starting SpringbootCamelRestdslApplication on Lakmals-MacBook-Pro.local with PID 32626 (/Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes started by lakmal in /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl)
2019-08-11 16:52:25.605  INFO 32626 --- [           main] .l.s.p.SpringbootCamelRestdslApplication : No active profile set, falling back to default profiles: default
2019-08-11 16:52:26.859  INFO 32626 --- [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.apache.camel.spring.boot.CamelAutoConfiguration' of type [org.apache.camel.spring.boot.CamelAutoConfiguration$$EnhancerBySpringCGLIB$$a0fd2241] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2019-08-11 16:52:27.150  INFO 32626 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2019-08-11 16:52:27.184  INFO 32626 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2019-08-11 16:52:27.184  INFO 32626 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.22]
2019-08-11 16:52:27.330  INFO 32626 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2019-08-11 16:52:27.331  INFO 32626 --- [           main] o.s.web.context.ContextLoader            : Root WebApplicationContext: initialization completed in 1664 ms
2019-08-11 16:52:27.624  INFO 32626 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2019-08-11 16:52:27.889  INFO 32626 --- [           main] o.a.c.i.converter.DefaultTypeConverter   : Type converters loaded (core: 195, classpath: 7)
2019-08-11 16:52:28.240  INFO 32626 --- [           main] o.a.camel.spring.boot.RoutesCollector    : Loading additional Camel XML routes from: classpath:camel/*.xml
2019-08-11 16:52:28.240  INFO 32626 --- [           main] o.a.camel.spring.boot.RoutesCollector    : Loading additional Camel XML rests from: classpath:camel-rest/*.xml
2019-08-11 16:52:28.248  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Apache Camel 2.24.0 (CamelContext: camel-1) is starting
2019-08-11 16:52:28.249  INFO 32626 --- [           main] o.a.c.m.ManagedManagementStrategy        : JMX is enabled
2019-08-11 16:52:28.538  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : StreamCaching is not in use. If using streams then its recommended to enable stream caching. See more details at http://camel.apache.org/stream-caching.html
2019-08-11 16:52:28.657  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.662  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.662  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.663  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.663  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.664  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.664  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.665  INFO 32626 --- [           main] o.a.c.c.jackson.JacksonDataFormat        : Found single ObjectMapper in Registry to use: com.fasterxml.jackson.databind.ObjectMapper@106a3ea1
2019-08-11 16:52:28.667  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route1 started and consuming from: direct://addOrder
2019-08-11 16:52:28.668  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route2 started and consuming from: direct://getOrder
2019-08-11 16:52:28.669  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route3 started and consuming from: direct://putOrder
2019-08-11 16:52:28.670  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route4 started and consuming from: direct://deleteOrder
2019-08-11 16:52:28.672  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route5 started and consuming from: servlet:/ordermgt/order?httpMethodRestrict=POST
2019-08-11 16:52:28.673  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route6 started and consuming from: servlet:/ordermgt/order/%7BorderId%7D?httpMethodRestrict=GET
2019-08-11 16:52:28.674  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route7 started and consuming from: servlet:/ordermgt/order/%7BorderId%7D?httpMethodRestrict=PUT
2019-08-11 16:52:28.675  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Route: route8 started and consuming from: servlet:/ordermgt/order/%7BorderId%7D?httpMethodRestrict=DELETE
2019-08-11 16:52:28.676  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Total 8 routes, of which 8 are started
2019-08-11 16:52:28.678  INFO 32626 --- [           main] o.a.camel.spring.SpringCamelContext      : Apache Camel 2.24.0 (CamelContext: camel-1) started in 0.428 seconds
2019-08-11 16:52:28.710  INFO 32626 --- [           main] o.a.c.c.s.CamelHttpTransportServlet      : Initialized CamelHttpTransportServlet[name=CamelServlet, contextPath=]
2019-08-11 16:52:28.713  INFO 32626 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2019-08-11 16:52:28.718  INFO 32626 --- [           main] .l.s.p.SpringbootCamelRestdslApplication : Started SpringbootCamelRestdslApplication in 3.457 seconds (JVM running for 7.282)
```

Now you can carried out application testing in your local machine.

Now let's deploy our application in Kubernetes cluster. Here I am used docker-for-mac (with k8s enabled) setup. 

You can use fabric8 maven build with several options.

- fabric8:resource 	-	Create Kubernetes and OpenShift resource descriptors
- fabric8:build		- 	Build Docker images
- fabric8:push		- 	Push Docker images to a registry
- fabric8:deploy	- 	Deploy Kubernetes / OpenShift resource objects to a cluster
- fabric8:watch		-	Watch for doing rebuilds and restarts

Lets use fabric8:deploy first. Here you I have skipped tests and use k8s profile which we configured previously.

```bash
$> mvn clean -DskipTests fabric8:deploy -Pk8s
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Building springboot-camel-restdsl 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:3.1.0:clean (default-clean) @ springboot-camel-restdsl ---
[INFO] Deleting /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target
[INFO] 
[INFO] >>> fabric8-maven-plugin:4.2.0:deploy (default-cli) > install @ springboot-camel-restdsl >>>
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:resources (default-resources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] Copying 0 resource
[INFO] 
[INFO] --- fabric8-maven-plugin:4.2.0:resource (default) @ springboot-camel-restdsl ---
[INFO] F8: Running generator spring-boot
[INFO] F8: spring-boot: Using Docker image fabric8/java-centos-openjdk8-jdk:1.5 as base / builder
[INFO] F8: fmp-controller: Adding a default Deployment
[INFO] F8: fmp-service: Adding a default service 'springboot-camel-restdsl' with ports [8080]
[INFO] F8: fmp-revision-history: Adding revision history limit to 2
[INFO] F8: validating /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/kubernetes/springboot-camel-restdsl-deployment.yml resource
[INFO] F8: validating /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/kubernetes/springboot-camel-restdsl-service.yml resource
[INFO] F8: fmp-controller: Adding a default DeploymentConfig
[INFO] F8: fmp-service: Adding a default service 'springboot-camel-restdsl' with ports [8080]
[INFO] F8: fmp-revision-history: Adding revision history limit to 2
[INFO] F8: validating /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/openshift/springboot-camel-restdsl-route.yml resource
[INFO] F8: validating /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/openshift/springboot-camel-restdsl-deploymentconfig.yml resource
[INFO] F8: validating /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/openshift/springboot-camel-restdsl-service.yml resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) @ springboot-camel-restdsl ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 4 source files to /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:testResources (default-testResources) @ springboot-camel-restdsl ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:testCompile (default-testCompile) @ springboot-camel-restdsl ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.2:test (default-test) @ springboot-camel-restdsl ---
[INFO] Tests are skipped.
[INFO] 
[INFO] --- maven-jar-plugin:3.1.2:jar (default-jar) @ springboot-camel-restdsl ---
[INFO] Building jar: /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/springboot-camel-restdsl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.1.7.RELEASE:repackage (repackage) @ springboot-camel-restdsl ---
[INFO] Replacing main artifact with repackaged archive
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.1.7.RELEASE:repackage (default) @ springboot-camel-restdsl ---
[INFO] Replacing main artifact with repackaged archive
[INFO] 
[INFO] --- fabric8-maven-plugin:4.2.0:build (default) @ springboot-camel-restdsl ---
[INFO] F8: Running in Kubernetes mode
[INFO] F8: Building Docker image in Kubernetes mode
[INFO] F8: Running generator spring-boot
[INFO] F8: spring-boot: Using Docker image fabric8/java-centos-openjdk8-jdk:1.5 as base / builder
[INFO] Copying files to /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/docker/sample/springboot-camel-restdsl/latest/build/maven
[INFO] Building tar: /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/docker/sample/springboot-camel-restdsl/latest/tmp/docker-build.tar
[INFO] F8: [sample/springboot-camel-restdsl:latest] "spring-boot": Created docker-build.tar in 442 milliseconds
[INFO] F8: [sample/springboot-camel-restdsl:latest] "spring-boot": Built image sha256:d8cec
[INFO] F8: [sample/springboot-camel-restdsl:latest] "spring-boot": Removed old image sha256:ff2e5
[INFO] F8: [sample/springboot-camel-restdsl:latest] "spring-boot": Tag with latest
[INFO] 
[INFO] --- maven-install-plugin:2.5.2:install (default-install) @ springboot-camel-restdsl ---
[INFO] Installing /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/springboot-camel-restdsl-0.0.1-SNAPSHOT.jar to /Users/lakmal/.m2/repository/com/lakwarus/sample/springboot-camel-restdsl/0.0.1-SNAPSHOT/springboot-camel-restdsl-0.0.1-SNAPSHOT.jar
[INFO] Installing /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/pom.xml to /Users/lakmal/.m2/repository/com/lakwarus/sample/springboot-camel-restdsl/0.0.1-SNAPSHOT/springboot-camel-restdsl-0.0.1-SNAPSHOT.pom
[INFO] Installing /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/kubernetes.yml to /Users/lakmal/.m2/repository/com/lakwarus/sample/springboot-camel-restdsl/0.0.1-SNAPSHOT/springboot-camel-restdsl-0.0.1-SNAPSHOT-kubernetes.yml
[INFO] Installing /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/openshift.yml to /Users/lakmal/.m2/repository/com/lakwarus/sample/springboot-camel-restdsl/0.0.1-SNAPSHOT/springboot-camel-restdsl-0.0.1-SNAPSHOT-openshift.yml
[INFO] 
[INFO] <<< fabric8-maven-plugin:4.2.0:deploy (default-cli) < install @ springboot-camel-restdsl <<<
[INFO] 
[INFO] 
[INFO] --- fabric8-maven-plugin:4.2.0:deploy (default-cli) @ springboot-camel-restdsl ---
[INFO] F8: Using Kubernetes at https://kubernetes.docker.internal:6443/ in namespace default with manifest /Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/kubernetes.yml 
[INFO] F8: Using namespace: default
[INFO] F8: Creating a Service from kubernetes.yml namespace default name springboot-camel-restdsl
[INFO] F8: Created Service: target/fabric8/applyJson/default/service-springboot-camel-restdsl.json
[INFO] F8: Creating a Deployment from kubernetes.yml namespace default name springboot-camel-restdsl
[INFO] F8: Created Deployment: target/fabric8/applyJson/default/deployment-springboot-camel-restdsl.json
[INFO] F8: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 14.852 s
[INFO] Finished at: 2019-08-11T17:03:20-07:00
[INFO] Final Memory: 54M/187M
[INFO] ------------------------------------------------------------------------
```

Lets list docker images;

```bash
$> docker images
REPOSITORY                                                       TAG                           IMAGE ID            CREATED             SIZE
sample/springboot-camel-restdsl                                  latest                        d8cec9ad7bb7        2 minutes ago       482MB

```
Our maven:deploy created a `sample/springboot-camel-restdsl:latest` docker image with our application. It has used artifact-id as the default name.

Lets run kubectl commands and see.

```bash
$>kubectl get all
NAME                                            READY   STATUS    RESTARTS   AGE
pod/springboot-camel-restdsl-7f77c84498-dnqlk   1/1     Running   0          8m9s

NAME                               TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
service/kubernetes                 ClusterIP   10.96.0.1      <none>        443/TCP    7h31m
service/springboot-camel-restdsl   ClusterIP   10.97.88.178   <none>        8080/TCP   8m9s

NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/springboot-camel-restdsl   1/1     1            1           8m9s

NAME                                                  DESIRED   CURRENT   READY   AGE
replicaset.apps/springboot-camel-restdsl-7f77c84498   1         1         1       8m9s
```

It has created `springboot-camel-restdsl ` kubernetes deployment and `springboot-camel-restdsl  ` kubernetes service with Cluster-IP type. 

Since I am running Kubernetes cluster in my mac machine, I can’t access the `springboot-camel-restdsl ` with Cluster-IP type. I need at least use nodePort type to access from my mach machine.

Also if you looked at maven output, Kubernetes artifacts use for above deployment can be found in `/Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/kubernetes/` location. 

Additional to Kubernetes artifacts, it has created Openshift artifacts in `/Users/lakmal/Documents/workspace-sts-3.9.9.RELEASE/springboot-camel-restdsl/target/classes/META-INF/fabric8/openshift/`. Compared to Kubernetes artifacts, it has created additional `springboot-camel-restdsl-route.yml` to configure OpenShift cluster to make route which will able to access as Zero Configuration.

#### External Configuration templates

Like you see, Zero Configuration is not enough to test our application in environment like docker-for-mac. Lets see how we can use `External Configuration templates` to modify some Kubernetes artifacts.

You can create different Kubernetes resource templates. First we need to create fabric8 folder inside the `src/main`. Here is the sample deployment template.

``` yaml
spec:
  replicas: 1
  template:
    spec:
      containers:
        - 
          resources:
            requests:
              cpu: "0.2"
              memory: 256Mi
            limits:
              cpu: "1.0"
              memory: 256Mi
```

Lets create kubernetes Service template with `NodePort` type to enable access outside the Kubernetes cluster. 

```yaml
spec:
  type: NodePort
```

Now let's build the project again.

```bash
$> mvn clean -DskipTests fabric8:deploy -Pk8s
```

```bash
$> kubectl get all
NAME                                            READY   STATUS    RESTARTS   AGE
pod/springboot-camel-restdsl-5f9ddf6d99-xblcg   1/1     Running   0          22s

NAME                               TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
service/kubernetes                 ClusterIP   10.96.0.1        <none>        443/TCP          8h
service/springboot-camel-restdsl   NodePort    10.105.164.129   <none>        8080:30648/TCP   22s

NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/springboot-camel-restdsl   1/1     1            1           22s

NAME                                                  DESIRED   CURRENT   READY   AGE
replicaset.apps/springboot-camel-restdsl-5f9ddf6d99   1         1         1       22s

```
Now you can access application by using nodePort.

```bash
$> curl -X POST -d '{ "id": "100500", "name": "XYZ", "description": "Sample order."}' "http://localhost:30648/ordermgt/order" -H "Content-Type:application/json" -v
* TCP_NODELAY set
* Connected to localhost (::1) port 30648 (#0)
> POST /ordermgt/order HTTP/1.1
> Host: localhost:30648
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type:application/json
> Content-Length: 64
> 
* upload completely sent off: 64 out of 64 bytes
< HTTP/1.1 201 
< Location: http://localhost:8080/ordermgt/order/100500
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Mon, 12 Aug 2019 01:27:51 GMT
< 
* Connection #0 to host localhost left intact
{"status":"Order Created!","orderId":"100500"}
```





 





## References

[1] https://github.com/fabric8io/fabric8-maven-plugin/ 

[2] https://github.com/fabric8-quickstarts/spring-boot-camel

