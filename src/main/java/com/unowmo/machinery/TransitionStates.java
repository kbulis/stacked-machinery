package com.unowmo.machinery;

/**
 * Simple description of a state machine as an easy-to-deserialize container.
 * 
 * @author Kirk Bulis
 *
 */
public class TransitionStates {

	public String name = "";
	public State [] states = new State[0];

	/**
	 * Machines are composed of states that define axions to execute on entry
	 * and leave, transitions given entry axion results or external events.
	 */
	public static class State {

		public String label = "";
		public String entry = "";
		public String leave = "";
		public Trans [] trans = new Trans[0];
		
		public static class Trans {

			public String event = "";
			public String state = "";

		}

		String followOn(final String event) {
			if (event.isEmpty() == false)
			{
				for (Trans next : this.trans)
				{
					if (next.event.equalsIgnoreCase(event) == true)
					{
						return next.state;
					}
				}
			}

			for (Trans next : this.trans)
			{
				if (next.event.isEmpty() == true)
				{
					return next.state;
				}
			}
			
			return "";
		}

	}
	
}
