package com.springdeveloper.cloud.shell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultHistoryFileNameProvider;
import org.springframework.stereotype.Component;

/**
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HistoryFileNameProvider extends DefaultHistoryFileNameProvider {

	public String getHistoryFileName() {
		return "cloud-shell.log";
	}

	@Override
	public String name() {
		return "Cloud Shell";
	}

}
