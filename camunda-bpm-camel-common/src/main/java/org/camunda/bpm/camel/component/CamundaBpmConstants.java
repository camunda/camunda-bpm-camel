package org.camunda.bpm.camel.component;

/**
 * Common constants for the camunda BPM Apache Camel component
 */
public final class CamundaBpmConstants {

    public static final String CAMUNDA_BPM_CAMEL_URI_SCHEME = "camunda-bpm";

    public static final String EXCHANGE_HEADER_PROCESS_DEFINITION_KEY = "CamundaBpmProcessDefinitionKey";
    public static final String EXCHANGE_HEADER_PROCESS_DEFINITION_ID = "CamundaBpmProcessDefinitionId";
    public static final String EXCHANGE_HEADER_PROCESS_INSTANCE_ID = "CamundaBpmProcessInstanceId";
    public static final String EXCHANGE_HEADER_PROCESS_PRIO = "CamundaBpmProcessInstancePrio";
    public static final String EXCHANGE_HEADER_BUSINESS_KEY = "CamundaBpmBusinessKey";
    public static final String EXCHANGE_HEADER_CORRELATION_KEY = "CamundaBpmCorrelationKey";
    public static final String EXCHANGE_HEADER_CORRELATION_KEY_TYPE = "CamundaBpmCorrelationKeyType";
    public static final String EXCHANGE_HEADER_TASK = "CamundaBpmExternalTask";
    public static final String EXCHANGE_HEADER_RETRIESLEFT = "CamundaBpmExternalRetriesLeft";
    public static final String EXCHANGE_HEADER_ATTEMPTSSTARTED = "CamundaBpmExternalAttemptsStarted";
    public static final String EXCHANGE_HEADER_TASKID = "CamundaBpmExternalTaskId";
    public static final String EXCHANGE_RESPONSE_IGNORE = "CamundaBpmExternalTaskIgnore";

    /* Apache Camel URI parameters */
    public final static String PROCESS_DEFINITION_KEY_PARAMETER = "processDefinitionKey";
    public final static String TOPIC_PARAMETER = "topic";
    public final static String WORKERID_PARAMETER = "workerId";
    public final static String VARIABLESTOFETCH_PARAMETER = "variablesToFetch";
    public final static String DESERIALIZEVARIABLES_PARAMETER = "deserializeVariables";
    public final static boolean DESERIALIZEVARIABLES_DEFAULT = true;
    public final static String MAXTASKSPERPOLL_PARAMETER = "maxTasksPerPoll";
    public final static int MAXTASKSPERPOLL_DEFAULT = 5;
    public final static String ASYNC_PARAMETER = "async";
    public final static boolean ASYNC_DEFAULT = false;
    public final static String ONCOMPLETION_PARAMETER = "onCompletion";
    public final static boolean ONCOMPLETION_DEFAULT = false;
    public final static String LOCKDURATION_PARAMETER = "lockDuration";
    public final static long LOCKDURATION_DEFAULT = 60000;
    public final static String RETRIES_PARAMETER = "retries";
    public final static String RETRYTIMEOUT_PARAMETER = "retryTimeout";
    public final static long RETRYTIMEOUT_DEFAULT = 500;
    public final static String RETRYTIMEOUTS_PARAMETER = "retryTimeouts";
    public final static String MESSAGE_NAME_PARAMETER = "messageName";
    public final static String CORRELATION_KEY_NAME_PARAMETER = "correlationKeyName";
    public final static String ACTIVITY_ID_PARAMETER = "activityId";
    public final static String COPY_MESSAGE_PROPERTIES_PARAMETER = "copyProperties";
    public final static String COPY_MESSAGE_HEADERS_PARAMETER = "copyHeaders";
    public final static String COPY_MESSAGE_BODY_AS_PROCESS_VARIABLE_PARAMETER = "copyBodyAsVariable";
    public final static String COPY_PROCESS_VARIABLES_TO_OUT_BODY_PARAMETER = "copyVariablesToOutBody";

    private CamundaBpmConstants() {
    } // prevent instantiation of helper class

    public static String camundaBpmUri(String path) {
        return CAMUNDA_BPM_CAMEL_URI_SCHEME + ":" + path;
    }
}
