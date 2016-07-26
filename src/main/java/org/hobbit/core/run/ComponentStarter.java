package org.hobbit.core.run;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the main method starting a given {@link Component}.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ComponentStarter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentStarter.class);

	/**
	 * Exit code that is used if the program has to terminate because of an internal error.
	 */
	private static final int ERROR_EXIT_CODE = 1;

	/**
	 * This is the main method creating and starting an instance of a
	 * {@link Component} with the given class name.
	 * 
	 * @param args
	 *            The first element has to be the class name of the component.
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			LOGGER.error("Not enough arguments. The name of a class implementing the "
					+ Component.class.getCanonicalName() + " interface was expected.");
			System.exit(ERROR_EXIT_CODE);
		}
		Component component = null;
		boolean success = true;
		try {
			component = createComponentInstance(args[0]);
			// initialize the component
			component.init();
			// run the component
			component.run();
		} catch (Throwable t) {
			LOGGER.error("Exception while executing component. Exiting with error code.", t);
			success = false;
		} finally {
			IOUtils.closeQuietly(component);
		}

		if (!success) {
			System.exit(ERROR_EXIT_CODE);
		}
	}

	/**
	 * This method simply creates an instance of the given class by calling a
	 * constructor that needs no arguments and cats the newly created instance
	 * into a {@link Component} instance. Note that this method assumes that a)
	 * there is a constructor that needs no arguments to be executed and b) the
	 * class with the given name is implementing the {@link Constructor}
	 * interface.
	 * 
	 * @param className
	 *            the name of the class implementing the {@link Component}
	 *            interface.
	 * @return an instance of that class.
	 * @throws ClassNotFoundException
	 *             - If the class with the given name can not be found.
	 * @throws NoSuchMethodException
	 *             - If there is no constructor that needs no parameters.
	 * @throws SecurityException
	 *             - If the constructor can not be accessed because of security
	 *             policies.
	 * @throws InstantiationException
	 *             - If the class with the given class name represents an
	 *             abstract class.
	 * @throws IllegalAccessException
	 *             - If the Constructor object is enforcing Java language access
	 *             control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException
	 *             - If the number of actual and formal parameters differ; if an
	 *             unwrapping conversion for primitive arguments fails; or if,
	 *             after possible unwrapping, a parameter value cannot be
	 *             converted to the corresponding formal parameter type by a
	 *             method invocation conversion; if this constructor pertains to
	 *             an enum type. (Should not occur)
	 * @throws InvocationTargetException
	 *             - If the constructor throws an exception.
	 */
	private static Component createComponentInstance(String className) throws ClassNotFoundException,
			NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Class<?> componentClass = ClassLoader.getSystemClassLoader().loadClass(className);
		Constructor<?> constructor = componentClass.getConstructor();
		return Component.class.cast(constructor.newInstance());
	}
}
