package muon.app;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import muon.app.ssh.GraphicalInputBlocker;
import muon.app.ssh.InputBlocker;
import muon.app.ui.AppWindow;
import muon.app.ui.components.session.ExternalEditorHandler;
import muon.app.ui.components.session.SessionContentPanel;
import muon.app.ui.laf.AppSkin;
import muon.app.ui.laf.AppSkinDark;
import muon.app.ui.laf.AppSkinLight;
import util.CollectionHelper;

/**
 * Hello world!
 *
 */
public class App {
	public static final String CONFIG_DIR = System.getProperty("user.home") + File.separatorChar + "muon-ssh";
	public static final String SESSION_DB_FILE = "session-store.json";
	public static final String CONFIG_DB_FILE = "settings.json";
	public static final String SNIPPETS_FILE = "snippets.json";
	public static final String PINNED_LOGS = "pinned-logs.json";
	private static Settings settings;
	public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
	public static final SnippetManager SNIPPET_MANAGER = new SnippetManager();
	private static InputBlocker inputBlocker;
	private static ExternalEditorHandler externalEditorHandler;
	private static AppWindow mw;
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH)
			.startsWith("mac");
	public static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH)
			.contains("windows");
	private static Map<String, List<String>> pinnedLogs = new HashMap<>();

	public static void main(String[] args) throws UnsupportedLookAndFeelException {

		Security.setProperty("networkaddress.cache.ttl", "0");
		Security.setProperty("networkaddress.cache.negative.ttl", "0");

		Security.addProvider(new BouncyCastleProvider());
		File appDir = new File(CONFIG_DIR);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}
		loadSettings();

		SKIN = settings.isUseGlobalDarkTheme() ? new AppSkinDark() : new AppSkinLight();

		UIManager.setLookAndFeel(SKIN.getLaf());

		// JediTerm seems to take a long time to load, this might make UI more
		// responsive
		App.EXECUTOR.submit(() -> {
			try {
				Class.forName("com.jediterm.terminal.ui.JediTermWidget");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		});

		mw = new AppWindow();
		inputBlocker = new GraphicalInputBlocker(mw);
		externalEditorHandler = new ExternalEditorHandler(mw);
		SwingUtilities.invokeLater(() -> {
			mw.setVisible(true);
		});
	}

	public synchronized static void loadSettings() {
		File file = new File(CONFIG_DIR, CONFIG_DB_FILE);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		if (file.exists()) {
			try {
				settings = objectMapper.readValue(file, new TypeReference<Settings>() {
				});
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		settings = new Settings();
	}

	public synchronized static void saveSettings() {
		File file = new File(CONFIG_DIR, CONFIG_DB_FILE);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(file, settings);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized static Settings getGlobalSettings() {
		return settings;
	}

	public static AppSkin SKIN;// = new AppSkinDark();
	// public static final AppSkin SKIN = new AppSkinLight();

	/**
	 * @return the inputBlocker
	 */
	public static InputBlocker getInputBlocker() {
		return inputBlocker;
	}

	/**
	 * @return the externalEditorHandler
	 */
	public static ExternalEditorHandler getExternalEditorHandler() {
		return externalEditorHandler;
	}

	public static SessionContentPanel getSessionContainer(int activeSessionId) {
		return mw.getSessionListPanel().getSessionContainer(activeSessionId);
	}

	/**
	 * @return the pinnedLogs
	 */
	public static Map<String, List<String>> getPinnedLogs() {
		return pinnedLogs;
	}

	public synchronized static void loadPinnedLogs() {
		File file = new File(CONFIG_DIR, PINNED_LOGS);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		if (file.exists()) {
			try {
				pinnedLogs = objectMapper.readValue(file, new TypeReference<Map<String, List<String>>>() {
				});
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		pinnedLogs = new HashMap<String, List<String>>();
	}

	public synchronized static void savePinnedLogs() {
		File file = new File(CONFIG_DIR, PINNED_LOGS);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(file, pinnedLogs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
