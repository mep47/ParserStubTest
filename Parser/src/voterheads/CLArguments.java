package voterheads;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import static java.lang.System.exit;

public class CLArguments {
	
    private static Logger logger = Logger.getLogger(Voterheads.class);
	
    public static final String RUN = "r";
    public static final String SKIP = "s";
	public static final String ORG_URL = "ou";
	public static final String RETURN_URL = "ru";
	
	private static Map<String,String> defaultValues = new HashMap<String,String>();
	
    private static CommandLine cmd = null;
	
	public static void initialize(String[] args)
	{
		Options options = new Options();
		Option opt = null;
        CommandLineParser cliParser = new GnuParser();
		
		options.addOption(RUN, true, "List of Organizations to Process");
		options.addOption(SKIP, true, "List of Organizations to skip");
		options.addOption(ORG_URL, true, "Url of site which sends Organization List");
		options.addOption(RETURN_URL, true, "URL of site to send Parse Results");
        
        try {
			cmd = cliParser.parse(options, args);
		} catch (ParseException e) {
			logger.error(e);
			exit(1);
		}
        
        defaultValues.put(ORG_URL, "https://www.voterheads.com/organizations.json");
        defaultValues.put(RETURN_URL, "http://api.voterheads.com/v1/events/");
	}
	
	public static String getArgValue(String optionName)
	{
		String value = null;
		String defaultValue = null;

        if(cmd.hasOption(optionName))
        {
        	defaultValue = defaultValues.get(optionName);
        	if(defaultValue == null)
        	{
        		value = cmd.getOptionValue(optionName);
        	}
        	else
        	{
        		value = cmd.getOptionValue(optionName, defaultValue);
        	}
        }
        
        return value;
	}
	
	public static void getMultiValueArg(String optionName,Set<String> argSet)
	{
		String value = null;
		String defaultValue = null;
		
        if(cmd.hasOption(optionName))
        {
        	defaultValue = defaultValues.get(optionName);
        	if(defaultValue == null)
        	{
        		value = cmd.getOptionValue(optionName);
        	}
        	else
        	{
        		value = cmd.getOptionValue(optionName, defaultValue);
        	}
    		
    		argSet.clear();
    		for(String arg: value.split(","))
    		{
    			argSet.add(arg.trim());
    		}

        }
		
	}
}
