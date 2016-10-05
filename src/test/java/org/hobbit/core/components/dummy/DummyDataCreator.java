package org.hobbit.core.components.dummy;

import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummyDataCreator extends AbstractDataGenerator {

    private int dataSize;

    public DummyDataCreator(int dataSize) {
        this.dataSize = dataSize;
    }

    @Override
    protected void generateData() throws Exception {
        byte data[];
        for (int i = 0; i < dataSize; ++i) {
            data = RabbitMQUtils.writeString(Integer.toString(i));
            sendDataToSystemAdapter(data);
            sendDataToTaskGenerator(data);
        }
    }

}
