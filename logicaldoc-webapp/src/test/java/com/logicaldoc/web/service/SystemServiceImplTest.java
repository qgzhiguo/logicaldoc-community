package com.logicaldoc.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.logicaldoc.core.document.DocumentEvent;
import com.logicaldoc.core.job.AbstractJob;
import com.logicaldoc.core.job.JobManager;
import com.logicaldoc.core.task.Task;
import com.logicaldoc.core.task.TaskTrigger;
import com.logicaldoc.gui.common.client.ServerException;
import com.logicaldoc.gui.common.client.beans.GUIHistory;
import com.logicaldoc.gui.common.client.beans.GUIParameter;
import com.logicaldoc.gui.common.client.beans.GUITask;
import com.logicaldoc.gui.common.client.beans.GUIValue;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.config.LogConfigurator;
import com.logicaldoc.util.io.FileUtil;
import com.logicaldoc.util.plugin.PluginException;
import com.logicaldoc.web.AbstractWebappTestCase;
import com.logicaldoc.web.UploadServlet;

public class SystemServiceImplTest extends AbstractWebappTestCase {

	// Instance under test
	private SystemServiceImpl testSubject = new SystemServiceImpl();

	@Override
	public void setUp() throws FileNotFoundException, IOException, SQLException, PluginException {
		super.setUp();
		Context.get().getProperties().setMaxBackups(0);

		File testWebappFolder = new File("target/webapp");
		testWebappFolder.mkdir();
		SystemServiceImpl.defaultWebappRootFolder = testWebappFolder;
	}

	@Override
	public void tearDown() throws SQLException {
		super.tearDown();
		FileUtil.strongDelete(SystemServiceImpl.defaultWebappRootFolder);
	}

	@Override
	protected List<String> getPluginArchives() {
		return List.of("/logicaldoc-core-plugin.jar", "/logicaldoc-dummy-plugin.jar");
	}

	@Test
	public void testLoadTasks() throws ServerException {
		List<GUITask> tasks = testSubject.loadTasks("en");
		assertEquals(6, tasks.size());
	}

	@Test
	public void testGetTastByName() throws ServerException {
		GUITask task = testSubject.getTaskByName("IndexerTask", "en");
		assertNotNull(task);
		assertEquals("IndexerTask", task.getName());

		assertNull(testSubject.getTaskByName("Unexisting", "en"));
	}

	@Test
	public void testSaveTask() throws ServerException {
		GUITask task = testSubject.getTaskByName("IndexerTask", "en");
		assertNotNull(task);

		task.getScheduling().setSimple(false);
		task.getScheduling().setMode(TaskTrigger.MODE_CRON);
		task.getScheduling().setCronExpression("00 00 10/10 * * ?");
		testSubject.saveTask(task, "en");
		task = testSubject.getTaskByName("IndexerTask", "en");
		assertNotNull(task);
		assertEquals("00 00 10/10 * * ?", task.getScheduling().getCronExpression());

		task.getScheduling().setSimple(true);
		task.getScheduling().setMode(TaskTrigger.MODE_SIMPLE);
		task.getScheduling().setDelay(3);
		testSubject.saveTask(task, "en");
		task = testSubject.getTaskByName("IndexerTask", "en");
		assertNotNull(task);
		assertEquals(3, task.getScheduling().getDelay());
	}

	@Test
	public void testStartStop() throws ServerException, InterruptedException {
		testSubject.startTask("IndexerTask");
		GUITask task = testSubject.getTaskByName("IndexerTask", "en");

		testSubject.stopTask("IndexerTask");
		waiting();
		task = testSubject.getTaskByName("IndexerTask", "en");
		assertEquals(Task.STATUS_IDLE, task.getStatus());
	}

	@Test
	public void testEnableDisable() throws ServerException, InterruptedException {
		testSubject.disableTask("IndexerTask");
		GUITask task = testSubject.getTaskByName("IndexerTask", "en");
		assertFalse(task.getScheduling().isEnabled());

		testSubject.enableTask("IndexerTask");
		task = testSubject.getTaskByName("IndexerTask", "en");
		assertTrue(task.getScheduling().isEnabled());
	}

	@Test
	public void testSetGUILanguageStatus() throws ServerException {
		ContextProperties config = Context.get().getProperties();
		boolean oldEnabled = "enabled".equals(config.getProperty("default.lang.it.gui"));
		testSubject.setGUILanguageStatus("it", !oldEnabled);
		boolean newEnabled = "enabled".equals(config.getProperty("default.lang.it.gui"));
		assertNotSame(oldEnabled, newEnabled);
	}

	@Test
	public void testRestart() throws ServerException {
		ContextProperties config = Context.get().getProperties();
		testSubject.restart();
		File restartFile = new File(config.getProperty("LDOCHOME") + "/updates/restart");
		try {
			assertTrue(restartFile.exists());
		} finally {
			FileUtil.strongDelete(restartFile);
		}
	}

	@Test
	public void testConfirmUpdate() throws ServerException, IOException {
		ContextProperties config = Context.get().getProperties();

		try {
			assertEquals("default", config.getProperty("runlevel"));
			assertEquals("test", config.getProperty("runlevel.back"));
			testSubject.confirmUpdate();
			assertEquals("test", config.getProperty("runlevel"));
		} finally {
			config.setProperty("runlevel", "default");
			config.write();
		}
	}

	@Test
	public void testGetPlugins() throws ServerException {
		List<GUIValue> plugins = testSubject.getPlugins();
		assertEquals(2, plugins.size());
	}

	@Test
	public void testSearch() throws ServerException, InterruptedException {
		List<GUIHistory> hits = testSubject.search(null, null, null, 100, null, new ArrayList<>(), null);
		assertEquals(7, hits.size());

		hits = testSubject.search(null, null, null, 100, null, new ArrayList<>(), 5L);
		assertEquals(3, hits.size());

		hits = testSubject.search(1L, new Date(), new Date(), 100, "unxisting",
				List.of(DocumentEvent.STORED.toString()), 5L);
		assertEquals(0, hits.size());
	}

	@Test
	public void testSearchApiCalls() throws ServerException, InterruptedException {
		List<GUIHistory> hits = testSubject.searchApiCalls(null, null, null, null, null, null, 100);
		assertEquals(7, hits.size());

		hits = testSubject.searchApiCalls(1L, new Date(), new Date(), "unexisting", "soap", "boh", 100);
		assertEquals(0, hits.size());
	}

	@Test
	public void testGetStatistics() throws ServerException {
		List<List<GUIParameter>> stats = testSubject.getStatistics("en");
		assertEquals(5, stats.size());
	}

	@Test
	public void testUninstallPlugin() throws ServerException {
		File pluginFile = new File("target/tests-plugins/logicaldoc-dummy-plugin.jar");
		assertTrue(pluginFile.exists());
		assertEquals(2, testSubject.getPlugins().size());
		testSubject.uninstallPlugin("logicaldoc-dummy");

		pluginFile = new File("target/tests-plugins/logicaldoc-dummy-plugin.jar");
		assertFalse(pluginFile.exists());
	}

	@Test
	public void testInitializePlugin() throws ServerException {
		try {
			assertEquals(2, testSubject.getPlugins().size());
			testSubject.initializePlugin("logicaldoc-dummy");
		} catch (ServerException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSaveLogger() throws ServerException {
		try {
			assertFalse(new LogConfigurator().getLoggers().stream()
					.anyMatch(l -> "pippo.pluto".equals(l.getAttributeValue("name"))));
			testSubject.saveLogger("pippo.pluto", "warn", true);
			assertTrue(new LogConfigurator().getLoggers().stream()
					.anyMatch(l -> "pippo.pluto".equals(l.getAttributeValue("name"))));
		} finally {
			testSubject.removeLogger("pippo.pluto");
		}
	}

	@Test
	public void testInstallPlugin() throws ServerException, IOException, PluginException {
		testSubject.uninstallPlugin("logicaldoc-dummy");

		File libFolder = new File(new File(SystemServiceImpl.defaultWebappRootFolder, "WEB-INF"), "lib");
		libFolder.mkdirs();

		File pluginFile = new File("target/logicaldoc-dummy-8.9.2-plugin.zip");
		FileUtil.copyResource("/" + pluginFile.getName(), pluginFile);

		Map<String, File> uploadedFilesMap = new HashMap<>();
		uploadedFilesMap.put(pluginFile.getName(), pluginFile);
		session.getDictionary().put(UploadServlet.RECEIVED_FILES, uploadedFilesMap);

		testSubject.installPlugin();

		File jarFile = new File(libFolder, pluginFile.getName().replace(".zip", ".jar"));
		assertTrue(jarFile.exists());
	}

	@Test
	public void testUnscheduleJobs() throws SchedulerException, ServerException {
		JobManager jobManager = (JobManager) Context.get().getBean(JobManager.class);
		jobManager.schedule(new AbstractJob("Dummy", "xyz") {

			@Override
			public void execute(JobExecutionContext arg0) throws JobExecutionException {
				while (true)
					System.out.println("hello");
			}
		}, Map.of(), DateUtils.addDays(new Date(), 1));

		String triggerName = jobManager.getTriggers("xyz", null).get(0).getKey().getName();
		testSubject.unscheduleJobs(List.of(new GUIValue(triggerName, "xyz")));
		assertEquals(0, jobManager.getTriggers("xyz", null).size());
	}
}