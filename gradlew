#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0
#

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then PRG="$link"
  else PRG=`dirname "$PRG"`"/$link"; fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2; exit 1
  fi
else
  JAVACMD="java"
  command -v java >/dev/null 2>&1 || { echo "ERROR: JAVA_HOME not set and java not found in PATH." >&2; exit 1; }
fi

GRADLE_OPTS="${GRADLE_OPTS:-} $DEFAULT_JVM_OPTS"

exec "$JAVACMD" $GRADLE_OPTS $JAVA_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
