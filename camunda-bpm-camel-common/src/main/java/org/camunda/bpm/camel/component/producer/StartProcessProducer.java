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
package org.camunda.bpm.camel.component.producer;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.camunda.bpm.camel.common.ExchangeUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

/**
 * Starts a process instance given a process definition key.
 * <p>
 * Example: camunda-bpm://start?processDefinitionKey=aProcessDefinitionKey
 *
 * @author Ryan Johnston (@rjfsu)
 * @author Tijs Rademakers (@tijsrademakers)
 * @author Rafael Cordones (@rafacm)
 * @author Bernd Ruecker
 */
public class StartProcessProducer extends CamundaBpmProducer {

  private final String processDefinitionKey;
  
  private HistoryService historyService;

  public StartProcessProducer(CamundaBpmEndpoint endpoint, Map<String, Object> parameters) {
    super(endpoint, parameters);

    try {
      historyService = super.processEngine.getHistoryService();
    } catch (Exception e) {
      historyService = null; // It may be OK to have no history service
    }
    
    if (parameters.containsKey(PROCESS_DEFINITION_KEY_PARAMETER)) {
      this.processDefinitionKey = (String) parameters.get(PROCESS_DEFINITION_KEY_PARAMETER);
    } else {
        processDefinitionKey = null;
      // throw new IllegalArgumentException("You need to pass the '" + PROCESS_DEFINITION_KEY_PARAMETER + "' parameter! Parameters received: " + parameters);
    }
  }

  @Override
  public void close() {
    log.info("Closing StartProcessProducer");
    this.stop();
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Map<String, Object> processVariables = new HashMap<String, Object>();
    if (parameters.containsKey(COPY_MESSAGE_PROPERTIES_PARAMETER)) {
      processVariables.putAll(exchange.getProperties());
    }
    if (parameters.containsKey(COPY_MESSAGE_HEADERS_PARAMETER)) {
      processVariables.putAll(exchange.getIn().getHeaders());
    }

    processVariables.putAll(ExchangeUtils.prepareVariables(exchange, parameters));

    /*
     * If the exchange contains the CAMUNDA_BPM_BUSINESS_KEY then we pass it to the engine
     */
    String processDefinitionKey = this.processDefinitionKey != null ? this.processDefinitionKey : exchange.getIn().getHeader(EXCHANGE_HEADER_PROCESS_DEFINITION_KEY, String.class);

    ProcessInstance instance = null;
    if (exchange.getProperties().containsKey(EXCHANGE_HEADER_BUSINESS_KEY)) {
      instance = runtimeService.startProcessInstanceByKey(processDefinitionKey,
                                                          exchange.getProperty(EXCHANGE_HEADER_BUSINESS_KEY, String.class),
                                                          processVariables);
      exchange.setProperty(EXCHANGE_HEADER_BUSINESS_KEY, instance.getBusinessKey());
    } else {
      instance = runtimeService.startProcessInstanceByKey(processDefinitionKey, processVariables);
    }

    exchange.setProperty(EXCHANGE_HEADER_PROCESS_DEFINITION_ID, instance.getProcessDefinitionId());
    exchange.setProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID, instance.getProcessInstanceId());
    
    setOutBody(exchange, instance);
    
  }
  
  private void setOutBody(final Exchange exchange, final ProcessInstance instance) {
    if (parameters.containsKey(COPY_PROCESS_VARIABLES_TO_OUT_BODY_PARAMETER)) {
      if (historyService == null) {
          throw new RuntimeCamelException("Fetching process instance variables is only supported "
                                          + "for processes returning in a wait state because the "
                                          + "Camunda history service is needed but not available!");
      }
      
      final String variableName = parameters.get(COPY_PROCESS_VARIABLES_TO_OUT_BODY_PARAMETER).toString();
      
      if (variableName.equals("*")) {
        if (historyService != null) {
          final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                      .processInstanceIdIn(instance.getProcessInstanceId())
                      .list();
          exchange.getOut().setBody(convertHistoricVariablesToMap(variables));
        } else {
          final List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
                  .processInstanceIdIn(instance.getProcessInstanceId())
                  .list();
          exchange.getOut().setBody(convertToRuntimeVariablesMap(variables));
        }
      } else if (variableName.contains(",")) {
        final HashMap<String, Object> variables = new HashMap<String, Object>();
        for (final String variableNameItem : variableName.split(",")) {
          if (historyService != null) {
            final HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceIdIn(instance.getProcessInstanceId())
                        .variableName(variableNameItem)
                        .singleResult();
            if (variable != null) {
              variables.put(variable.getName(), variable.getValue());
            }
          } else {
            final VariableInstance variable = runtimeService.createVariableInstanceQuery()
                    .processInstanceIdIn(instance.getProcessInstanceId())
                    .variableName(variableNameItem)
                    .singleResult();
            if (variable != null) {
              variables.put(variable.getName(), variable.getValue());
            }
          }
        }
        exchange.getOut().setBody(variables);
      } else if (!variableName.trim().isEmpty()) {
        if (historyService != null) {
          final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                      .processInstanceIdIn(instance.getProcessInstanceId())
                      .variableName(variableName)
                      .list();
          if (variables.isEmpty()) {
            exchange.getOut().setBody(null);
          } else {
            exchange.getOut().setBody(variables.iterator().next().getValue());
          }
        } else {
          final List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
                      .processInstanceIdIn(instance.getProcessInstanceId())
                      .variableName(variableName)
                      .list();
          if (variables.isEmpty()) {
            exchange.getOut().setBody(null);
          } else {
            exchange.getOut().setBody(variables.iterator().next().getValue());
          }
        }
      } else {
        exchange.getOut().setBody(instance.getProcessInstanceId());
      }
    } else {
      exchange.getOut().setBody(instance.getProcessInstanceId());
    }
  }
  
  private HashMap<String, Object> convertHistoricVariablesToMap(final List<HistoricVariableInstance> variables) {
    final HashMap<String,Object> result = new HashMap<String, Object>();
    final Iterator<HistoricVariableInstance> iterator = variables.iterator();
    while (iterator.hasNext()) {
      final HistoricVariableInstance variable = iterator.next();
      result.put(variable.getName(), variable.getValue());
    }
    return result;
  }

  private HashMap<String, Object> convertToRuntimeVariablesMap(final List<VariableInstance> variables) {
    final HashMap<String,Object> result = new HashMap<String, Object>();
    final Iterator<VariableInstance> iterator = variables.iterator();
    while (iterator.hasNext()) {
      final VariableInstance variable = iterator.next();
      result.put(variable.getName(), variable.getValue());
    }
    return result;
  }
  
}