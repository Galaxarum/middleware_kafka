package it.polimi.cs.mtds.kafka;

import it.polimi.cs.mtds.kafka.functions.FunctionFactory;
import it.polimi.cs.mtds.kafka.functions.StringFunctionFactory;
import it.polimi.cs.mtds.kafka.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class PipelineRunner {

	private static final List<Thread> stageThreads = new LinkedList<>();
	private static final List<Stage<String,String,String, String>> stages = new LinkedList<>();
	private static final Logger logger = Logger.getLogger(PipelineRunner.class.getName());

	/**
	 * Open config.properties
	 * Read the list of stages and function executed at each stage from properties
	 * Start a new thread for each stage ({@link Stage})
	 * Join all stages
	 *
	 * @throws IOException if fails to open config.properties
	 */
	public static void main(String[] args) throws IOException {

		if(args.length<=0) throw new IllegalArgumentException("Specify a property file");

		//Prepare properties
		final Properties processProperties = new Properties();
		final InputStreamReader propertiesIn = new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8);
		final FunctionFactory<String,String,String> functionFactory = new StringFunctionFactory();
		try {
			processProperties.load(propertiesIn);
		}catch ( IOException e ){ throw new IOException("Cannot read property file",e); }

		final String bootstrap_servers = processProperties.getProperty("bootstrap.servers");
		logger.info("Loaded property file");

		//Read list of stages on this process
		final Integer[] stages = Arrays.stream(processProperties.getProperty("stages").split(","))
				.map(Integer::parseInt).toArray(Integer[]::new);

		//Read replica ids on this process
		final Integer[] ids = Arrays.stream(processProperties.getProperty("ids").split(","))
				.map(Integer::parseInt).toArray(Integer[]::new);

		//Read function names for each stage
		final String[] functions = processProperties.getProperty("functions").split(",");

		//Safety check
		if(stages.length!=functions.length || stages.length!=ids.length) throw new IllegalStateException("Invalid property file: the same number of stages, ids and functions is required");
		else logger.info("Properties are ok");

		//Start the stages
		for(int i=0; i<functions.length; i++){
			final Stage<String, String, String, String> stage = new Stage<>(functionFactory.getFunction(functions[i]), "0", stages[i], ids[i],bootstrap_servers);
			final Thread stageThread = new Thread(stage,"Stage "+i);
			PipelineRunner.stageThreads.add(stageThread);
			PipelineRunner.stages.add(stage);
			stageThread.start();
			logger.info("Started new stage executing function "+functions[i]);
		}

		//Handle SIGINT
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			PipelineRunner.stages.forEach(Stage::shutdown);
			for ( Thread thread : PipelineRunner.stageThreads )
				try { thread.join(); } catch ( InterruptedException e ) {
					e.printStackTrace();
					System.err.println("ShutdownHook interrupted?");
					return;
				}
			logger.info("All stages closed");
		}));

		//Output correct start info
		logger.info("All stages running");
	}
}
