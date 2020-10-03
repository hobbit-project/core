package org.hobbit.core.components;

import com.rabbitmq.client.AMQP;

public abstract class AbstractPlatformController extends AbstractCommandReceivingComponent{
    
    public AbstractPlatformController() {
        super();
    }
    
    public AbstractPlatformController(boolean execCommandsInParallel) {
        super(execCommandsInParallel);
    }
    
    public abstract void createComponent(byte command, byte[] data, String sessionId, AMQP.BasicProperties props);

}
