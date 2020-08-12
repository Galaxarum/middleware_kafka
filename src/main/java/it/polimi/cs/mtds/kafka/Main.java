package it.polimi.cs.mtds.kafka;

import it.polimi.cs.mtds.kafka.functions.FunctionFactory;
import it.polimi.cs.mtds.kafka.functions.StringFunctionFactory;
import it.polimi.cs.mtds.kafka.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Main {

	private static final List<Thread> stageThreads = new LinkedList<>();
	private static final List<Stage<Integer,String,Integer>> stages = new LinkedList<>();

	/**
	 * Open config.properties
	 * Read the list of stages and function executed at each stage from properties
	 * Start a new thread for each stage ({@link Stage})
	 * Join all stages
	 *
	 * @throws IOException if fails to open config.properties
	 */
	public static void main(String[] args) throws IOException {

		//Prepare properties
		final Properties processProperties = new Properties();
		final String propertiesName = args.length>0?args[0]:"config.properties";
		final InputStream propertiesIn = Main.class.getClassLoader().getResourceAsStream(propertiesName);
		final FunctionFactory<String,Integer,String> functionFactory = new StringFunctionFactory();
		try {
			processProperties.load(propertiesIn);
		}catch ( IOException e ){ throw new IOException("Cannot read property file",e); }

		//Read list of stages on this process
		final Integer[] stages = Arrays.stream(processProperties.getProperty("stages").split(","))
				.map(Integer::parseInt).toArray(Integer[]::new);

		//Read function names for each stage
		final String[] functions = processProperties.getProperty("functions").split(",");

		//Safety check
		if(stages.length!=functions.length) throw new IllegalStateException("Invalid property file: the same number of stages and functions is required");

		//Start the stages
		for(int i=0; i<functions.length; i++){
			final Stage<Integer,String, Integer> stage = new Stage<>(functionFactory.getFunction(functions[i]),0,stages[i]);
			final Thread stageThread = new Thread(stage,"Stage "+i);
			Main.stageThreads.add(stageThread);
			Main.stages.add(stage);
			stageThread.start();
		}

		//Handle SIGINT
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			Main.stages.forEach(Stage::shutdown);
			for ( Thread thread : Main.stageThreads )
				try { thread.join(); } catch ( InterruptedException e ) {
					e.printStackTrace();
					System.err.println("ShutdownHook interrupted?");
					return;
				}
			System.out.println("All stages closed");
		}));

		//Output correct start info
		System.out.println("All stages running");
	}
}
