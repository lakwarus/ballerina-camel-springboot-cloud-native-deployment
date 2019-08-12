# Cloud Native Deployment - Ballerina vs SpringBoot

With the emergence of microservice architecture, software architects and developers adapt microservice development for fast innovation. Smaller teams, agile software development life cycles, freedom to use heterogeneous technologies, early feedback cycles are the key drivers for success stories. 

To get all these benefits, devOps have to play a major role. They have to tune their deployment engine to roll out frequent releases without disrupting the end users. Cloud native deployment play a key role. The Importance of this aspect, cloud native deployment should not be an afterthought. This article mainly focus on how Camel+SpringBoot and Ballerina provide cloud native deployment support.

Docker and Kubernetes are playing a major role in cloud native deployment. Docker is a tool designed to make it easier to create, deploy, and run applications by using containers. Containers allow a developer to package up an application with all of the parts it needs, such as libraries and other dependencies, and ship it all out as one package. Kubernetes (K8s) is an open-source system for automating deployment, scaling, and management of containerized applications.

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


