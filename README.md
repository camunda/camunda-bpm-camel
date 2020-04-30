This [community extension](https://docs.camunda.org/manual/latest/introduction/extensions/) focuses on bringing two great Open Source frameworks closer together, the [camunda BPM platform](http://camunda.org) and [Apache Camel](http://camel.apache.org).

![Use Cases supported by camunda BPM Camel Component][2]

See [example project 'camel use cases'](https://github.com/camunda-consulting/code/tree/master/one-time-examples/camel-use-cases) for code for all of the use cases shown in the above model.



## camunda BPM --> Apache Camel

### Calling a Camel Endpoint (Service)

Use the following expression in a ServiceTask to send all the process instance variables as a map to Camel endpoint:

```
${camel.sendTo('<camel endpoint>')}
```
Hint: You will also get variables which were set but contain a null value.

Alternatively you can specify which process instance variables you want to send to Camel with:

```
${camel.sendTo('<camel endpoint>', '<comma-separated list of process variables>')}
```
Hint: missing or null value variables will cause to throw an IllegalArgumentException. You can append a question mark to each name of a variable which is allowed to be missing or null (e.g. 'mayBeNullVar?,mustNotBeNullVar').
 
Additionally you can specify a correlationKey to send to Camel. It can be used to correlate a response message. The route for the response must contain a parameter correlationKeyName with the name of the process variable which is used for correlation:

```
${camel.sendTo('<camel endpoint>', '<comma-separated list of process variables>', 'correlationKey')}
```

The properties `CamundaBpmProcessInstanceId`, `CamundaBpmBusinessKey` (if available) and `CamundaBpmCorrelationKey` (if set) will be available to any downstream processors in the Camel route.

## Apache Camel --> camunda BPM
The following use cases are supported by the camunda BPM Camel component (see [Camel Components](http://camel.apache.org/components.html)).

### `camunda-bpm://start` Start a process instance

A direct consumer to start process instances.

The following URI parameters are supported:

Parameter | Description
--- | ---
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process to start an instance of
`copyBodyAsVariable` | name of the process variable to which the body of the Camel should be copied. Default is `camelBody`
`copyHeaders` | whether the [Camel message headers](http://camel.apache.org/header.html) should be copied as process variables
`copyProperties` | whether the [Camel exchange](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/Exchange.html) properties should be copied as process variables
`copyVariablesToOutBody` | After the first wait state or the end of the process is reached the current values of the requested variables as a map is set to the out body instead of the process instance id. Supported formats are '*' (for all variables), 'var1,var2' (for certain variables) or 'var1' (for a single variable's value instead of a map). Note that getting variables from a process already ended requires Camunda's history service and you need to set the [history level](https://docs.camunda.org/manual/7.7/user-guide/process-engine/history/#choose-a-history-level) to at least `AUDIT`.

If the Camel message body is a map, then all the keys will be copied as process variables of the started instance.

If the property `CamundaBpmBusinessKey` is available on the incoming message then it will be associated with the started process instance and can be later followed to look it up.

The properties `CamundaBpmProcessInstanceId`, `CamundaBpmProcessDefinitionId` and `CamundaBpmBusinessKey` are available to the downstream processors in the Camel route as Camel exchange properties.

Example: `camunda-bpm://start?processDefinitionKey=startProcessFromRoute&copyBodyAsVariable=var1`

Starts a process instance of the process definition `startProcessFromRoute` with the body of the message as a map with process variable `var1` as a key.



### `camunda-bpm://message` Send a message to the process engine

A direct consumer to send a message to the process engine. This can either:
* trigger the start of a new process instance, see [Start Message Event](http://docs.camunda.org/latest/api-references/bpmn20/#events-message-events)
* send a message to a waiting process instances. The process instance might either wait in a [ReceiveTask](http://docs.camunda.org/latest/api-references/bpmn20/#tasks-receive-task) or an [Intermediate Message Event](http://docs.camunda.org/latest/api-references/bpmn20/#events-message-events).

The following URI parameters are supported:

Parameter | Description
--- | ---
`messageName`| the name of the message in the BPMN 2.0 XML (mandatory if you correlate to a Intermediate Message Event or a ReceiveTask with a message reference)
`activityId`| the id of the ReceiveTask in the BPMN 2.0 XML (mandatory if the process instance waits in a ReceiveTask without a message reference - considered as deprecated)
`correlationKeyName`| the name of a process variable to which the property `CamundaBpmCorrelationKey` will be correlated
`copyBodyAsVariable` | name of the process variable to which the body of the Camel should be copied. Default is `camelBody`.
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process definition this operation is related to. In case of working without a message this can help to make correlation unique, it is always an optional parameter.

Note that either one of the properties `CamundaBpmProcessInstanceId`, `CamundaBpmBusinessKey` or `CamundaBpmCorrelationKey` need to be present in the message if it is correlated to a waiting process instance. Usage of `CamundaBpmCorrelationKey` and / or `CamundaBpmBusinessKey` is preferred.

### `camunda-bpm://poll-externalTasks` Consuming external tasks

With version 7.4.0 Camunda [introduced external tasks](https://blog.camunda.org/post/2015/11/camunda-bpm-740-released/). With version 7.5.0 [further improvements](https://blog.camunda.org/post/2016/05/camunda-bpm-750-released/) were added.

Since version 0.5 of camunda-bpm-camel it is possible to consume external tasks by Camel endpoints. There are advantages by doing so:
* You don't have to place Camel instructions into your BPMN which is another level of technical decoupling.
* External tasks are not processed by the thread processing the workflow and therefore they do not block the origin thread which may be part of Camunda's worker thread-pool.
* If you have asynchronous communication external tasks in combination with Camel are a great deal to split the service task into three transactions: The first is about the workflow currently processing (context: Camunda); the second is for sending the request (context: Camel) and the third for processing the response (context: Camel).
* External tasks may be used as some sort of queue - especially if the service does some outbound network communication. In some installations it might be sufficient to use this aspect instead of using an ESB.

Example: `camunda-bpm:poll-externalTasks?topic=topic1`

The following URI parameters are supported:

Parameter | Description
--- | ---
`topic` | (mandatory) The name of the topic as configured for the external task in BPMN. The endpoint will only consume tasks of this topic.
`maxTasksPerPoll` | (optional, default: 5) The endpoint is a polling consumer. This parameter defines the number of tasks fetched by each poll. Further configuration concerning scheduling of polling can be found at the description of [Camel's scheduler component](http://camel.apache.org/scheduler.html).
`workerId` | (optional, default: the endpoint URI *) The workerId used to poll Camunda for external tasks. Polled tasks are locked for that workerId. So if the task is not completed by the endpoint itself (see parameter `async`), then the workerId used to complete the task must be the same used to poll the task.
`lockDuration` | (optional, default: 60s) Once a task is fetched it is locked for other consumers. This parameter defines how long it is locked if there is no further interaction.
`retries` | The number of times the external task will be tried to resolve before an incident is raised.
`retryTimeout` | (optional, default: 500ms) The timeout between subsequent retries.
`retryTimeouts` | (optional, no default) A comma separated list of timeouts used for retries. This is useful when calling an external services which might be down or simply busy. Using `retryTimeouts=5s,30s,5m` in combination with will `retryTimeout=30m&retries=5` will use the retry timeout sequence 5, 30 seconds, 5, 30 and 30 minutes.
`variablesToFetch` | (optional, no default) A list of process instance variables which will be fetched for every external task consumed.
`deserializeVariables` | (optional, default: true for Camunda >= 7.6.0 otherwise false) Controls whether serialized variables should be deserialized on fetching them among external tasks.
`async` | (optional, default: false) Usually you want to complete the task once the exchange is processed. If you want to do asynchronous communication then you can use `async=true` to not complete the tasks consumed by the endpoint. Therefore it is up to your responsibility to complete the task once you processed the asynchronous response. See also `camunda-bpm://async-externalTask`.

*) The endpoint URI is not the URI remarked in your configuration file. It is a URI generated by Camel but
it is very similar to your URI. An example: `camunda-bpm:poll-externalTasks?topic=topic1&amp;maxTasksPerPoll=5`
-> `camunda-bpm://poll-externalTasks?maxTasksPerPoll=5&topic=topic1` (parameters are sorted).

The exchange produced by the endpoint got several properties:
* `CamundaBpmProcessInstanceId`
* `CamundaBpmProcessDefinitionId`
* `CamundaBpmProcessDefinitionKey`
* `CamundaBpmProcessInstancePrio`

Additionally these in-headers are set:
* `CamundaBpmExternalTask` contains the entire [LockedExternalTask](https://docs.camunda.org/javadoc/camunda-bpm-platform/7.5/org/camunda/bpm/engine/externaltask/LockedExternalTask.html) object
* `CamundaBpmExternalAttemptsStarted` contains how many times the task was processed but failed
* `CamundaBpmExternalRetriesLeft` contains how many times the task will be processed any more if this execution fails
* the in-body is a map containing the process instance variables requested (Map< String, Object >)

If the reply-body contains a map (Map< String, Object >) this map is treated as a list of process instance variables and therefore used to update the process. If the reply-body is a string it is used as a BPMN error code to signal an error to the process. If processing the exchange fails (e.g. an exception is caught) then the exception's message is used to mark the task as failed (which might cause further retries or an incident if the number of retries elapsed). Additionally exceptions can be annotated by `@org.camunda.bpm.camel.component.externaltasks.SetExternalTaskRetries` to control how the current exception effects the retry counter. 

Hint: Processing of the route is not done in parallel even if the route uses a AsyncProcessor. This is Camel's default behavior of a polling Consumer! If you want to process the external tasks in parallel you have to define your own scheduler:

Example: `camunda-bpm:poll-externalTasks?topic=topic1&scheduler=#topic1Scheduler`
 
A scheduler is found by lookup in Camel's registry (what kind of registry is used depends on your environment, typical registries are CDI BeanManager and JNDI). This is an example of CDI producer building a scheduler which processes two external tasks in parallel: 

```@Produces
@Named("topic1Scheduler")
public ScheduledPollConsumerScheduler produceTopic1Scheduler() {
    final DefaultScheduledPollConsumerScheduler scheduledPollConsumerScheduler = new DefaultScheduledPollConsumerScheduler();
    scheduledPollConsumerScheduler.setConcurrentTasks(2);
    return scheduledPollConsumerScheduler;
}
```

### `camunda-bpm://async-externalTask` Processing outstanding external tasks

By using `camunda-bpm://poll-externalTasks`' parameter `async` at value `true` it is up to your responsibility to complete the task once your processing is done. A typical situation is when you want to process the response of asynchronous communication. In this situation you may use this processor endpoint (see [Camel docs](http://camel.apache.org/processor.html#Processor-TurningyourprocessorintoafullComponent))
to complete the external task. The endpoint also supports throwing an incident or raising an BPM error as described for `camunda-bpm://poll-externalTasks`.

For using it the incoming message of the Camel route must include a parameter `CamundaBpmExternalTaskId` which contains the id of the external task.

Example: `camunda-bpm:async-externalTask?topic=topic1`

Typical usage:
* `<from uri="direct:bar" /><to uri="camunda-bpm:async-externalTask" />`
* `<from uri="direct:bar" /><to uri="camunda-bpm:async-externalTask?onCompletion=true" /><to uri="some-other-endpoint" />`

Parameter | Description
--- | ---
`topic` | (option) The name of the topic. If given the external task is checked for belonging to that topic.
`workerId` | (optional) The id of the worker. If given the external task is checked for belonging to that worker id.
`retries` | The number of times the external task will be tried to resolve before an incident is raised if processing the task by the route fails.
`retryTimeout` | (optional, default: 500ms) The timeout between subsequent retries.
`retryTimeouts` | (optional, no default) A comma separated list of timeouts used for retries. This is useful when calling an external services which might be down or simply busy. Using `retryTimeouts=5s,30s,5m` in combination with will `retryTimeout=30m&retries=5` will use the retry timeout sequence 5, 30 seconds, 5, 30 and 30 minutes.
`onCompletion` | (optional, default: false) Setting this parameter to `true` the external task is completed after the entire Camel route was processed. This allows you to define this endpoint direct behind the route's start endpoint. The main advantage of doing so is that it catches thrown exceptions and treats them as failure. If this endpoint is at the end of the Camel route any exception throw by preceding processors not being treated in that way. Using this parameter set to `true` forces exactly the same behavior as done by the endpoint `camunda-bpm://poll-externalTasks` and it's parameter `async` set to `false`. Additionally if exchanges out body is equal to the string `CamundaBpmExternalTaskIgnore` then the current action will be ignored and won't effect the external task in any way. 

These in-headers are set to be used subsequent endpoints:
* `CamundaBpmExternalAttemptsStarted` contains how many times the task was processed but failed
* `CamundaBpmExternalRetriesLeft` contains how many times the task will be processed any more if this execution fails

The Exception `org.camunda.bpm.camel.component.externaltasks.NoSuchExternalTaskException` (derived from RuntimeCamelException) might be thrown on processing an external task. The reason for this circumstance might be a situation in a BPM process in which the task implemented by the external task (e.g. ServiceTask) was cancelled. Tasks are cancelled due to interrupting events (e.g. timer event).

# Examples
Check the existing integration tests for guidance on how to use the current supported features in your projects: [Spring](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring) or [CDI](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/). To run the CDI integration tests do `mvn -DskipITs=false`.

Further there exist two example projects showing camunda-bpm-camel in Action (on JBoss AS 7 though):
* [camel use cases](https://github.com/camunda/camunda-consulting/tree/master/showcases/camel-use-cases)
* [Bank Account Opening Process using Camel](https://github.com/camunda/camunda-consulting/tree/master/showcases/bank-account-opening-camel)



# Using it in your project
This project is at the moment in incubation phase. This means that changes are bound to happen that will break backwards compatibility. Be warned!

## Environment

* <= 0.3: < JDK 1.7
* 0.4 - 0.5: JDK 1.7
* >= 0.6: JDK 1.8

Please also check Camel requirements: http://camel.apache.org/what-are-the-dependencies.html (e.g. Camel 2.14 onwards requires JDK 1.7 or better).


## Maven coordinates

You might have to add the camunda BPM repository in your project's `pom.xml`:

```
<repositories>
	<repository>
		<id>camunda-bpm-nexus</id>
		<name>camunda-bpm-nexus</name>
		<url>https://app.camunda.com/nexus/content/groups/public</url>
	</repository>
</repositories>
```

Choose a dependency depending on your target environment:

### Spring
```
<dependency>
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-spring</artifactId>
    <version>0.7</version>
</dependency>
```
In your Spring configuration you need to configure the `CamelService` like this:

```
  <bean id="camel" class="org.camunda.bpm.camel.spring.impl.CamelServiceImpl">
    <property name="processEngine" ref="processEngine"/>
    <property name="camelContext" ref="camelContext"/>
  </bean>
```

The Spring bean id `camel` will be then available to expressions used in ServiceTasks to send data to Camel.

### CDI

```
<dependency>
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-cdi</artifactId>
    <version>0.7</version>
</dependency>
```

The CDI configuration needs a bit more work - especially for bootstrapping Camel. The easiest is to do this in a Singleton Startup EJB (see [Example: CamelBootStrap.java](https://github.com/camunda/camunda-bpm-examples/blob/master/camel-use-cases/src/main/java/org/camunda/demo/camel/CamelBootStrap.java)):

```
@Singleton
@Startup
public class CamelBootStrap {

  @Inject
  private CdiCamelContext cdiCamelContext;

  @Inject
  private ProcessEngine processEngine;

  @Inject
  private MyCamelRouteBuilder routeBuilder; // your own route declaration

  @PostConstruct
  public void init() throws Exception {
    CamundaBpmComponent component = new CamundaBpmComponent(processEngine);
    component.setCamelContext(cdiCamelContext);
    cdiCamelContext.addComponent("camunda-bpm", component);
    cdiCamelContext.addRoutes(routeBuilder);
    cdiCamelContext.start();
  }

  @PreDestroy
  public void shutDown() throws Exception {
    cdiCamelContext.stop();
  }
}
```

Best read [Apache Camel's CDI documentation](http://camel.apache.org/cdi.html) and have a look at the CDI integration tests [here](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/) for guidance.


### Blueprint
```
<dependency>
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-blueprint</artifactId>
    <version>0.7</version>
</dependency>
```
The OSGi Framework is used to retrieve the `ProcessEngine` and a `DefaultCamelContext` therefore the bean definition of the `CamelServiceImpl` is obsolete.

The camunda-bpm-osgi project is used with the blueprint-wrapper `context.xml`. The `BlueprintELResolver` was extended by the `CamelBlueprintELResolver`. You need to replace the class of the ‘blueprintELResolver’ bean in the context.xml:

```
...
<bean id="blueprintELResolver" class=" org.camunda.bpm.camel.blueprint.CamelBlueprintELResolver" />
...
```


# Feedback and further development

This project is part of the [camunda BPM incubation space](https://github.com/camunda/camunda-bpm-incubation). Feedback, pull requests, ... you name it... are very welcome! Meet us on the [Camunda BPM Forum](forum.camunda.org/).


# Credits

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and the following people have contributed to its further development in the context of camunda BPM: [contributors](https://github.com/camunda/camunda-bpm-camel/graphs/contributors).

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.

[1]: http://rafael.cordones.me/assets/camunda-bpm-camel.png
[2]: https://raw.github.com/camunda/camunda-bpm-camel/master/use-cases.png
