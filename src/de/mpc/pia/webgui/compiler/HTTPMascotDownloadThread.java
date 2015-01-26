package de.mpc.pia.webgui.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Class for handling the download of a Mascot result file via HTTP.
 * 
 * @author julian
 */
public class HTTPMascotDownloadThread extends Thread {
	
	/** the progress of downloading */
	private Long downloadProgress;
	
	/** list of error messages */
	private List<String> messages;
	
	/** name of the file for download */
	private String downloadJobName;
	
	/** name of the date path for download */
	private String jobDatePath;
	
	/** the path to the temporary data */
	private String tmpPath;
	
	/** the path to the copied file */
	private String tmpFilePath;
	
	/** the mascot host, e.g. "http://mascotmaster" or "http://192.168.0.10" */
	private String host;
	
	/** the path on the host to mascot, e.g. "mascot" */
	private String mascotPath;
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(HTTPMascotDownloadThread.class);
	
	
	/* TODO: make list of last runs and select one for download:
	 * command to get last 50 runs (in html format)
	 * http://mascotmaster/mascot/x-cgi/ms-review.exe?CalledFromForm=1&logfile=..%2Flogs%2Fsearches.log&start=-1&howMany=50&pathToData=&column=0&s0=1&s1=1&s2=1&s3=1&s4=1&s5=1&s6=1&s7=1&s8=1&s9=1&s10=1&s11=1&s12=1&s14=1&f0=&f1=&f2=&f3=&f4=&f5=&f6=&f7=&f8=&f9=&f10=&f11=&f12=&f14=
	 */
	
	
	/**
	 * Initialises the HTTP mascot download object with the given data.
	 */
	public HTTPMascotDownloadThread(String tmpPath, String host,
			String mascotPath) {
		this.tmpPath = tmpPath;
		this.host = host;
		this.mascotPath = mascotPath;
		
		messages = new ArrayList<String>();
		downloadProgress = -1L;
		
		this.downloadJobName = null;
		this.jobDatePath = null;
	}
	
	
	/**
	 * Getter for the current progress of download.
	 * @return
	 */
	public Long getDownloadProgress() {
		return downloadProgress;
	}
	
	
	/**
	 * Getter for the error messages, may be empty, if everything is ok.
	 * @return
	 */
	public List<String> getMessages() {
		return messages;
	}
	
	
	/**
	 * The path to the copied temporary file
	 * @return
	 */
	public synchronized String getTmpFilePath() {
		return tmpFilePath;
	}
	
	
	/**
	 * The original file name (name of the DAT file on server)
	 * @return
	 */
	public synchronized String getOriginalFileName() {
		return downloadJobName;
	}
	
	
	/**
	 * Initialises the file download.
	 * @param fileName
	 */
	public synchronized void initialiseHTTPDownload(String jobName, String jobDate) {
		if (this.isAlive()) {
			logger.error("Cannot change download file during download!");
		} else {
			downloadJobName = jobName;
			jobDatePath = jobDate;
		}
	}
	
	
	@Override
	public void run() {
		InputStream inputStream = null;
		FileOutputStream fos = null;
		int downloaded = 0; // number of bytes downloaded
		
		downloadProgress = 50l; // if the size is unknown, show something
		messages.clear();
		tmpFilePath = null;
		
		try {
			String params = "do_export=1&"
					+ "export_format=MascotDAT&"
					+ "file=../data/" + jobDatePath + "/" + downloadJobName;
			
			URL downloadUrl = new URL("http://" + host + "/" + mascotPath +
					"/cgi/export_dat_2.pl?" + params);
			
			// open connection to URL
			HttpURLConnection connection = 
					(HttpURLConnection)downloadUrl.openConnection();
			
			// Connect to server.
			connection.connect();
			
			logger.debug("connecting to " + downloadUrl.toString());
			
			// Make sure response code is in the 200 range.
			logger.debug("response code: " + connection.getResponseCode());
			if (connection.getResponseCode() / 100 != 2) {
				messages.add("Connection error: " + connection.getResponseCode());
				logger.error("Connection error: " + connection.getResponseCode());
            }
			
			// Check for valid content length.
			int contentLength = connection.getContentLength();
			if (contentLength < 1) {
				logger.warn("Unknown/invalid content length: " + contentLength);
			}
			
			// Open fileStream and inputStream
			tmpFilePath = tmpPath + "/" +
					(new SimpleDateFormat("yyyyMMddHHmmssSSS")).format(new Date()) +
					"-" + getOriginalFileName();
			fos = new FileOutputStream(tmpFilePath);
			
			inputStream = connection.getInputStream();
			if (inputStream == null) {
				logger.error("Error opening stream to host.");
				messages.add("Error opening stream to host.");
			}
			
            byte buffer[] = new byte[1024];
			while (inputStream != null) {
				// Read from server into buffer.
				int read = inputStream.read(buffer);
				if (read == -1) {
					// the stream is done
					downloadProgress = 100L;
					break;
				}
				
				// Write buffer to file.
				fos.write(buffer, 0, read);
				downloaded += read;
				
				if (contentLength > 0) {
					downloadProgress = (long)(((double)downloaded / contentLength) * 100.0);
				}
			}
		} catch (Exception e) {
			logger.error("Error while downloading.", e);
			messages.add("Error while downloading, " + e.getMessage());
		} finally {
			// Close file.
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("Error closing file.", e);
					messages.add("Error closing file, " + e.getMessage());
				}
			}
			
			// Close connection to server.
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
					logger.error("Error closing stream.", e);
					messages.add("Error closing stream, " + e.getMessage());
				}
			}
		}
		
		if (messages.size() == 0) {
			// file is copied, check for normal DAT file
			try {
				inputStream = new FileInputStream(tmpFilePath);
				BufferedReader reader =
						new BufferedReader(new InputStreamReader(inputStream));
				
				String line;
				boolean fileOk = false;
				while ((line = reader.readLine()) != null) {
					// look for specific lines to indicate good or bad file
					if (line.contains("Mascot results file not found.")) {
						logger.info("Mascot results file not found.");
						messages.add("Mascot results file not found.");
						break;
					} else if (line.contains("Content-Type: application/x-Mascot")) {
						// ok, this is a DAT file
						fileOk = true;
						break;
					}
				}
				
				if (!fileOk) {
					// no line with "Content-Type: application/x-Mascot" found, return error
					messages.add("Error checking for valid DAT file, the file is no valid mascot search file!");
				}
				
				reader.close();
				inputStream.close();
			} catch (Exception e) {
				logger.error("Error checking for valid DAT file.", e);
				messages.add("Error checking for valid DAT file, " + e.getMessage());
			}
		}
		
		if (messages.size() == 0) {
			// all done and file seems ok
			messages.add("Download of '" + downloadJobName + "' done.");
			
			// progress is done
			downloadProgress = 101l;
		} else {
			if (tmpFilePath != null) {
				// delete the file (if it is there)
				File tmpFile = new File(tmpFilePath);
				tmpFile.delete();
			}
			
			// something went wrong, reset progress
			downloadProgress = -1l;
		}
	}
}
