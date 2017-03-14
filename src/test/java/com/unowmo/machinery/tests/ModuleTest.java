package com.unowmo.machinery.tests;

import java.io.*;
import org.junit.*;
import com.google.gson.*;
import com.unowmo.machinery.*;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ModuleTest {

	private static class Counter {
		int count = 0;
		
	}
	
    @Test
    public void testModule() {
    	final StackOfMachinery stacked = new StackOfMachinery();
    	final Counter hasAnnounced = new Counter();

    	try
    	{
	    	InputStream input = this.getClass().getClassLoader().getResourceAsStream("testing.library");
			
			try
			{
				stacked.initialize(mapper.fromJson(new InputStreamReader(input), AxionTaskLibrary.class));
			}
			finally
			{
				try
				{
					input.close();
				}
				catch (IOException eX)
				{
				}
			}

			stacked.setResolve
				( new AxionTaskResolve() {
					public String execute(final String axionLabel, final LabeledValuePair ... axionPairs) {
						return "success";
					}
					public void command(final String eventCommand, final String eventStatus) {
						System.out.println("command " + eventCommand + ": " + eventStatus);
						
						if (eventCommand.equalsIgnoreCase("testing") == true)
						{
							if (eventStatus.equalsIgnoreCase("announce") == true)
							{
								++hasAnnounced.count;
							}
						}
					}
					public void log(final String message) {
						System.out.println(message);
					}
				});
			
			for (String external : new String [] { "started", "testing", "testing", "testing", "takeoff", "testing" })
			{
				stacked.handleEvent
					( external
					);
			}
			
			Assert.assertEquals
				( "Some testing runs haven't announced"
				, 4
				, hasAnnounced.count
				);
    	}
    	catch (Exception eX)
    	{
    		Assert.fail(eX.getMessage());
    	}
    }

    private static Gson mapper = new Gson();
    
}
