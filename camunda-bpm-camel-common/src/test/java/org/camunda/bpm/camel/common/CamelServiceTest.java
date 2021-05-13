package org.camunda.bpm.camel.common;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_BUSINESS_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_CORRELATION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.*;

import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.context.BpmnExecutionContext;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class CamelServiceTest extends BaseCamelTest {

  protected CamelServiceCommonImpl service;
  protected ProducerTemplate producerTemplate;
  protected ExecutionEntity execution;

  @Before
  public void setupService() {
    service = new CamelServiceCommonImpl() {
      @Override
      public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
      }

      @Override
      public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
      }
    };
    service.setProcessEngine(processEngine);
    CamelContext camelContext = mock(ExtendedCamelContext.class);
    service.setCamelContext(camelContext);

    producerTemplate = mock(ProducerTemplate.class);
    when(camelContext.createProducerTemplate()).thenReturn(producerTemplate);

    BpmnExecutionContext executionContext = mock(BpmnExecutionContext.class);
    execution = mock(ExecutionEntity.class);

    when(executionContext.getExecution()).thenReturn(execution);
    when(execution.getProcessInstanceId()).thenReturn("theProcessInstanceId");
    when(execution.getBusinessKey()).thenReturn("theBusinessKey");
    when(execution.getVariable(anyString())).thenReturn("theVariable");

    PowerMockito.mockStatic(Context.class);
    PowerMockito.when(Context.getBpmnExecutionContext()).thenReturn(
        executionContext);
  }

  @Test
  public void testSendToEndpoint() throws Exception {
    Message message = mock(Message.class);
    Exchange send = new DefaultExchange(mock(CamelContext.class));
    send.setIn(message);
    send.setOut(null);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
        send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
        .forClass(Exchange.class);

    service.sendTo("what/ever");

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());
    verify(execution).getVariableNames();

    assertThat(exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_BUSINESS_KEY))
        .isEqualTo("theBusinessKey");
    assertThat(
        exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_CORRELATION_KEY))
        .isNull();
    assertThat(
        exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID))
        .isEqualTo("theProcessInstanceId");
  }

  @Test
  public void testSendToEndpointReturningOutMessage() throws Exception {
    Message message = mock(Message.class);
    Exchange send = new DefaultExchange(mock(CamelContext.class));
    send.setIn(null);
    send.setOut(message);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
            send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
            .forClass(Exchange.class);

    service.sendTo("what/ever");

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());
    verify(execution).getVariableNames();

    assertThat(exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_BUSINESS_KEY))
            .isEqualTo("theBusinessKey");
    assertThat(
            exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_CORRELATION_KEY))
            .isNull();
    assertThat(
            exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID))
            .isEqualTo("theProcessInstanceId");
  }

  @Test
  public void testSendToEndpointWithNoVariables() throws Exception {
    Exchange send = mock(Exchange.class);
    Message message = mock(Message.class);
    when(send.getOut()).thenReturn(message);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
        send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
        .forClass(Exchange.class);

    service.sendTo("what/ever", "");

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());
    verify(execution, never()).getVariableNames();
  }

  @Test
  public void testSendToEndpointWithOneVariable() throws Exception {
    Exchange send = mock(Exchange.class);
    Message message = mock(Message.class);
    when(send.getOut()).thenReturn(message);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
        send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
        .forClass(Exchange.class);

    service.sendTo("what/ever", "varName");

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());
    verify(execution, never()).getVariableNames();
    verify(execution).getVariable("varName");
  }

  @Test
  public void testSendToEndpointWithAlleVariables() throws Exception {
    Exchange send = mock(Exchange.class);
    Message message = mock(Message.class);
    when(send.getOut()).thenReturn(message);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
        send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
        .forClass(Exchange.class);

    service.sendTo("what/ever", null);

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());
    verify(execution).getVariableNames();
  }

  @Test
  public void testSendToEndpointWithCorrelation() throws Exception {
    Exchange send = mock(Exchange.class);
    Message message = mock(Message.class);
    when(send.getOut()).thenReturn(message);
    when(producerTemplate.send(anyString(), any(Exchange.class))).thenReturn(
        send);

    ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor
        .forClass(Exchange.class);

    service.sendTo("what/ever", null, "theCorrelationKey");

    verify(producerTemplate).send(anyString(), exchangeCaptor.capture());

    assertThat(
        exchangeCaptor.getValue().getProperty(EXCHANGE_HEADER_CORRELATION_KEY))
        .isEqualTo("theCorrelationKey");
  }
}