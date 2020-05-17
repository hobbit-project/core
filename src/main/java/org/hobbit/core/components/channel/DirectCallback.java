package org.hobbit.core.components.channel;

import java.util.List;

public interface DirectCallback {

	void callback(byte[] data, List<Object> classs);

}
