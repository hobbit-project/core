package org.hobbit.core.components.dummy;

import org.hobbit.core.components.AbstractTaskGenerator;
import org.junit.Ignore;

@Ignore
public class DummyTaskGenerator extends AbstractTaskGenerator {

    @Override
    protected void generateTask(byte[] data) throws Exception {
        String taskId = getNextTaskId();
        sendTaskToSystemAdapter(taskId, data);
    }

}
