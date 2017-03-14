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
	 * ...
	 */
	private static abstract class Processor implements Runnable {
		final List<Timer> timers;
		
		private Processor(final List<Timer> timers) {
			this.timers = timers;
		}

	}

	/**
	 * ...
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
	 * ...
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
	 * ...
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
	 * ...
	 */
	void clear() {
		synchronized (this.timers) {
			this.timers.clear();
		}
	}

}
