package com.unowmo.machinery;

import java.util.*;

/**
 * Tracks recorded timers and generates timeout events to be handled by specific
 * machine stack layers.
 * 
 * @author Kirk Bulis
 *
 */
public abstract class MachineryTimeout {
	private final List<Timer> timers = new ArrayList<Timer>();

	/**
	 * Runnable container of a set of active timers.
	 */
	private static abstract class Processor implements Runnable {

		final MachineryTimeout that;
		
		private Processor(final MachineryTimeout timeout) {
			this.that = timeout;
		}

	}

	/**
	 * Description of a specific, active timer.
	 */
	private static class Timer {

		final String target;
		final String event;
		final long when;

		Timer(final String target, final String event, final long when) {
			this.target = target;
			this.event = event;
			this.when = when;
		}

	}

	/**
	 * Signals to defining container or subclass that a timer has expired on the
	 * timeout processing thread. Make sure to synchronize access to the target.
	 * 
	 * @param target event target
	 * @param event event to handle on timeout
	 */
	protected abstract void onTimeout(final String target, final String event);

	/**
	 * Signals to defining container or subclass there are no more timers queued.
	 */
	protected abstract void onAllDone();
	
	/**
	 * Inserts new active timer into the current set. Timers are executed in
	 * chronological order, with expired timeouts processing immediately.
	 * 
	 * @param target event target
	 * @param event event to handle on timeout
	 * @param when time to handle event (epoch time in ms)
	 */
	void register(final String target, final String event, final long when)
	{
		int posi = 0;

		synchronized (this.timers) {
			for (final Timer timer : this.timers)
			{
				if (timer.target.contentEquals(target) == true && timer.event.equalsIgnoreCase(event) == true)
				{
					this.timers.remove(posi);
					
					break;
				}
				
				++posi;
			}

			posi = 0;
			
			for (final Timer timer : this.timers)
			{
				if (when < timer.when)
				{
					break;
				}

				++posi;
			}

			this.timers.add
				( posi
				, new Timer(target, event, when)
				);
			
			if (posi == 0)
			{
				this.process();
			}
		}
	}

	/**
	 * Internal method for handling active timers. Each call to process restarts
	 * looping through active timers when not currently running. Only one thread
	 * should be active at any time.
	 */
	private void process() {
		synchronized (this.timers) {
			if (this.timers.size() == 1)
			{
				new Thread(new Processor(this) {
					public void run() {
						try
						{
							synchronized (that.timers) {
								while (that.timers.isEmpty() == false)
								{
									long now = new Date().getTime();
									Timer next = that.timers.get(0);
	
									if (next.when <= now)
									{
										that.timers.remove(0);
										
										try
										{
											that.onTimeout(next.target, next.event);
										}
										catch (Exception eX)
										{
										}
									}
									else
									{
										try
										{
											that.timers.wait(next.when - now);
										}
										catch (Exception eX)
										{
										}
									}
								}
							}
						}
						finally
						{
							that.onAllDone();
						}
					}
				}).start();
			}
			else
			{
				this.timers.notify();
			}
		}
	}

	/**
	 * Erases all current timers.
	 */
	void clear() {
		synchronized (this.timers) {
			this.timers.clear();
		}
	}

}
