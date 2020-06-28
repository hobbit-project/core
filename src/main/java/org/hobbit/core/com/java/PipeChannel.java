package org.hobbit.core.com.java;

import java.nio.channels.Pipe;

import com.rabbitmq.client.AMQP.BasicProperties;
/**
 * PipeChannel is a POJO class to map pipe with the respective properties.
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class PipeChannel {
    
    Pipe pipe;
    BasicProperties props;
	
    public PipeChannel(Pipe pipe, BasicProperties props) {
        this.pipe = pipe;
        this.props = props;
    }
	
    public PipeChannel(Pipe pipe) {
        this.pipe = pipe;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
    }

    public BasicProperties getProps() {
        return props;
    }

    public void setProps(BasicProperties props) {
        this.props = props;
    }
	
}
