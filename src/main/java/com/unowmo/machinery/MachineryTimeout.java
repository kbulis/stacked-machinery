package com.unowmo.machinery;

import java.util.*;

/**
 * Tracks recorded timers and generates timeout events to be handled by specific
 * machine stack layers.
 * 
 * @author Kirk Bulis
 *
 */
public class MachineryTimeout {
	private final List<Timer> timers = new ArrayList<Timer>();

	/**
	 * Runnable container of a set of active timers.
	 */
	private static abstract class Processor implements Runnable {

		final List<Timer> timers;
		
		private Processor(final List<Timer> timers) {
			this.timers = timers;
		}

	}

	/**
	 * Description of a specific, active timer.
	 */
	private static class Timer {

		final StackOfMachinery.Layer target;
		final String event;
		final long when;

		Timer(final StackOfMachinery.Layer target, final String event, final long when) {
			this.target = target;
			this.event = event;
			this.when = when;
		}

	}

	/**
	 * Inserts new active timer into the current set. Timers are executed in
	 * chronological order, with expired timeouts processing immediately.
	 */
	void register(final StackOfMachinery.Layer target, final String event, final long when)
	{
		int posi = 0;

		synchronized (this.timers) {
			for (final Timer timer : this.timers)
			{
				if (timer.target == target && timer.event.equalsIgnoreCase(event) == true)
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
				new Thread(new Processor(this.timers) {
					public void run() {
						synchronized (this.timers) {
							while (this.timers.isEmpty() == false)
							{
								long now = new Date().getTime();
								Timer next = this.timers.get(0);

								if (next.when > now)
								{
									try
									{
										this.timers.wait(next.when - now);
									}
									catch (Exception eX)
									{
									}
								}

								if (this.timers.isEmpty() == false)
								{
									now = new Date().getTime();
									next = this.timers.get(0);
									
									if (next.when <= now)
									{
										// inject event
									}
								}
							}
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
