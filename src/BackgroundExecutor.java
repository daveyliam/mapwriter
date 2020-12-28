package mapwriter;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import mapwriter.tasks.Task;



/*
This class handles executing and managing 'tasks'.
A single background thread runs tasks in the sequence they are added (via addTask()).
A linked list FIFO queue of every unfinished task is processed every time the processTaskQueue method is called.
processTaskQueue checks the task at the front of the queue to see if the background thread has processed it.
If it the task is complete processTaskQueue calls the onComplete() method of the task and removes it from the queue.
If it is not complete the task is added to the front of the queue again.
In this way the tasks are always processed sequentially, in the order they were added to the queue.

Tasks are extensions of the base Task class.
There are two abstract methods which must be overwritten by the extending class.
  void run()
    Is executed in the background thread when the executor reaches this task.
  void onComplete()
    Is called by processTaskQueue() when the task is done (after the run method is complete).
	This method runs in the main thread so is a good place to copy the results of the run() method.

The run() method of a task added to the queue is guaranteed to be run before the run() method of the next task
added. Likewise the onComplete() method of the first task is guaranteed to be run before the onComplete() of the second
task. However the run() method of any class added after a Task may be executed before the onComplete() method of
the earlier Task is called.

e.g. addTask(Task1)
     addTask(Task2)
	 addTask(Task3)
	 
may run in the order:
    Task1.run()
	Task2.run()
	  Task1.onComplete()
	Task3.run()
	  Task2.onComplete()
	  Task3.onComplete()
*/

public class BackgroundExecutor {
	
	private ExecutorService executor;
	private LinkedList<Task> taskQueue;
	public boolean closed = false;
	
	public BackgroundExecutor() {
		this.executor = Executors.newSingleThreadExecutor();
		this.taskQueue = new LinkedList<Task>();
	}
	
	// add a task to the queue
	public boolean addTask(Task task) {
		if (!this.closed) {
			Future<?> future = this.executor.submit(task);
			task.setFuture(future);
			this.taskQueue.add(task);
		} else {
			MwUtil.log("MwExecutor.addTask: error: cannot add task to closed executor");
		}
		return this.closed;
	}
	
	// Pop a Task entry from the task queue and check if the task's thread has finished.
	// If it has completed then call onComplete for the task.
	// If it has not completed then push the task back on the queue.
	public boolean processTaskQueue() {
		boolean processed = false;
		Task task = this.taskQueue.poll();
		if (task != null) {
			if (task.isDone()) {
				task.printException();
				task.onComplete();
				processed = true;
			} else {
				// put entry back on top of queue
				this.taskQueue.push(task);
			}
		}
		return !processed;
	}
	
	public boolean processRemainingTasks(int attempts, int delay) {
		while ((this.taskQueue.size() > 0) && (attempts > 0)) {
			if (this.processTaskQueue()) {
				try {
					Thread.sleep(delay);
				} catch (Exception e) {
				}
				attempts--;
			}
		}
		return (attempts <= 0);
	}
	
	public int tasksRemaining() {
		return this.taskQueue.size();
	}
	
	public boolean close() {
		boolean error = true;
		try {
			// stop accepting new tasks
			this.executor.shutdown();
			// process remaining tasks
			this.processRemainingTasks(50, 5);
			// should already be terminated, but just in case...
			error = !this.executor.awaitTermination(10L, TimeUnit.SECONDS);
			error = false;
		} catch (InterruptedException e) {
			MwUtil.log("error: IO task was interrupted during shutdown");
			e.printStackTrace();
		}
		this.closed = true;
		return error;
	}
}
