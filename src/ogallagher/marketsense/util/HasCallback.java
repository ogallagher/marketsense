package ogallagher.marketsense.util;

import java.lang.reflect.InvocationTargetException;

/**
 * TODO explain why this is necessary for instance callback runnables.
 * 
 * @author Owen Gallagher
 * @since 2021-11-02
 *
 */
public interface HasCallback<T extends Runnable> {
	public T getCallback(Object ...args) 
		throws 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, SecurityException;
	
	public void setCallback(Class<T> callback);
}
