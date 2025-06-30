package io.siddhi.langserver;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.event.Event;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.query.api.definition.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LSEventStreamService implements EventStreamService {
    private static final Logger log = LoggerFactory.getLogger(LSEventStreamService.class);

    public List<String> getStreamNames(String siddhiAppName) {
        SiddhiAppRuntime siddhiAppRuntime = LSOperationContext.INSTANCE.getSiddhiAppRuntime(siddhiAppName);
        return new ArrayList<>(siddhiAppRuntime.getStreamDefinitionMap().keySet());
    }

    public List<Attribute> getStreamAttributes(String siddhiAppName, String streamName) {
        SiddhiAppRuntime siddhiAppRuntime = LSOperationContext.INSTANCE.getSiddhiAppRuntime(siddhiAppName);
        if (siddhiAppRuntime.getStreamDefinitionMap().containsKey(streamName)) {
            return (siddhiAppRuntime.getStreamDefinitionMap().get(streamName)).getAttributeList();
        } else {
            log.error("Siddhi App '{}' does not contain stream '{}'.", siddhiAppName, streamName);
            return Collections.emptyList();
        }
    }

    public void pushEvent(String siddhiAppName, String streamName, Event event) {
        SiddhiAppRuntime siddhiAppRuntime = LSOperationContext.INSTANCE.getSiddhiAppRuntime(siddhiAppName);
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(streamName);

        try {
            inputHandler.send(event);
        } catch (InterruptedException e) {
            log.error("Error when pushing events to Siddhi engine ", e);
        }
    }
}
