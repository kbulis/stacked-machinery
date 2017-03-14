package com.unowmo.machinery;

/**
 * Contract for machine stack container that is executed when machine activity
 * causes transition into or from a state with defined axions. 
 * 
 * @author Kirk Bulis
 *
 */
public abstract class AxionTaskResolve {

	/**
	 * Container for command parameters as specified by machine states for each
	 * axion (enter or leave). 
	 */
	public static class Part {

		public LabeledValuePair [] list = new LabeledValuePair [0]; 
		public String label = "";

		String valueOf(final String key, final String ifNoMatch) {
			for (final LabeledValuePair pair : this.list)
			{
				if (pair.label.equalsIgnoreCase(key) == true)
				{
					return pair.value;
				}
			}
			
			return ifNoMatch;
		}

	}
	
	/**
	 * Handles labeled axion and produces result event (or empty if no event
	 * intended).
	 * 
	 * @param axionLabel label of axion to execute
	 * @param axionPairs pairs of named-value args
	 * 
	 * @return string response for follow on
	 */
	protected abstract String execute(final String axionLabel, final LabeledValuePair ... axionPairs);

	/**
	 * Broadcast to container from handling entity arbitrary command with
	 * status to notify coupled machines of application-specific requests.
	 * 
	 * @param eventCommand command to broadcast to listeners
	 * @param eventStatus status of broadcast command
	 */
	protected abstract void command(final String eventCommand, final String eventStatus);

	/**
	 * Splits an axion string into its label and named-value
	 * pairs for subsequent handling by the state machine.
	 * 
	 * Example:
	 * <pre>
	 * {@code"dosomething:label=argument,value=5,extra=foo"}
	 * 
	 * splits into
	 * 
	 * { "dosomething"
	 * , [ "label" = "argument"
	 *   , "value" = "5"
	 *   , "extra" = "foo"
	 *   ]
	 * }
	 * </pre>
	 * 
	 * @param axion axion string declared in state descriptor
	 * 
	 * @return label and parameters for subsequent handling
	 */
	protected Part split(final String axion) {
		Part breakOut = new Part();
		int p = axion.indexOf(':');
		
		if (p > 0)
		{
			String [] args = axion.substring(p + 1).split(",");

			breakOut.label = axion.substring(0, p).trim();

			if (args.length > 0)
			{
				int i = 0;
				
				breakOut.list = new LabeledValuePair [args.length];
				
				for (String pair : args)
				{
					LabeledValuePair keep = new LabeledValuePair();
					
					p = pair.indexOf("=");
					
					if (p > 0)
					{
						keep.label = pair.substring(0, p + 0).trim();
						keep.value = pair.substring(p + 1).trim();
					}
					
					breakOut.list[i++] = keep;
				}
			}
		}
		else
		if (p < 0)
		{
			breakOut.label = axion;
		}
		
		return breakOut;
	}

	/**
	 * Records to log arbitrary message from resolving container.
	 * 
	 * @param message string to write to the log
	 */
	protected abstract void log(final String message);

}
