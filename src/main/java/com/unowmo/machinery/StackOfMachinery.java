package com.unowmo.machinery;

import java.util.*;

/**
 * Layered stack of machines that maintains the library, pushes/pops children
 * machines, maintains state, and applies external events while processing
 * axion transitions.
 * 
 * @author Kirk Bulis
 *
 */
public class StackOfMachinery {
	private AxionTaskLibrary library = new AxionTaskLibrary();
	private ListOfGraphEntry entries = new ListOfGraphEntry();
	private AxionTaskResolve resolve = new AxionTaskResolve() {
		protected String execute(final String axionLabel, final LabeledValuePair ... axionPairs) {
			return "";
		}
		protected void command(final String eventCommand, final String eventStatus) {
		}
		protected void log(final String message) {
		}
	};

	/**
	 * Internal interface for handling external events' side effects within the
	 * context of processing said events.
	 */
	private static interface QueuedEvents {

		/**
		 * Signals to handler to start a new machine linked to this machine as
		 * its parent.
		 * 
		 * @param machine
		 * @param frame
		 * @param who
		 */
		String start(final String machine, final LabeledValuePair [] frame, final Layer who);

		/**
		 * Tells handler to send command to the adapter for all other attached
		 * handlers; allows plugins to process application events orginated by
		 * machine axions.
		 *  
		 * @param command
		 * @param status
		 * @param who
		 */
		String patch(final String command, final String status, final Layer who);

		String write(final String label, final String value, final Layer who);

		String count(final LabeledValuePair [] tuple, final Layer who);

		String raise(final String event, final Layer who);

		String pop(final Layer who);

	}

	/**
	 * Internal interface for dealing with layers during navigation through the
	 * hierarchy of graphed layer entries.
	 */
	private interface OnGraphedEntries {

		void onVisit(final Layer target);

	}
	
	/**
	 * Container of layers. Stores layers in a parent-child hierarchy, where
	 * each entry points to its parent. Traversable by closure.
	 */
	private static class ListOfGraphEntry {
		final List<Entry> graphed = new ArrayList<Entry>();

		void visitClosure(final Layer ancestor, final OnGraphedEntries v) {
			for (Entry entry : this.graphed)
			{
				Entry current = entry;
				
				while (current.parent != null)
				{
					if (current.parent.target == ancestor)
					{
						v.onVisit(entry.target);
					}
					
					current = current.parent;
				}
			}
		}

	}
	
	/**
	 * Container of hierarchically graphed layer entries. 
	 */
	private static class Entry {
		private final Entry parent;
		private final Layer target;
		
		Entry(final Entry start, final Layer layer) {
			this.parent = start;
			this.target = layer;
		}

	}

	/**
	 * Simple container for counts in string format.
	 */
	private static class Count {
		private Integer count = 0;

		Count increment() {
			++this.count;
			
			return this;
		}
	
		String value() {
			return this.count.toString();
		}

	}
	
	/**
	 * Internal container.
	 */
	static class Layer extends Frames {
		private final TransitionStates machine;
		private final String uniqued;
		private Integer current = 0;

		boolean matching(final LabeledValuePair ... tuple) {
			for (final LabeledValuePair pair : tuple)
			{
				if (this.isMatching(pair.label, pair.value) == false)
				{
					return false;
				}
			}
			
			return true;
		}

		void followOn(final String external, final AxionTaskResolve resolve, final QueuedEvents handler) {
			if (this.current >= 0 && this.current < this.machine.states.length)
			{
				// Given current state, we check for the possibility to transition
				// out of here. If so, then we want to advance the machine to the
				// next state by matching the event to a transition. We don't use
				// the "any" path with asynchronous events.
				
				TransitionStates.State state = this.machine.states[this.current];

				if (state.trans.length > 0)
				{
					String followTo = state.followOn(external);
					String previous = "";

					while (followTo.isEmpty() == false)
					{
						int i = this.current;
						
						resolve.log
							( String.format
								( "(%s) state '%s' on '%s' -> '%s'"
								, this.uniqued
								, state.label
								, external
								, followTo
								)
							);

						for ( ; ; )
						{
							final TransitionStates.State match = this.machine.states[i];
							
							if (match.label.equalsIgnoreCase(followTo) == true)
							{
								String opRes;

								if (state.leave.isEmpty() == false)
								{
									// Process axion on leaving the current state before
									// processing any axion associated with entering the
									// target state. We ignore the result.
									
									AxionTaskResolve.Part part = this.expand(resolve.split(state.leave));

									if (part.label.equalsIgnoreCase("start") == true)
									{
										String machine = part.valueOf("machine", "");
										
										if (machine.isEmpty() == false)
										{
											handler.start(machine, part.list, this);
										}
									}
									else
									if (part.label.equalsIgnoreCase("patch") == true)
									{
										String command = part.valueOf("command", "");
										String status = part.valueOf("status", "");
										
										if (command.isEmpty() == false)
										{
											handler.patch(command, status, this);
										}
									}
									else
									if (part.label.equalsIgnoreCase("set") == true)
									{
										this.apply
											( part.valueOf("label", "")
											, part.valueOf("value", "")
											);
									}
									else
									if (part.label.equalsIgnoreCase("pop") == true)
									{
										this.pop();
									}
									else
									{
										resolve.execute
											( part.label
											, part.list
											);
									}
								}
								
								state = match;
								
								if (state.entry.isEmpty() == false)
								{
									// Handle the axion associated with transitioning to
									// the target state.
									
									AxionTaskResolve.Part part = this.expand(resolve.split(state.entry));

									if (part.label.equalsIgnoreCase("start") == true)
									{
										opRes = handler.start
											( part.valueOf("machine", "")
											, part.list
											, this
											);
									}
									else
									if (part.label.equalsIgnoreCase("raise") == true)
									{
										opRes = handler.raise
											( part.valueOf("event", "")
											, this
											);
									}
									else
									if (part.label.equalsIgnoreCase("patch") == true)
									{
										opRes = handler.patch
											( part.valueOf("command", "")
											, part.valueOf("status", "")
											, this
											);
									}
									else
									if (part.label.equalsIgnoreCase("write") == true)
									{
										opRes = handler.write
											( part.valueOf("label", "")
											, part.valueOf("value", "")
											, this
											);
									}
									else
									if (part.label.equalsIgnoreCase("count") == true)
									{
										opRes = handler.count
											( part.list
											, this
											);
									}
									else
									if (part.label.equalsIgnoreCase("inc") == true)
									{
										String value = this.matchUp(part.valueOf("label", ""), "0");
										
										opRes = "success";

										try
										{
											value = Integer.toString(Integer.parseInt(value) + Integer.parseInt(part.valueOf("value", "0")));
										}
										catch (Exception eX)
										{
											opRes = "failure";
											value = "0";
										}
										
										this.apply
											( part.valueOf("label", "")
											, value
											);
									}
									else
									if (part.label.equalsIgnoreCase("dec") == true)
									{
										String value = this.matchUp(part.valueOf("label", ""), "0");
										
										opRes = "success";

										try
										{
											value = Integer.toString(Integer.parseInt(value) - Integer.parseInt(part.valueOf("value", "0")));
										}
										catch (Exception eX)
										{
											opRes = "failure";
											value = "0";
										}
										
										this.apply
											( part.valueOf("label", "")
											, value
											);
									}
									else
									if (part.label.equalsIgnoreCase("equ") == true)
									{
										String value = this.matchUp(part.valueOf("label", ""), "0");

										opRes = "notsame";
										
										if (value.equalsIgnoreCase(part.valueOf("value", "")) == true)
										{
											opRes = "success";
										}
										
										resolve.log
											( String.format
												( "(%s) axion '%s' of '%s' <- '%s'"
												, this.uniqued
												, part.label
												, value
												, opRes
												)
											);
									}
									else
									if (part.label.equalsIgnoreCase("set") == true)
									{
										opRes = "success";

										this.apply
											( part.valueOf("label", "")
											, part.valueOf("value", "")
											);
									}
									else
									if (part.label.equalsIgnoreCase("rem") == true)
									{
										opRes = external;
									}
									else
									if (part.label.equalsIgnoreCase("use") == true)
									{
										opRes = previous;
									}
									else
									if (part.label.equalsIgnoreCase("new") == true)
									{
										this.push();

										opRes = "";
									}
									else
									if (part.label.equalsIgnoreCase("pop") == true)
									{
										this.pop();
										
										opRes = "";
									}
									else
									{
										opRes = resolve.execute
											( part.label
											, part.list
											);
									}

									// After processing any associated entry axion, we
									// figure out the next state based on the result of
									// that axion.
									
									followTo = state.followOn
										( opRes
										);
									
									// Keep track of axion results for recalling by
									// subsequent axions.
									
									previous = opRes;
								}
								else
								{
									// No axion, but there may be an automatic path
									// traversal to take. If so, take it. If not,
									// we expect and empty result, which should be
									// ignored and end traversal.
									
									followTo = state.followOn
										( ""
										);
								}

								break;
							}
							
							++i;
							
							if (i == this.machine.states.length)
							{
								i = 0;
							}
							
							if (i == this.current)
							{
								break;
							}
						}

						if (i != this.current)
						{
							this.current = i;
						}
						else
						{
							resolve.log
								( String.format
									( "(%s) state '%s' on '%s' -> '%s' is invalid transition (not found)"
									, this.uniqued
									, state.label
									, external
									, followTo
									)
								);
							
							followTo = "";
						}

						if (followTo.isEmpty() == true)
						{
							break;
						}
					}
				}
					
				if (state.label.equalsIgnoreCase("final") == true)
				{
					if (state.leave.isEmpty() == false)
					{
						AxionTaskResolve.Part part = this.expand(resolve.split(state.leave));

						if (part.label.equalsIgnoreCase("start") == true)
						{
							String machine = part.valueOf("machine", "");
							
							if (machine.isEmpty() == false)
							{
								handler.start(machine, part.list, this);
							}
						}
						else
						if (part.label.equalsIgnoreCase("patch") == true)
						{
							String command = part.valueOf("command", "");
							String status = part.valueOf("status", "");
							
							if (command.isEmpty() == false)
							{
								handler.patch(command, status, this);
							}
						}
						else
						if (part.label.equalsIgnoreCase("write") == true)
						{
							String label = part.valueOf("label", "");
							String value = part.valueOf("value", "");
							
							if (label.isEmpty() == false)
							{
								handler.write(label, value, this);
							}
						}
						else
						if (part.label.equalsIgnoreCase("raise") == true)
						{
							String event = part.valueOf("event", "");
							
							if (event.isEmpty() == false)
							{
								handler.raise(event, this);
							}
						}
					}

					handler.pop
						( this
						);
				}
			}
		}

		private AxionTaskResolve.Part expand(final AxionTaskResolve.Part part) {
			for (final LabeledValuePair pair : part.list)
			{
				int l = pair.value.length();

				if (l > 4)
				{
					if (pair.value.charAt(0) == '(' && pair.value.charAt(1) == '(' && pair.value.charAt(l - 2) == ')' && pair.value.charAt(l - 1) == ')')
					{
						pair.value = this.matchUp(pair.value.substring(2, l - 2).trim(), "");
					}
				}
			}
			
			return part;
		}

		Layer(final TransitionStates machine) {
			this.uniqued = String.format("%08x", random.nextInt());
			
			this.machine = machine;

			while (this.current < this.machine.states.length)
			{
				if (this.machine.states[this.current].label.equalsIgnoreCase("start") == true)
				{
					return;
				}
				
				++this.current;
			}
			
			this.current = 0;

			this.push();
		}

		static Random random = new Random();
		
	}

	/**
	 * Internal container.
	 */
	static class Frames {
		final List<Frame> frames = new ArrayList<Frame>();

		boolean isMatching(final String label, final String value) {
			for (int i = this.frames.size() - 1; i >= 0; i--)
			{
				for (final Value match : this.frames.get(i).values)
				{
					if (match.label.equalsIgnoreCase(label) == true)
					{
						if (match.value.equalsIgnoreCase(value) == true)
						{
							return true;
						}
						else
						{
							return false;
						}
					}
				}
			}
			
			return false;
		}
		
		String matchUp(final String label, final String ifNoMatch) {
			for (final Frame frame : this.frames)
			{
				for (final Value match : frame.values)
				{
					if (match.label.equalsIgnoreCase(label) == true)
					{
						return match.value;
					}
				}
			}

			return ifNoMatch;
		}

		void apply(final String label, final String value) {
			for (final Frame frame : this.frames)
			{
				for (final Value match : frame.values)
				{
					if (match.label.equalsIgnoreCase(label) == true)
					{
						match.value = value;
						
						return;
					}
				}
			}

			this.frames.get(0).set
				( label
				, value
				);
		}
		
		void write(final String label, final String value) {
			this.frames.get(0).set
				( label
				, value
				);
		}

		void push() {
			this.frames.add(0, new Frame());
		}

		void pop() {
			if (this.frames.size() > 1)
			{
				this.frames.remove(0);
			}
		}

		Frames() {
			this.frames.add(new Frame());
		}

	}

	/**
	 * Internal container.
	 */
	static class Frame extends Values {
	}

	/**
	 * Internal container.
	 */
	static class Values {
		final List<Value> values = new ArrayList<Value>();

		void set(final String label, final String value) {
			if (label.isEmpty() == false)
			{
				for (final Value match : this.values)
				{
					if (match.label.equalsIgnoreCase(label) == true)
					{
						match.value = value;
						
						return;
					}
				}
				
				this.values.add(new Value(label, value));
			}
		}
		
	}

	/**
	 * Internal container.
	 */
	static class Value {
		
		public String label = "";
		public String value = "";

		Value(final String label, final String value) {
			this.label = label;
			this.value = value;
		}

	}

	/**
	 * Internal container.
	 */
	static class Event {

		public final String event;
		public final Layer layer;
		
		public Event(final String event, final Layer layer) {
			this.event = event;
			this.layer = layer;
		}

	}

	/**
	 * Process external event and queue up side effects to be handled in order
	 * as we progress. We run through the axion results as intermediate events,
	 * so no external event can interrupt the complete processing of a prior
	 * external event and all of its side effects. We use the currently set
	 * axion task resolver hook throughout processing of this event.
	 * 
	 * @param external event to process
	 * @param target specific layer to limit handling, or null for all
	 * 
	 * @return this instance
	 */
	private StackOfMachinery handleEvent(final String external, final String target) {
		final AxionTaskResolve contain = this.resolve;
		
		if (external.isEmpty() == false)
		{
			final List<Event> queuing = new ArrayList<Event>();

			contain.log
				( String.format
					( "Handling %s"
					, external
					)
				);
			
			for (final Entry entry : this.entries.graphed)
			{
				if (entry.target.uniqued.contentEquals(target) || target.isEmpty() == true)
				{
					queuing.add(new Event(external, entry.target));
				}
			}

			if (queuing.isEmpty() == false)
			{
				final QueuedEvents handler = new QueuedEvents() {
					private final ListOfGraphEntry hierarchy = entries;
					
					public String start(final String namedAs, final LabeledValuePair [] frame, final Layer who) {
						for (final Entry entry : this.hierarchy.graphed)
						{
							if (entry.target != who)
							{
								continue;
							}

							for (final TransitionStates machine : library.machines)
							{
								if (machine.name.equalsIgnoreCase(namedAs) == true)
								{
									Layer child = new Layer(machine);

									queuing.add(new Event("started", child));

									for (LabeledValuePair pair : frame)
									{
										child.apply(pair.label, pair.value);
									}
									
									entries.graphed.add
										( new Entry
											( entry
											, child
											)
										);
									
									return "success";
								}
							}
							
							return "invalid";
						}
						
						return "failure";
					}

					public String patch(final String command, final String status, final Layer who) {
						for (final Entry entry : this.hierarchy.graphed)
						{
							if (entry.target != who)
							{
								continue;
							}

							contain.command(command,  status);

							return "success";
						}
						
						return "failure";
					}

					public String write(final String label, final String value, final Layer who) {
						for (final Entry entry : this.hierarchy.graphed)
						{
							if (entry.target != who)
							{
								continue;
							}

							if (entry.parent != null)
							{
								if (label.isEmpty() == false)
								{
									entry.parent.target.write(label, value);
								}
							}
							
							return "success";
						}
						
						return "failure";
					}

					public String count(final LabeledValuePair [] tuple, final Layer who) {
						final Count count = new Count();
						
						this.hierarchy.visitClosure
							( who
							, new OnGraphedEntries() {
								public void onVisit(final Layer target) {
									if (target.matching(tuple) == true)
									{
										count.increment();
									}
								}
							});

						return count.value();
					}

					public String raise(final String event, final Layer who) {
						for (final Entry entry : this.hierarchy.graphed)
						{
							if (entry.target != who)
							{
								continue;
							}

							if (entry.parent != null)
							{
								if (event.isEmpty() == false)
								{
									queuing.add(new Event(event, entry.parent.target));
								}
							}
							
							return "success";
						}
						
						return "failure";
					}
				
					public String pop(final Layer who) {
						for (final Entry entry : this.hierarchy.graphed)
						{
							if (entry.target != who)
							{
								continue;
							}

							if (entry.parent != null)
							{
								this.hierarchy.graphed.remove
									( entry
									);
							}

							return "success";
						}
						
						return "failure";
					}

				};
				
				for (int i = 0; i < queuing.size(); ++i)
				{
					Event next = queuing.get(i);

					contain.log
						( String.format
							( "Layer (%s) of %s following on %s"
							, next.layer.uniqued
							, next.layer.machine.name
							, next.event
							)
						);
					
					next.layer.followOn
						( next.event
						, contain
						, handler
						);
				}
			}			
		}
		
		return this;
	}

	/**
	 * Process external event and queue up side effects to be handled in order
	 * as we progress. We run through the axion results as intermediate events,
	 * so no external event can interrupt the complete processing of a prior
	 * external event and all of its side effects. We use the currently set
	 * axion task resolver hook throughout processing of this event.
	 * 
	 * @param external event to process
	 * 
	 * @return this instance
	 */
	public StackOfMachinery handleEvent(final String external) {
		return this.handleEvent(external, "");
	}
	
	/**
	 * Reset entry graph and point to new library. We clean house and start it
	 * all over again.
	 * 
	 * @param library deserialized set of machine declarations
	 * 
	 * @return this instance
	 */
	public StackOfMachinery initialize(final AxionTaskLibrary library) {
		this.entries.graphed.clear();
		
		for (final TransitionStates machine : library.machines)
		{
			if (machine.name.equalsIgnoreCase("default") == true)
			{
				this.entries.graphed.add
					( new Entry
						( null
						, new Layer
							( machine
							)
						)
					);
				
				break;
			}
		}

		this.library = library;
		
		return this;
	}

	/**
	 * Apply container-specific resolver of application-defined axions. This is
	 * your hook to process axion requests that only make sense within the app
	 * domain this stack of machines is deployed. Multiple threads access this
	 * hook, but we expect updates to be atomic. That said, try to set your app
	 * resolver hook before starting the stack.
	 * 
	 * @param updated new hook for handling machine axions
	 * 
	 * @return this instance
	 */
	public StackOfMachinery setResolve(final AxionTaskResolve updated) {
		if (updated != null)
		{
			this.resolve = updated;
		}
		
		return this;
	}
	
	/**
	 * Construct default.
	 * 
	 * @param library deserialized set of machine declarations
	 */
	public StackOfMachinery(final AxionTaskLibrary library) {
		this.initialize(library);
	}	

	/**
	 * Construct default.
	 */
	public StackOfMachinery() {
	}

}