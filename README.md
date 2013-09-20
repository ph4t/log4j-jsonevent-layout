# Logstash `json_event` pattern for log4j - groovy edition



## What is it?
The [original library](https://github.com/logstash/log4j-jsonevent-layout) has a few drawbacks when used in grails/groovy projects:
- file name, line number, class name & method name are wrong. This is a known issue in log4j appenders.
- external json library is obsolete due to groovy `JsonBuilder`.

Main differences compared to the original code are:
- outputs correct location information (thanks to [this post](http://stackoverflow.com/questions/18070863/grails-logging-is-there-any-existing-solution-to-be-able-to-log-the-file-lin))
- switched from an external json library to JsonBuilder
- MDC key/value pairs are added directly to `@fields`
- gradle instead of maven
- v1 version removed

## Grails Usage
Complete documentation is provided by [original library](https://github.com/logstash/log4j-jsonevent-layout)
To use this library inside a grails project:

1.  Build this library (`gradle build`) and move the produced `jar` file to the `lib` folder
2.  Define the appender inside `Config.groovy`:
3.  
	log4j = {
    	appenders {
    		...
    		rollingFile name: "logstash", maxFileSize: '100MB', file: "logstash.log", layout: (new net.logstash.log4j.JSONGroovyEventLayout())
    		...
    	}
    	//don't forget to use the new appender


Sample logstash config:

	input {
      file {
        path => ["C:/workspace/planG/logstash.log" ]
    	codec =>   json {
    	}
      }
    }
