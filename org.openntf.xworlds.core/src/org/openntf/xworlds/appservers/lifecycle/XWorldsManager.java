package org.openntf.xworlds.appservers.lifecycle;

/*

<!--
Copyright 2016 Daniele Vistalli, Paul Withers
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License
-->

*/

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.Thread.State;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.openntf.domino.utils.Factory;
import org.openntf.domino.xots.Xots;
import org.openntf.xworlds.core.Command;
import org.openntf.xworlds.xots.XotsDominoExecutor;

import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;

/**
 * @author Daniele Vistalli
 * @since 1.0.0
 * 
 *        This Manager class acts as an appserver-wide management service to
 *        ensure ODA services are just started once.<br>
 *        <br>
 *        The application listener embedded in every XWorlds enabled application
 *        will startup the engine by calling XWorldsManager.startup()<br>
 *        <br>
 *        The startup code will initialize application tracking code to list all
 *        enabled applications and to manage monitoring and service reporting
 *        and will:
 *        <ul>
 *        <li>Setup a map of initialized "servletContexts" using XWorlds
 *        services
 *        <li>Startup Factory.startup() if not already done globally (managing
 *        concurrency)
 *        <li>Startup a "locker" NotesThread to ensure domino is loaded for the
 *        whole execution of the application server. This will eventually be
 *        replaced by a service similar to ODA's Xots to submit batches. Ideally
 *        XWolds should also come to support Java Batch extensions as supported
 *        by WAS Libery.
 *        </ul>
 *        </br>
 *        The application listener will even manage XWorldsManager.shutdown() to
 *        reduce the number of loaded instances and to update monitoring<br>
 *
 */
public class XWorldsManager {

	private static Logger log = Logger.getLogger(XWorldsManager.class.getName());
	private static XWorldsManager _instance = null;

	private boolean _started = false;
	private boolean _napiStarted = false;

	private AtomicInteger xwmStartedCount = new AtomicInteger(0);
	private AtomicInteger xwmRunningContextsCount = new AtomicInteger(0);

	private int xotsTasks = 10;
	private int xotsStopDelay = 15;

	lotus.domino.NotesThread NotesLockerThread = null;

	/**
	 * Get or creates an instance of this class
	 * 
	 * @return XWorldsManager
	 */
	public static XWorldsManager getInstance() {

		synchronized (XWorldsManager.class) {
			if (_instance == null) {
				_instance = new XWorldsManager();
			}
		}

		return _instance;
	}

	/**
	 * Identifies if the XWorldsManager has been started or not for this server
	 * 
	 * @return boolean whether or not XWorlds is started
	 */
	public boolean isStarted() {
		return _started;
	}

	/**
	 * Starts XWorlds
	 */
	public void Startup() {

		checkState(_started == false, "Cannot call XWorldsManager.Startup() if already running.");
		checkState(Factory.isStarted() == false, "ODA Factory already started, something is wrong");

		// Start OpenNTF Domino API Factory

		log.info("XWorlds:Manager - Starting manager");

		// Synchronize ODA Factory initialization
		synchronized (Factory.class) {

			if (!Factory.isStarted()) {

				Factory.startup();
				log.info("XWorlds:Manager - Starting XOTS");
				XotsDominoExecutor executor = new XotsDominoExecutor(xotsTasks);
				try {
					Xots.start(executor);
					log.info("XWorlds:Manager - XOTS started");
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				NotesThread.sinitThread();

				String userIdPassword = System.getProperty("xworlds.userid.password");

				if (userIdPassword != null) {
					log.info("XWorlds:Manager - Opening UserID for this system");
					try {
						// Open the id if the password is specified
						NotesFactory.createSession((String) null, (String) null, userIdPassword);
					} catch (NotesException e) {
						e.printStackTrace();
					}
				}

				log.info("XWorlds:Manager - ODA has started for: " + Factory.getLocalServerName());
				log.fine("XWorlds:Manager - Initializing nApi");

				if (!_napiStarted) {
					com.ibm.domino.napi.c.C.initLibrary(null);
					_napiStarted = true; // Ensure this is started only once
				}

				log.info("XWorlds:Manager - Starting system Domino thread");

				NotesLockerThread = new NotesThread(new Runnable() {
					@Override
					public void run() {
						boolean stopped = false;
						log.info("XWorlds:Manager - Domino thread started");
						NotesThread.sinitThread();

						while (!stopped) {
							try {
								Thread.sleep(4000);
							} catch (InterruptedException e) {
								log.info("XWorlds:Manager - Interrupted, shutting dowm system Domino thread.");
								NotesThread.stermThread();
								stopped = true;
							}
						}

					}
				}, "XWorlds System Thread");

				NotesLockerThread.setDaemon(true);
				NotesLockerThread.start();

				NotesThread.stermThread();

				_started = true;

			} else {
				log.severe("ODA's Factory failed to start");
			}

		}

		xwmStartedCount.incrementAndGet();

	}

	/**
	 * Outputs XWorlds statistics, called from OSGi command in
	 * {@linkplain Command#report()}
	 * 
	 * @return String report of XWorlds statistics
	 */
	public String getXWorldsReportAsString() {

		checkState(_started == true, "XWorldsManger should bestarted to provide a report.");

		StringBuilder report = new StringBuilder();

		report.append("Started environments count: ");
		report.append(xwmStartedCount.get());
		report.append("\n");

		report.append("Running application contexts: ");
		report.append(xwmRunningContextsCount.get());
		report.append("\n");

		report.append("Active object count: " + Factory.getActiveObjectCount() + "\n");
		report.append("Auto recycle object count: " + Factory.getAutoRecycleCount() + "\n");
		report.append("Factory counters dump:\n" + Factory.dumpCounters(true) + "\n");

		return report.toString();
	}

	/**
	 * Register a servlet application to use XWorlds
	 * 
	 * @param servletContextName
	 *            String servlet context name of the application
	 * @param contextPath
	 *            String servlet context path for accessing the application from
	 *            a browser
	 * @param majorVersion
	 *            int major version of the application
	 * @param minorVersion
	 *            int minor version of the application
	 */
	public void addApplication(String servletContextName, String contextPath, int majorVersion, int minorVersion) {

		checkNotNull(servletContextName, "The servlet context name cannot be null");
		checkNotNull(contextPath, "The servlet context path cannot be null");

		log.info("XWorlds:Manager - Adding " + servletContextName + " [" + contextPath + "] " + majorVersion + "."
				+ minorVersion);

		xwmRunningContextsCount.incrementAndGet();
	}

	/**
	 * De-register a servlet application to use XWorlds
	 * 
	 * @param servletContextName
	 *            String servlet context name of the application
	 * @param contextPath
	 *            String servlet context path for accessing the application from
	 *            a browser
	 * @param majorVersion
	 *            int major version of the application
	 * @param minorVersion
	 *            int minor version of the application
	 */
	public void removeApplication(String servletContextName, String contextPath, int majorVersion, int minorVersion) {

		checkNotNull(servletContextName, "The servlet context name cannot be null");
		checkNotNull(contextPath, "The servlet context path cannot be null");

		log.info("XWorlds:Manager - Removing " + servletContextName + " / " + contextPath + " " + majorVersion + "."
				+ minorVersion);
		xwmRunningContextsCount.decrementAndGet();
	}

	/**
	 * Shut down XWorlds
	 */
	public void Shutdown() {

		checkState(_started == true, "XWorldsManager.Shutdown() must not be called if not started");

		if (xwmStartedCount.decrementAndGet() == 0 && _started == true) {

			// On startedCount = 0 shutdown everything
			NotesLockerThread.interrupt();

			int secs = 0;
			while (NotesLockerThread.getState() != State.TERMINATED && secs < 10) {
				secs++;
				log.fine("Waiting for domino system thread to terminate [" + NotesLockerThread.getState() + "]");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}

			log.info("Shutting down XOTS");
			if (Xots.isStarted()) {
				Xots.stop(xotsStopDelay);
			}
			log.info("Shutting down ODA Factory");
			if (Factory.isStarted()) {
				log.fine("Shutting down ODA on thread " + Thread.currentThread().getId() + " / "
						+ Thread.currentThread().getName());
				Factory.shutdown();
			}

			_started = false;
		}

	}

}
