package voterheads.util;

import org.kohsuke.args4j.Option;

public class CLA {

	@Option(name="-r", usage="Comma separated list of Organizations to run")
	static public final String run = "none";
	
	static public final String skip = "all";

}
