package net.logstash.log4j

import groovy.json.JsonBuilder;
import net.logstash.log4j.data.HostData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;


public class JSONGroovyEventLayout extends Layout {

    protected boolean locationInfo = false;
    protected boolean ignoreThrowable = false;
    protected boolean activeIgnoreThrowable = ignoreThrowable;
    protected String hostname = new HostData().getHostName();

    protected String ndc;
    protected Map mdc;


    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONGroovyEventLayout() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONGroovyEventLayout(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String format(LoggingEvent loggingEvent) {


        mdc = loggingEvent.getProperties();
        ndc = loggingEvent.getNDC();

//        logstashEvent = new JSONObject();
		def json = [
			"@source_host"	: hostname,
			"@message": loggingEvent.getRenderedMessage(),
			"@timestamp": dateFormat(loggingEvent.getTimeStamp()),
			"@fields": [
					loggerName: loggingEvent.getLoggerName(),
					ndc : ndc,
					level: loggingEvent.getLevel().toString(),
					threadName: loggingEvent.getThreadName()
			]
		 ]
		mdc?.each{key, value ->
			json."@fields"."$key" = value
		}


        if (loggingEvent.getThrowableInformation()) {
			def exceptionInformation = [:]
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                exceptionInformation.exception_class =  throwableInformation.getThrowable().getClass().getCanonicalName()
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.exception_message = throwableInformation.getThrowable().getMessage()
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(), "\n");
                exceptionInformation.stacktrace =  stackTrace
            }
			json."@fields".exception = exceptionInformation
        }

        if (locationInfo) {
			retrieveLocationInformation(loggingEvent, json."@fields");
        }

		def builder = new JsonBuilder(json)



        return builder.toString() + "\n";
    }

	/**
	 * We can't rely on log4j to provide us the correct location information
	 * Solution is based on http://stackoverflow.com/questions/18070863/grails-logging-is-there-any-existing-solution-to-be-able-to-log-the-file-lin/18198442#18198442
	 *
	 * @param loggingEvent
	 */
	protected void retrieveLocationInformation(LoggingEvent loggingEvent, Map fields) {
		StackTraceElement[] strackTraceElements = Thread.currentThread().getStackTrace();

		StackTraceElement targetStackTraceElement = null;
		for (int i = 0; i < strackTraceElements.length; i++) {
			StackTraceElement strackTraceElement = strackTraceElements[i];
			if (strackTraceElement != null &&
					strackTraceElement.declaringClass != null &&
					strackTraceElement.declaringClass.startsWith("org.apache.commons.logging.Log") &&
					i < (strackTraceElements.length - 1)) {
				targetStackTraceElement = strackTraceElements[++i];
				while (targetStackTraceElement.declaringClass != null &&
						targetStackTraceElement.declaringClass.startsWith("org.codehaus.groovy.runtime.callsite.") &&
						i < (strackTraceElements.length - 1)) {
					targetStackTraceElement = strackTraceElements[++i];
				}
				break;
			}
		}

		if (targetStackTraceElement) {
			fields.file =  targetStackTraceElement.getFileName()
			fields.line_number =  targetStackTraceElement.getLineNumber()
			fields.class = targetStackTraceElement.getClassName()
			fields.method = targetStackTraceElement.getMethodName()
		}
	}

	public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }


}
