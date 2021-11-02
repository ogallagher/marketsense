package ogallagher.marketsense.util;

import java.lang.reflect.InvocationTargetException;

/**
 * 
 * @author Owen Gallagher
 * @since 2021-11-02
 *
 */
public abstract class SimpleHasCallback implements HasCallback<Runnable> {
	private Class<Runnable> callback;
	
	public Runnable getCallback(Object ...args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?>[] classes = new Class[args.length];
		for (int i=0; i<args.length; i++) {
			classes[i] = args[i].getClass();
		}
		
		return callback.getDeclaredConstructor(classes).newInstance(args);
	}
	
	public void setCallback(Class<Runnable> callback) {
		this.callback = callback;
	}
}
