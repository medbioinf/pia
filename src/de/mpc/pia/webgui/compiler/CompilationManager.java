package de.mpc.pia.webgui.compiler;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import de.mpc.pia.tools.PIAConfigurationProperties;

/**
 * This class manages all the compilations.
 * 
 * @author julian
 *
 */
@ManagedBean(eager=true,
	name="compilationManager")
@ApplicationScoped
public class CompilationManager implements Runnable {
	
	/** the threads waiting to be started */
	private Map<Long, CompilationThread> compilationThreads;
	
	/** the current running threads */
	private Map<Long, CompilationThread> runningThreads;
	
	/** all the temporary files for compilation */
	private Map<String, TempUploadedFile> tempFiles;
	
	/** number of concurrent compilations */
	private Integer maxConcurrentCompilations;
	
	/** the timeout between notifications of the manager to be still alive, in millis */
	private Long notifyTimeout;
	
	/** the number of threads called so far */
	private Long threadsSoFar;
	
	/** the running thread of this class... */
	private Thread runner;
	
	/** if this variable is set to false, the thread finishes */
	private boolean goRun;
	
	/** the monitor, which may resume the wait in the run */
	private Object monitor;
	
	
	@ManagedProperty(value="#{piaConfiguration}")
    private PIAConfigurationProperties configurationProperties;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(CompilationManager.class);
	
	
	/**
	 * Basic constructor.
	 */
	public CompilationManager() {
		logger.info("Initialising the CompilationManager");
		
		compilationThreads = new HashMap<Long, CompilationThread>();
		runningThreads = new HashMap<Long, CompilationThread>();
		tempFiles = new HashMap<String, TempUploadedFile>();
		
		// TODO: these should be set via an ini file
		maxConcurrentCompilations = 1;
		notifyTimeout = 10*60*1000L;	// 10 minutes
		
		// ok, start our run()
		threadsSoFar = 0L;
		goRun = true;
		monitor = new Object();
		runner = new Thread( this );
		runner.start();
	}
	
	
	@PreDestroy
	public void destruct() {
		goRun = false;
		
		// wake up the run() (to fuinish it)
		if (runner.isAlive()) {
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
	}
	
	
	/**
	 * Puts the given {@link TempUploadedFile} into the map of files and returns
	 * the same, as a {@link Map#put(Object, Object)}.
	 * 
	 * @param key
	 * @param tempFile
	 * @return
	 */
	public synchronized TempUploadedFile putNewTempFile(String key,
			TempUploadedFile tempFile) {
		return tempFiles.put(key, tempFile);
	}
	
	
	/**
	 * Puts the {@link TempUploadedFile} with the given key from the map of
	 * files and returns the same, as a {@link Map#remove(Object)}.
	 * 
	 * @param key
	 * @return
	 */
	public synchronized TempUploadedFile removeFromTempFiles(String key) {
		return tempFiles.remove(key);
	}
	
	
	/**
	 * Starts a new compilation thread. Actually, rather creates the thread and
	 * puts it into the list of threads to be run.
	 * 
	 * @param projectName
	 * @param files
	 */
	public synchronized void startCompilationThread(String projectName,
			List<TempCompileFile> files) {
		// we need to synchronize an the Map, because it is checked in the run()
		synchronized (compilationThreads) {
			logger.info("starting a compilation called '" + projectName + "' with " +
					files.size() + " files");
			
			int nrThreads = 0;
			try {
				nrThreads =
						Integer.parseInt(configurationProperties.getPIAProperty("nr_threads", "0"));
			} catch (NumberFormatException e) {
				logger.info("could not get nr_threads from configuration");
			}
			
			CompilationThread thread = new CompilationThread(threadsSoFar+1,
					projectName, files, nrThreads,
					configurationProperties.getPIAProperty("data_path", "/data"),
					monitor);
			compilationThreads.put(thread.getId(), thread);
			threadsSoFar++;
			
			// wake up the run()
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
	}
	
	
	@Override
	public void run() {
		while(goRun) {
			logger.info("CompilationManager " + runner.getName() + " is still running.");
			
			// count the running threads and remove finished threads
			synchronized (runningThreads) {
				Set<Long> keySet = new HashSet<Long>(runningThreads.keySet());
				
				for (Long id : keySet) {
					if (!runningThreads.get(id).isAlive()) {
						runningThreads.remove(id);
					}
				}
				
				logger.info("number of running threads: " + runningThreads.size());
			}
				
			synchronized (compilationThreads) {
				// We need a copy of the map, so we can access it and remove items
				Set<Long> keySet = new HashSet<Long>(compilationThreads.keySet());
				
				// now start some new threads, if we have the capacity
				for (Long id : keySet) {
					if (runningThreads.size() < maxConcurrentCompilations) {
						CompilationThread thread = compilationThreads.get(id);
						compilationThreads.remove(id);
						synchronized (runningThreads) {
							runningThreads.put(id, thread);
						}
						thread.start();
					} else {
						// ok, no more capacities left
						break;
					}
				}
			}
			
			// go to sleep for a while (or until notified)
			synchronized (monitor) {
				try {
					monitor.wait(notifyTimeout);
				} catch (InterruptedException e) {
					logger.error("hm, weird...", e);
				}
			}
		}
		
		logger.info("CompilationManager " + runner.getName() + " is now finished.");
	}
	
	
	/**
	 * Builds a list of all waiting, running and finished compilations.
	 * @return
	 */
	public List<Compilation> getCompilations() {
		HashMap<String, Compilation> compilationsMap = new HashMap<String, Compilation>();
		
		// threads waiting to start
		synchronized (compilationThreads) {
			for (CompilationThread thread : compilationThreads.values()) {
				String key = "99999999" + thread.getCompilationID();
				compilationsMap.put(key, new Compilation(true, false, false, 
								thread.getProjectName(), null, "") );
			}
		}
		
		// running threads
		synchronized (runningThreads) {
			for (CompilationThread thread : runningThreads.values()) {
				String key = "99990000" + thread.getCompilationID();
				compilationsMap.put(key, new Compilation(false, true, false, 
								thread.getProjectName(), null, "") );
			}
		}
		
		// the finished files
		File projectsDir = new File(
				configurationProperties.getPIAProperty("data_path", "/data"));
		
		File[] files = projectsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".pia.xml");
			}
		});
		
		for (int i=0; i < files.length; i++) {
			String key = files[i].getName().substring(0, 17);
			
			// get the name and date from the first few lines
			String compName = null;
			Date compStart = null;
			
			try {
				FileInputStream fstream = new FileInputStream(files[i]);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				Pattern namePattern = Pattern.compile("name=\"([^\"]*)\"");
				Pattern datePattern = Pattern.compile("date=\"([^\"]*)\"");
				Matcher matcher;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				
				String strLine;
				int lineNr = 0;
				while (((strLine = br.readLine()) != null) &&
						(lineNr++ < 10) && // the name and date should be in the first few lines
						(compName == null) &&
						(compStart == null)) {
					
					if (compName == null) {
						matcher = namePattern.matcher(strLine);
						if (matcher.find()) {
							compName = matcher.group(1);
						}
					}
					
					if (compStart == null) {
						matcher = datePattern.matcher(strLine);
						if (matcher.find()) {
							try {
								compStart = sdf.parse(matcher.group(1));
							} catch (ParseException e) {
								logger.error("Error while parsing date '" +
										matcher.group(1) + "'.");
							}
						}
					}
				}
				
				br.close();
			} catch (IOException e) {
				logger.error("Error while trying to get ");
			}
			
			if (compName == null) {
				compName = files[i].getName();
			}
			
			if (compStart == null) {
				compStart = new Date(0L);
			}
			
			compilationsMap.put(key, new Compilation(false, false, false,
					compName, compStart, files[i].getAbsolutePath() ) );
		}
		
		// now sort the compilations and put them into a list
		ArrayList<Compilation> compilations = new ArrayList<Compilation>();
		List<String> mapKeys = new ArrayList<String>(compilationsMap.keySet());
		
		Collections.sort(mapKeys);
		Collections.reverse(mapKeys);
		
		for (String key : mapKeys) {
			compilations.add(compilationsMap.get(key));
		}
		
		return compilations;
	}
	
	
	/**
	 * Setter for the PIAConfigurationProperties, used for injection.
	 * 
	 * @param properties
	 */
	public void setConfigurationProperties(PIAConfigurationProperties properties) {
		this.configurationProperties = properties;
	}
}