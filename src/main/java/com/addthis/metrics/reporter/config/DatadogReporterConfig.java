package com.addthis.metrics.reporter.config;

import com.yammer.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.*;

public class DatadogReporterConfig extends AbstractReporterConfig {

	private static final Logger log = LoggerFactory.getLogger(DatadogReporterConfig.class);

	@NotNull
	private String apiKey = null;

	@NotNull
	private List<String> expansions = Arrays.asList(
		"RATE_1_MINUTE", "P95"
	);

	private String hostName;

	private boolean vmMetrics = false;

	private String fileName = null;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public List<String> getExpansions() {
		return expansions;
	}

	public void setExpansions(List<String> expansions) {
		this.expansions = expansions;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public boolean isVmMetrics() {
		return vmMetrics;
	}

	public void setVmMetrics(boolean vmMetrics) {
		this.vmMetrics = vmMetrics;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public boolean enable() {
		String className = "smartthings.cassandra.datadog.DatadogReporter";
		if (!isClassAvailable(className)) {
			log.error("Tried to enable DatadogReporter, but class {} was not found", className);
			return false;
		}

		try {
			Set<smartthings.cassandra.datadog.DatadogReporter.Expansions> exs = new HashSet<smartthings.cassandra.datadog.DatadogReporter.Expansions>();
			for (String ex : expansions) {
				smartthings.cassandra.datadog.DatadogReporter.Expansions e = smartthings.cassandra.datadog.DatadogReporter.Expansions.valueOf(ex);
				if (ex != null) {
					exs.add(e);
				}
			}
			if (exs.size() <= 0) {
				throw new IllegalArgumentException("Must have one or more expansion");
			}
			log.info("Enabling DatadogReporter with expansions {}", exs);

			smartthings.cassandra.datadog.DatadogReporter.Builder builder = new smartthings.cassandra.datadog.DatadogReporter.Builder();
			builder.withApiKey(apiKey)
				.withMetricsRegistry(Metrics.defaultRegistry())
				.withExpansions(EnumSet.copyOf(exs))
				.withPredicate(getMetricPredicate())
				.withMetricNameFormatter(new smartthings.cassandra.datadog.PrefixReplacingFormatter("org.apache.cassandra.metrics", "cassandra"))
				.withVmMetricsEnabled(vmMetrics);
			if (hostName == null) {
				builder.withEC2Host();
			} else {
				builder.withHost(hostName);
			}
			if (this.fileName != null) {
				builder.withTransport(new smartthings.cassandra.datadog.transports.FileTransport(fileName));
			}
			builder.build().start(getPeriod(), getRealTimeunit());

		} catch (Exception e) {
			log.error("Failed to enable DatadogReporter", e);
			return false;
		}
		return true;
	}
}
