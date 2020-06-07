/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.core.components;

import org.hobbit.core.components.dummy.DummyComponentExecutor;
import java.util.Random;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.MessageProperties;
import org.hobbit.core.rabbit.RabbitMQUtils;
import java.io.IOException;
import org.hobbit.core.Commands;
import org.hobbit.core.components.dummy.AbstractDummyPlatformController;
import org.hobbit.core.data.StartCommandData;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.hobbit.core.components.dummy.DummyCommandReceivingComponent;
import org.hobbit.core.Constants;
import org.hobbit.core.TestConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Stopwatch;
import static org.junit.Assert.*;


public class ContainerCreationNoCorrelationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerCreationNoCorrelationTest.class);

    private DummyPlatformController platformController = null;

    private static final String HOBBIT_SESSION_ID = "123";

    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private AbstractCommandReceivingComponent component;

    @Rule
    public Stopwatch stopwatch = new Stopwatch() {};

    @Before
    public void setUp() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, TestConstants.RABBIT_HOST);
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");
        environmentVariables.set(Constants.IS_RABBIT_MQ_ENABLED,"true");

        platformController = new DummyPlatformController(HOBBIT_SESSION_ID);
        DummyComponentExecutor platformExecutor = new DummyComponentExecutor(platformController);
        Thread platformThread = new Thread(platformExecutor);
        platformThread.start();
        platformController.waitForControllerBeingReady();

        component = new DummyCommandReceivingComponent();
        component.init();
    }

    @After
    public void tearDown() throws Exception {
        component.close();
        platformController.terminate();
    }

    @Test(timeout = 10000)
    public void test() throws Exception {
        Future<String> container1 = component.createContainerAsync("hello-world", null, new String[]{"ID=1"}, null);
        Future<String> container2 = component.createContainerAsync("hello-world", null, new String[]{"ID=2"}, null);
        String containerId1 = container1.get();
        String containerId2 = container2.get();
        System.out.println("Container ID : "+ containerId1);
        assertEquals("IDs of asynchronously created containers", "1;2", containerId1 + ";" + containerId2);
    }

    protected static class DummyPlatformController extends AbstractDummyPlatformController {
        public DummyPlatformController(String sessionId) {
            super(false);
            addCommandHeaderId(sessionId);
        }

        public void receiveCommand(byte command, byte[] data, String sessionId, AMQP.BasicProperties props) {
            if (command == Commands.DOCKER_CONTAINER_START) {
                String[] envVars = gson.fromJson(RabbitMQUtils.readString(data), StartCommandData.class).getEnvironmentVariables();
                String containerId = Stream.of(envVars).filter(kv -> kv.startsWith("ID=")).findAny().get().split("=", 2)[1];

                try {
                    AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
                    propsBuilder.deliveryMode(2);
                    AMQP.BasicProperties replyProps = propsBuilder.build();

//                    cmdChannel.basicPublish("", props.getReplyTo(), replyProps,
//                            RabbitMQUtils.writeString(containerId));
                } catch (Exception e/*IOException e*/) {
                    LOGGER.error("Exception in receiveCommand", e);
                }
            }
        }
    }
}
