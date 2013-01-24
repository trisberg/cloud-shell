package com.springdeveloper.cloud.shell;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.RestLogEntry;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Staging;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Component
public class CloudFoundryCommands implements CommandMarker {

	public static String VERSION = "0.1.1";

	CloudFoundryClient client;
	String target = "https://api.cloudfoundry.com";
	boolean v1 = true;
	List<RestLogEntry> logEntries = new ArrayList<RestLogEntry>();

	@CliAvailabilityIndicator({"cf info", "cf target"})
	public boolean isAlwaysAvailable() {
		return true;
	}

	@CliAvailabilityIndicator({"cf login"})
	public boolean isNotLoggedIn() {
		if (client == null) {
			return true;
		} else {
			return false;
		}
	}

	@CliAvailabilityIndicator({"cf logout", "cf apps", "cf push", "cf start", "cf stop", "cf delete", "cf restlog",
							"cf services", "cf map", "cf unmap",
							"cf create-service", "cf delete-service", "cf bind-service", "cf unbind-service"})
	public boolean isLoggedIn() {
		if (client != null) {
			return true;
		} else {
			return false;
		}
	}

	@CliCommand(value = "cf info", help = "Show cloud info")
	public String info() {
		String info = "Not available.";
		try {
			CloudFoundryClient infoClient = new CloudFoundryClient(new URL(target));
			info =  target + "\n" +
					infoClient.getCloudInfo().getDescription() + "\n" +
					"version: " + infoClient.getCloudInfo().getVersion();
		} catch (MalformedURLException e) {
		}
		return info;
	}

	@CliCommand(value = "cf target", help = "Target a cloud provider")
	public String target(
		@CliOption(key = { "uri" }, mandatory = false, unspecifiedDefaultValue = "https://api.cloudfoundry.com",
				help = "The uri to target") final String uri) {
		try {
			CloudFoundryClient infoClient = new CloudFoundryClient(new URL(uri));
			if (infoClient.getCloudInfo().getCloudControllerMajorVersion().equals(CloudInfo.CC_MAJOR_VERSION.V1)) {
				this.v1 = true;
			} else {
				this.v1 = false;
			}
			this.target = uri;
			return "Target set to " + uri;
		} catch (MalformedURLException e) {
			return "Invalid URI: " + uri;
		}
	}

	@CliCommand(value = "cf login", help = "Log-in to a cloud provider")
	public String login(
		@CliOption(key = { "email" }, mandatory = true, help = "User email") final String user,
		@CliOption(key = { "password" }, mandatory = true, help = "Password") final String passwd,
		@CliOption(key = { "org" }, mandatory = false,
				help = "The org to target") final String org,
		@CliOption(key = { "space" }, mandatory = false, unspecifiedDefaultValue = "development",
				help = "The space to target") final String space){
		CloudCredentials credentials = new CloudCredentials(user, passwd);
		CloudSpace sessionSpace = null;
		try {
			this.client = new CloudFoundryClient(credentials, new URL(target));
			this.client.login();
			if (!v1) {
				if (org != null && space != null) {
					for (CloudSpace s : this.client.getSpaces()) {
						if (s.getOrganization().getName().equals(org) && s.getName().equals(space)) {
							sessionSpace = s;
							break;
						}
					}
					this.client.logout();
					this.client = null;
					if (sessionSpace != null) {
						this.client = new CloudFoundryClient(credentials, new URL(target), sessionSpace);
						this.client.login();
					} else {
						return "Problem while connecting to " + target + " :: Couldn't find org and space.";
					}
				} else {
					return "Problem while connecting to " + target + " :: You must specify org and space.";
				}
			}
			logEntries.clear();
			this.client.registerRestLogListener(new RestLogCallback() {
				@Override
				public void onNewLogEntry(RestLogEntry logEntry) {
					logEntries.add(logEntry);
				}
			});
		} catch (Exception e) {
			return "Error while connecting to " + target + " :: " + e.getMessage();
		}
		if (sessionSpace != null) {
			return "Connected to " + target + " using " + user + " :: " + passwd + " org/space: " +
					sessionSpace.getOrganization().getName() + "/" + sessionSpace.getName();
		} else {
			return "Connected to " + target + " using " + user + " :: " + passwd;
		}
	}

	@CliCommand(value = "cf logout", help = "Log-out from a cloud provider")
	public String logout() {
		try {
			this.client.logout();
			this.client = null;
		} catch (RuntimeException e) {
			return "Error while disconnecting from " + target + " :: " + e.getMessage();
		}
		return "Disconnecting from " + target ;
	}

	@CliCommand(value = "cf apps", help = "List apps")
	public String apps() {
		String appList = "";
		try {
			List<CloudApplication> apps = this.client.getApplications();
			for (CloudApplication app : apps) {
				appList = appList + (appList.length() > 0 ? "\n" : "" ) + app.getName() +
						" " + app.getState().name() +
						" " + app.getInstances() + " x " + app.getMemory() + "M" +
						" " + app.getStaging().getRuntime() +
						" " + app.getUris() +
						" " + app.getServices();
			}
		} catch (RuntimeException e) {
			return "Error while getting apps from " + target + " :: " + e.getMessage();
		}
		return appList;
	}

	@CliCommand(value = "cf services", help = "List services")
	public String services() {
		String svcList = "";
		try {
			List<CloudService> svcs = this.client.getServices();
			for (CloudService svc : svcs) {
				svcList = svcList + (svcList.length() > 0 ? "\n" : "" ) + svc.getName() +
						" " + (svc.getLabel() != null ? svc.getLabel() : svc.getVendor()) +
						" " + svc.getVersion();
			}
		} catch (RuntimeException e) {
			return "Error while getting services from " + target + " :: " + e.getMessage();
		}
		return svcList;
	}

	@CliCommand(value = "cf delete", help = "Delete an app")
	public String delete(
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name) {
		try {
			this.client.deleteApplication(name);
		} catch (Exception e) {
			return getClientError(e, "Error while deleting app on " + target);
		}
		return "App deleted.";
	}

	@CliCommand(value = "cf start", help = "Start an app")
	public String start(
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name) {
		try {
			this.client.startApplication(name);
		} catch (Exception e) {
			return getClientError(e, "Error while starting app on " + target);
		}
		return "App started.";
	}

	@CliCommand(value = "cf stop", help = "Stop an app")
	public String stop(
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name) {
		try {
			this.client.stopApplication(name);
		} catch (Exception e) {
			return getClientError(e, "Error while stopping app on " + target);
		}
		return "App stopped.";
	}

	@CliCommand(value = "cf push", help = "Push an app")
	public String push(
			@CliOption(key = { "", "file" }, help = "The app file to push", mandatory = true) final File app,
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name,
			@CliOption(key = { "runtime" }, help = "The app runtime", mandatory = true) final Runtimes runtime,
			@CliOption(key = { "framework" }, help = "The app framework", mandatory = true) final Frameworks framework,
			@CliOption(key = { "memory" }, help = "The app framework", mandatory = true) final Memory memory,
			@CliOption(key = { "plan" }, help = "The app plan", mandatory = false, unspecifiedDefaultValue = "free") final String plan) {
		try {
			Staging staging = new Staging(runtime.name() ,framework.name());
			List<String> urls = new ArrayList<String>();
			List<String> services = new ArrayList<String>();
			this.client.createApplication(name, staging, memory.getMemory(), urls, services, plan);
			this.client.uploadApplication(name, app);
		} catch (Exception e) {
			return getClientError(e, "Error while pushing app to " + target);
		}
		return "App deployed.";
	}

	@CliCommand(value = "cf map", help = "Map a uri to an app")
	public String map(
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name,
			@CliOption(key = { "uri" }, help = "The app uri to map", mandatory = true) final String uri) {
		try {
			List<String> uris = new ArrayList<String>(this.client.getApplication(name).getUris());
			if (!uris.contains(uri)) {
				uris.add(uri);
			}
			this.client.updateApplicationUris(name, uris);
		} catch (Exception e) {
			return getClientError(e, "Error while mapping uri to " + name + " on " + target);
		}
		return "URI mapped.";
	}

	@CliCommand(value = "cf unmap", help = "Unmap a uri from an app")
	public String unmap(
			@CliOption(key = { "name" }, help = "The app name", mandatory = true) final String name,
			@CliOption(key = { "uri" }, help = "The app uri to un-map", mandatory = true) final String uri) {
		try {
			List<String> uris = new ArrayList<String>(this.client.getApplication(name).getUris());
			if (uris.contains(uri)) {
				uris.remove(uri);
			} else {
				return "Error while un-mapping uri " + uri + ", it is not bound to app " + name;
			}
			this.client.updateApplicationUris(name, uris);
		} catch (Exception e) {
			return getClientError(e, "Error while un-mapping uri from " + name + " on " + target);
		}
		return "URI un-mapped.";
	}

	@CliCommand(value = "cf create-service", help = "Create a service")
	public String createService(
			@CliOption(key = { "name" }, help = "The service name", mandatory = true) final String name,
			@CliOption(key = { "offering" }, help = "The service offering type", mandatory = true) final String offering,
			@CliOption(key = { "plan" }, help = "The service plan", mandatory = false) final String plan,
			@CliOption(key = { "version" }, help = "The service version", mandatory = true) final String version) {
		try {
			CloudService service = new CloudService(CloudService.Meta.defaultMeta(), name);
			if (v1) {
				service.setTier("free");
				service.setType("database");
				if (version != null) {
					service.setVersion(version);
				}
				service.setVendor(offering);
			} else {
				service.setProvider("core");
				service.setLabel(offering);
				if (version != null) {
					service.setVersion(version);
				}
				service.setPlan(plan == null ? "100" : plan);
			}
			this.client.createService(service);
		} catch (Exception e) {
			return getClientError(e, "Error while creating service of type " + offering + " on " + target);
		}
		return "Service created.";
	}

	@CliCommand(value = "cf delete-service", help = "Delete a service")
	public String deleteService(
			@CliOption(key = { "name" }, help = "The service name", mandatory = true) final String name) {
		try {
			this.client.deleteService(name);
		} catch (Exception e) {
			return getClientError(e, "Error while deleting service " + name + " on " + target);
		}
		return "Service deleted.";
	}

	@CliCommand(value = "cf bind-service", help = "Bind a service to an app")
	public String bind(
			@CliOption(key = { "app" }, help = "The app name", mandatory = true) final String name,
			@CliOption(key = { "service" }, help = "The service to bind to app", mandatory = true) final String service) {
		try {
			List<String> services = new ArrayList<String>(this.client.getApplication(name).getServices());
			if (!services.contains(service)) {
				services.add(service);
			}
			this.client.updateApplicationServices(name, services);
		} catch (Exception e) {
			return getClientError(e, "Error while binding service to " + name + " on " + target);
		}
		return "Service bound.";
	}

	@CliCommand(value = "cf unbind-service", help = "Un-bind a service from an app")
	public String unbind(
			@CliOption(key = { "app" }, help = "The app name", mandatory = true) final String name,
			@CliOption(key = { "service" }, help = "The service to un-bind from app", mandatory = true) final String service) {
		try {
			List<String> services = new ArrayList<String>(this.client.getApplication(name).getServices());
			if (services.contains(service)) {
				services.remove(service);
			} else {
				return "Error while un-binding service " + service + ", it is not bound to app " + name;
			}
			this.client.updateApplicationServices(name, services);
		} catch (Exception e) {
			return getClientError(e, "Error while unbinding service from " + name + " on " + target);
		}
		return "Service un-bound.";
	}

	@CliCommand(value = "cf restlog", help = "Print or clear REST logs")
	public String restlogs(
		@CliOption(key = { "print" }, mandatory = false, help = "Print log entries", unspecifiedDefaultValue = "true") final boolean print,
		@CliOption(key = { "clear" }, mandatory = false, help = "Clear log entries", unspecifiedDefaultValue = "false") final boolean clear) {
		StringBuilder logOutput = new StringBuilder();
		if (print) {
			for (RestLogEntry entry : logEntries) {
				if (logOutput.length() > 0) {
					logOutput.append("\n");
				}
				logOutput.append(">> [" + entry.getHttpStatus() + "] " + " " + entry.getMethod() + " " + entry.getUri() + " :: " + entry.getMessage());
			}
		}
		if (clear) {
			logEntries.clear();
			if (logOutput.length() == 0) {
				logOutput.append("Cleared logs.");
			}
		}
		return logOutput.toString();
	}

	private String getClientError(Exception e, String errorText) {
		if (e instanceof CloudFoundryException) {
			return  errorText + " :: " + e.getMessage() +
					" (" + ((CloudFoundryException)e).getDescription() + ")";
		} else {
			return errorText + e.getMessage();
		}
	}

	enum Runtimes {
		java, java7;
	}

	enum Frameworks {
		spring, grails;
	}

	enum Memory {
		M128("128"),
		M256("256"),
		M512("512");

		private String mem;

		private Memory(String mem){
			this.mem = mem;
		}

		public int getMemory(){
			return Integer.valueOf(mem);
		}
	}
}
