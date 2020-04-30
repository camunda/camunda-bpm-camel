/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.camel.spring;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:start-process-from-route-config.xml")
public class StartProcessFromRouteTest {

  MockEndpoint mockEndpoint;
  MockEndpoint processVariableEndpoint;

  @Autowired(required = true)
  CamelContext camelContext;

  @Autowired(required = true)
  RuntimeService runtimeService;

  @Autowired(required = true)
  HistoryService historyService;

  @Autowired(required = true)
  @Rule
  public ProcessEngineRule processEngineRule;

  @Before
  public void setUp() {
    mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:endpoint");
    mockEndpoint.reset();
    processVariableEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:processVariable");
    processVariableEndpoint.reset();
  }

  @Test
  @Deployment(resources = {"process/StartProcessFromRoute.bpmn20.xml"})
  public void doTest() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();

    String processInstanceId = (String) tpl.requestBody("direct:start", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(processInstanceId).isNotNull();
    System.out.println("Process instance ID: " + processInstanceId);

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(processInstanceId);

    // The body of the message comming out from the camunda-bpm:<process definition> endpoint is the process instance
    assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo(processInstanceId);
    
    // We should receive a hash map as the body of the message with a 'var1' key
    assertThat(processVariableEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=valueOfVar1}");
  }

  @Test
  @Deployment(resources = {"process/StartProcessFromRoute.bpmn20.xml"})
  public void doTestReturnVariable() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();

    String var1 = (String) tpl.requestBody("direct:startReturnVariable", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(var1).isNotNull();

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isNotNull();

    // The body of the message comming out from the camunda-bpm:<process definition> endpoint is the process instance
    assertThat(var1).isEqualTo("valueOfVar1");
    assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo(var1);
    
    // We should receive a hash map as the body of the message with a 'var1' key
    assertThat(processVariableEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=valueOfVar1}");
  }

  @Test
  @Deployment(resources = {"process/StartProcessFromRoute.bpmn20.xml"})
  public void doTestReturnVariables() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();

    Map<String, Object> vars = (Map<String, Object>) tpl.requestBody("direct:startReturnVariables", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(vars).isNotNull();

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isNotNull();

    // The body of the message comming out from the camunda-bpm:<process definition> endpoint is the process instance
    assertThat(vars).isNotNull();
    assertThat(vars.size()).isEqualTo(1);
    assertThat(vars.get("var1")).isEqualTo("valueOfVar1");
    assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).isEqualTo(vars);
    
    // We should receive a hash map as the body of the message with a 'var1' key
    assertThat(processVariableEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=valueOfVar1}");
  }

  @Test
  @Deployment(resources = {"process/StartProcessFromRoute.bpmn20.xml"})
  public void doTestReturnAllVariables() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();

    Map<String, Object> vars = (Map<String, Object>) tpl.requestBody("direct:startReturnAllVariables", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(vars).isNotNull();

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isNotNull();

    // The body of the message comming out from the camunda-bpm:<process definition> endpoint is the process instance
    assertThat(vars).isNotNull();
    assertThat(vars.size()).isEqualTo(1);
    assertThat(vars.get("var1")).isEqualTo("valueOfVar1");
    assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).isEqualTo(vars);
    
    // We should receive a hash map as the body of the message with a 'var1' key
    assertThat(processVariableEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=valueOfVar1}");
  }
}
