package de.mpc.pia.webgui.compiler;

import java.util.Date;

public class Compilation {
	
	private boolean isWaiting;
	
	private boolean isRunning;
	
	private boolean isAborted;
	
	private String compilationName;
	
	private Date compilationStart;
	
	private String filePath;
	
	
	public Compilation(boolean waiting, boolean running, boolean aborted,
			String name, Date compStart, String path) {
		this.isWaiting = waiting;
		this.isRunning = running;
		this.isAborted = aborted;
		this.compilationName = name;
		this.compilationStart = compStart;
		this.filePath = path;
	}
	
	
	public boolean getIsWaiting() {
		return isWaiting;
	}
	
	
	public boolean getIsRunning() {
		return isRunning;
	}
	
	
	public boolean getIsAborted() {
		return isAborted;
	}
	
	
	public Date getCompilationStart() {
		return compilationStart;
	}
	
	
	public String getCompilationName() {
		return compilationName;
	}
	
	
	public String getFilePath() {
		return filePath;
	}
}