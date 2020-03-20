#!/usr/bin/env bash
set -e 

JVM_OPTS="-Xms${HEAP_SIZE} -Xmx${HEAP_SIZE}"
if [ "${JAVA_ENABLED_DEBUG}" = "true" ]; then 
    JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" 
fi

echo java $JVM_OPTS -jar ${JAR_FILE}
java ${JVM_OPTS} -jar ${JAR_FILE}