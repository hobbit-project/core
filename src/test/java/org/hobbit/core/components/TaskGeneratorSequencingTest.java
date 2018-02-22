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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hobbit.core.components.dummy.DummyEvalStoreReceiver;
import org.hobbit.core.components.dummy.DummySystemReceiver;
import org.hobbit.utils.TestUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the workflow like the {@link TaskGeneratorTest} but makes sure that the
 * task generator creates the tasks sequentially when the maximum amount of
 * messages processed in parallel is set to 1.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@RunWith(Parameterized.class)
public class TaskGeneratorSequencingTest extends TaskGeneratorTest {//extends AbstractTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskGeneratorSequencingTest.class);

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        // We use only one single data generator
        testConfigs.add(new Object[] { 1, 300, 10 });
        // We use only one single data generator with longer task processing
        // duration
        testConfigs.add(new Object[] { 1, 30, 1000 });
        // We use three data generators
        testConfigs.add(new Object[] { 3, 100, 10 });
        // We use three data generators with longer task processing duration
        testConfigs.add(new Object[] { 3, 10, 1000 });
        return testConfigs;
    }

    private List<String> taskIds = new ArrayList<String>();

    public TaskGeneratorSequencingTest(int numberOfGenerators, int numberOfMessages, long taskProcessingTime) {
        super(numberOfGenerators, numberOfMessages, 1, taskProcessingTime);
    }

    private AtomicInteger processedMessages = new AtomicInteger(0);

    @Override
    public void close() throws IOException {
        super.close();
    }

    public void checkResults(DummySystemReceiver system, DummyEvalStoreReceiver evalStore)  {
        super.checkResults(system, evalStore);
        // Make sure that all tasks have been processed sequentially, i.e.,
      // that the pairs of ids are equal
      for (int i = 0; i < taskIds.size(); i += 2) {
          Assert.assertEquals(taskIds.get(i), taskIds.get(i + 1));
      }
    }

    @Override
    protected void generateTask(byte[] data) throws Exception {
        String taskIdString = getNextTaskId();
        // Add the Id to the list of processed Ids
        taskIds.add(taskIdString);
        Thread.sleep(taskProcessingTime);
        long timestamp = System.currentTimeMillis();
        sendTaskToSystemAdapter(taskIdString, data);
        sentTasks.add(TestUtils.concat(taskIdString, data));

        sendTaskToEvalStorage(taskIdString, timestamp, data);
        expectedResponses.add(TestUtils.concat(taskIdString, data, timestamp));
        // Add the Id a second time
        taskIds.add(taskIdString);
        processedMessages.incrementAndGet();
    }

}
