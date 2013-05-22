package mapwriter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class Task implements Runnable {
	
	// the task stores its own future
	private Future<?> future;

	// called by processTaskQueue after the thread completes
	public abstract void onComplete();
	
	// the method that runs in a separate thread
	// must not access future in run()
	public abstract void run();
	
	// methods to access the tasks Future variable
	public final Future<?> getFuture() {
		return this.future;
	}
	
	public final void setFuture(Future<?> future) {
		this.future = future;
	}
	
	public final boolean isDone() {
		return this.future.isDone();
	}
	
	public final void printException() {
		try {
			this.future.get();
		} catch (ExecutionException e) {
			Throwable rootException = e.getCause();
			rootException.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
