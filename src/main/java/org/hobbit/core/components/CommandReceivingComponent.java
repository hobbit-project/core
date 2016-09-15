package org.hobbit.core.components;

/**
 * This interface is implemented by components that want to receive and process
 * commands from the hobbit command queue.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface CommandReceivingComponent extends Component {

	/**
	 * This method is called if a command is received and might be interesting
	 * for this particular component.
	 * 
	 * @param command
	 *            the byte encoding the command
	 * @param data
	 *            additional data that was sent together with the command
	 */
	public void receiveCommand(byte command, byte[] data);

}
