# NextGen Adapters

### What is this project about?

An springboot based application that extends/completes a self-service BI suite to wrtie to a myriad of destination types. 

Adapters project contains functionality related to providing hooks into communicating with destination(consumers) outside of NextGen environment.

It's main purpose is to poll and process functionality based on events created via Dispatcher and Context Processor.

This project exposes Spring context through available AdapterService interface that will contain the destination-specific communication hooks.

Destination-specific resources can be data bases or web services that reside outside of 
NextGen environment or any other resource where data flowing from NextGen environment should be persisted.

### Usage:

This project is a JAR packaged project and can invoke this JAR via CLI or via any Java ready environment.

Provide "-Dspring.profiles.active= " as part of the execution command.

Example:
```bash
    java -jar -Dspring.profiles.active=facets-dev adapters.jar
    java -jar -Dspring.profiles.active=facets-sys adapters.jar
    java -jar -Dspring.profiles.active=ptdm-dev adapters.jar
    java -jar -Dspring.profiles.active=ptdm-sys adapters.jar
```
Above examples show the "destination-environment" style profile name to be passed as the value of spring.profiles.active key in the command line.

##### Example explained:

```bash
    java -jar -Dspring.profiles.active=facets-dev adapters.jar
```
In above example, we are asking Adapters project to load all facets destination related context (including all resources applicable).
All Database related connectivities, web service endpoint connections should be configured within the project.
Once the Adapters application is started, only the beans specific to facets destination will become available at runtime.
We are controlling this through Spring based @Profile annotation.



### How to add adapter context for new destination ?

As mentioned briefly above, Adapters project is Spring boot application and utilizes
the Spring @Profile based approach to conditionally load beans at startup of the application.

To add new destination (example for Proclaim).


* Create new package name under like this:
```markdown
    package com.acme.ng.provider.adapters.destination.proclaim
```
    
* Create Proclaim specific adapter context and implement _com.acme.ng.provider.adapters.context.AdapterContext_ interface
```markdown 
com.acme.ng.provider.adapters.destination.proclaim.ProclaimAdapterContext
```  
* Create configurations for databases, web-services as needed under this destination package like this:
```markdown 
com.acme.ng.provider.adapater.destination.proclaim.db
com.acme.ng.provider.adapater.destination.proclaim.ws
``` 
* Annotate with @Profile on the Database specific beans and Web services specific beans like this
(add more environment as needed):
```markdown
    @Profile({"proclaim-dev", "proclaim-prod", "proclaim-sys", "proclaim-int"})
```    
* Add required databases related configuration, beans, repositories (if needed) like this:
``` markdown
    com.acme.nd.provider.adapaters.destination.proclaim.db
    |-------------------------------------------------- .mongo
    |-------------------------------------------------- .oracle
    |-------------------------------------------------- .db2
    |-------------------------------------------------- .postgresql    
```
* Add required web services related configuration, beans, endpoints etc (if needed) like this:
``` markdown
    com.acme.nd.provider.adapaters.destination.proclaim.ws
    |-------------------------------------------------- .rest
    |-------------------------------------------------- .soap    
* Annotate with @Profile on the Database specific beans and Web services specific beans like this as needed.
(add more environment as needed):
```
    @Profile({"proclaim-dev", "proclaim-prod", "proclaim-sys", "proclaim-int"})       
* Add profile based yml file (if needed) or any Java based profile approach 
(if there are no resources to be read from YAML or properties file) like this:
```markdown
    application-proclaim-dev.yml --> contains dev environment specific proclaim stuff.
    application-proclaim-sys.yml --> contains sys environment specific proclaim stuff.
    application-proclaim-pvs.yml --> contains pvs environment specific proclaim stuff.
    application-proclaim-prod.yml --> contains prod environment specific proclaim stuff.
    
    Additionally, we can still utilize the defaul YAML:
    application.yml to load any 'must-have' common stuff into the applicaiton context at start up.
```
```markdown
*** Note: If we would like to avoid @Profile annotation across beans within .destination package, 
then we can choose to perform following steps:
1. Add DestinationAdapterContext.class (the specific destination) in the @Import section within the AdapterApplication.java class.
2. Add @Profile only on DestinationAdapterContext.java.
3. Apply @ComponentScan on the DestinationAdapterContext which should exist at the root package of destination package.
```

* Add any nextgen common dependencies in POM as needed.

* Add common logic that can be utilized by all destination adapter contexts in package like this (these beans need to be *Profile* annotated as they should be available and visible to all profiles and all destinations). These beans/classes should contain only common logic and *not* destination specific beans/logic
```markdown 
    com.acme.ng.provider.adapters.common
    com.acme.ng.provider.adapters.common.service
    com.acme.ng.provider.adapters.common.service.impl
``` 
* Configure all proclaim specific beans within the _ProclaimAdapterContext_ that was created above and annotate them with Proclaim specific Profile annotations like this:
```markdown
    @Profile({"proclaim-dev", "proclaim-prod", "proclaim-sys", "proclaim-int"})
```        
* This context will be exposed via _com.acme.ng.provider.adapters.context.AdapterContext_ inteface 
and any caller/consumer of this interface will only be given the _ProclaimAdapterContext_
with all Proclaim environment specific hooks (such as databases, web services etc) to communicate
to do DB / web calls to read or persist data.

### Conventions and Standards for YAML file and properties:

This section explains on general standards to add destination specific properties. It may be good practice to 
use the following standard to add any new properties:

##### Application YAML naming convention: 
````markdown
application-{destination}-{env}.yml
````

##### Properties Naming convention:
###### Database:
```markdown
{destination}:
    mongo[0...1]:
        uri:
    oracle[0...1]:
        username:
        password:
    postgresql[0...1]:
        username:
        password:
    anyDBName[0...1]:
        username:
        password:
```
###### Web Service:
```markdown
{destination}:
    rest[0...1]:
        endpoint:
        username:
        password:
    soap[0...1]:
        endpoint:
        username:
        password:
```
### JUnit Tests:

1. Annotate with ``` @ActiveProfiles("proclaim-test")``` on the Test class (in this case, to test Proclaim specific adapter context logic). 
2. Write Test classes specific to destination for destination specific tests.
