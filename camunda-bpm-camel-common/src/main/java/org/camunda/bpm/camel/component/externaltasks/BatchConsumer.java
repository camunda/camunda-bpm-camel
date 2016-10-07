package org.camunda.bpm.camel.component.externaltasks;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_DEFINITION_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_DEFINITION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_PRIO;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_TASK;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledBatchPollingConsumer;
import org.apache.camel.util.CastUtils;
import org.camunda.bpm.camel.common.CamundaUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;

public class BatchConsumer extends ScheduledBatchPollingConsumer {

    private final CamundaBpmEndpoint camundaEndpoint;

    // parameters
    private int timeout;
    private final long lockDuration;
    private final String topic;
    private final boolean completeTask;
    private final List<String> variablesToFetch;
    private final String workerId;

    private final TaskProcessor taskProcessor;

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor, final int retries,
            final long retryTimeout, final long[] retryTimeouts, final long lockDuration, final String topic,
            final boolean completeTask, final List<String> variablesToFetch, final String workerId) {

        super(endpoint, processor);

        this.camundaEndpoint = endpoint;
        this.lockDuration = lockDuration;
        this.topic = topic;
        this.completeTask = completeTask;
        this.variablesToFetch = variablesToFetch;
        this.workerId = workerId;

        this.taskProcessor = new TaskProcessor(endpoint,
                topic,
                retries,
                retryTimeout,
                retryTimeouts,
                completeTask,
                true,
                workerId);

    }

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor,
            final ScheduledExecutorService executor, final int retries, final long retryTimeout,
            final long[] retryTimeouts, final long lockDuration, final String topic, final boolean completeTask,
            final List<String> variablesToFetch, final String workerId) {

        super(endpoint, processor, executor);

        this.camundaEndpoint = endpoint;
        this.lockDuration = lockDuration;
        this.topic = topic;
        this.completeTask = completeTask;
        this.variablesToFetch = variablesToFetch;
        this.workerId = workerId;

        this.taskProcessor = new TaskProcessor(endpoint,
                topic,
                retries,
                retryTimeout,
                retryTimeouts,
                completeTask,
                true,
                workerId);

    }

    @Override
    public int processBatch(Queue<Object> exchanges) throws Exception {

        int total = exchanges.size();
        int answer = total;

        for (int index = 0; index < total && isBatchAllowed(); index++) {
            // only loop if we are started (allowed to run)
            // use poll to remove the head so it does not consume memory even
            // after we have processed it
            Exchange exchange = (Exchange) exchanges.poll();
            // add current index and total as properties
            exchange.setProperty(Exchange.BATCH_INDEX, index);
            exchange.setProperty(Exchange.BATCH_SIZE, total);
            exchange.setProperty(Exchange.BATCH_COMPLETE, index == total - 1);

            // update pending number of exchanges
            pendingExchanges = total - index - 1;

            boolean started = processExchange(exchange);

            // if we did not start processing then decrement the counter
            if (!started) {
                answer--;
            }
        }

        // drain any in progress files as we are done with this batch
        removeExcessiveInProgressTasks(CastUtils.cast((Queue<?>) exchanges, Exchange.class), 0);

        return answer;

    }

    private boolean processExchange(final Exchange exchange) throws Exception {

        taskProcessor.process(exchange);
        getProcessor().process(exchange);

        return true;

    }

    /**
     * Drain any in progress files as we are done with this batch
     *
     * @param exchanges
     *            the exchanges
     * @param limit
     *            the limit
     */
    protected void removeExcessiveInProgressTasks(Queue<Exchange> exchanges, int limit) {

        while (exchanges.size() > limit) {
            // must remove last
            Exchange exchange = exchanges.poll();
            releaseTask(exchange);
        }

    }

    private void releaseTask(final Exchange exchange) {

        exchange.setProperty(Exchange.ROLLBACK_ONLY, Boolean.TRUE);
        taskProcessor.internalProcessing(exchange);

    }

    private ExternalTaskService getExternalTaskService() {

        return camundaEndpoint.getProcessEngine().getExternalTaskService();

    }

    protected int poll() throws Exception {

        int messagesPolled = 0;

        PriorityQueue<Exchange> exchanges = new PriorityQueue<Exchange>(new Comparator<Exchange>() {
            @Override
            public int compare(Exchange o1, Exchange o2) {
                Long prio1 = (Long) o1.getProperty(EXCHANGE_HEADER_PROCESS_PRIO, 0);
                Long prio2 = (Long) o2.getProperty(EXCHANGE_HEADER_PROCESS_PRIO, 0);
                return prio1.compareTo(prio2);
            }
        });

        if (isPollAllowed()) {

            final List<LockedExternalTask> tasks = CamundaUtils.retryIfOptimisticLockingException(
                    new Callable<List<LockedExternalTask>>() {
                        @Override
                        public List<LockedExternalTask> call() {
                            return getExternalTaskService().fetchAndLock(maxMessagesPerPoll, workerId, true).topic(
                                    topic, lockDuration).variables(variablesToFetch).execute();
                        }
                    });

            messagesPolled = tasks.size();

            for (final LockedExternalTask task : tasks) {

                final ExchangePattern pattern = completeTask ? ExchangePattern.InOut : ExchangePattern.InOnly;
                Exchange exchange = getEndpoint().createExchange(pattern);

                exchange.setFromEndpoint(getEndpoint());
                exchange.setExchangeId(task.getWorkerId() + "/" + task.getId());
                exchange.setProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID, task.getProcessInstanceId());
                exchange.setProperty(EXCHANGE_HEADER_PROCESS_DEFINITION_KEY, task.getProcessDefinitionKey());
                exchange.setProperty(EXCHANGE_HEADER_PROCESS_DEFINITION_ID, task.getProcessDefinitionId());
                exchange.setProperty(EXCHANGE_HEADER_PROCESS_PRIO, task.getPriority());

                final Message in = exchange.getIn();
                in.setHeader(EXCHANGE_HEADER_TASK, task);
                in.setBody(task.getVariables());

                exchanges.add(exchange);

            }

        }

        processBatch(CastUtils.cast(exchanges));

        return messagesPolled;

    }

    public int getTimeout() {

        return timeout;

    }

    /**
     * Sets a timeout to use with {@link PollingConsumer}. <br/>
     * <br/>
     * Use <tt>timeout < 0</tt> for {@link PollingConsumer#receive()}. <br/>
     * Use <tt>timeout == 0</tt> for {@link PollingConsumer#receiveNoWait()}.
     * <br/>
     * Use <tt>timeout > 0</tt> for {@link PollingConsumer#receive(long)}}.
     * <br/>
     * The default timeout value is <tt>0</tt>
     *
     * @param timeout
     *            the timeout value
     */
    public void setTimeout(int timeout) {

        this.timeout = timeout;

    }

}
