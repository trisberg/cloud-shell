package com.springdeveloper.cloud.shell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.plugin.support.DefaultBannerProvider;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

/**
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BannerProvider extends DefaultBannerProvider
		implements CommandMarker {

	@CliCommand(value = {"version"}, help = "Displays current CLI version")
	public String getBanner() {
		StringBuffer buf = new StringBuffer();
		buf.append("  _____ _                 _    _____ _          _ _   " + OsUtils.LINE_SEPARATOR);
		buf.append(" /  ___| |               | |  /  ___| |        | | |  " + OsUtils.LINE_SEPARATOR);
		buf.append("'  '   | | ___  _   _  __| |  \\ `--.| |__   ___| | |  " + OsUtils.LINE_SEPARATOR);
		buf.append("| |    | |/ _ \\| | | |/ _' |   `--. \\ '_ \\ / _ \\ | |  " + OsUtils.LINE_SEPARATOR);
		buf.append("`  \\___| | (_) | |_| | (_| |  /\\__/ / | | |  __/ | |  " + OsUtils.LINE_SEPARATOR);
		buf.append(" \\____/|_|\\___/ \\__'_|\\__._|  \\____/|_| |_|\\___|_|_|  " + OsUtils.LINE_SEPARATOR);
		buf.append("                                                         " + OsUtils.LINE_SEPARATOR);
		buf.append(this.getVersion());
		return buf.toString();

	}

	public String getVersion() {
		return CloudFoundryCommands.VERSION;
	}

	public String getWelcomeMessage() {
		return "Welcome to Cloud Shell CLI";
	}

	@Override
	public String name() {
		return "Cloud Shell";
	}
}
