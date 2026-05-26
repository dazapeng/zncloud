#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   M2_HOME - location of maven2's installed home dir
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ] ; then
  if [ -f /usr/local/etc/mavenrc ] ; then
    . /usr/local/etc/mavenrc
  fi

  if [ -f /etc/mavenrc ] ; then
    . /etc/mavenrc
  fi

  if [ -f "$HOME/.mavenrc" ] ; then
    . "$HOME/.mavenrc"
  fi
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MINGW*) mingw=true ;;
esac

# For Cygwin or MSYS, ensure paths are in UNIX format before anything is touched
if $cygwin || $mingw ; then
  [ -n "$M2_HOME" ] && M2_HOME=$(cygpath --unix "$M2_HOME")
  [ -n "$JAVA_HOME" ] && JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
fi

# Resolve links: $0 may be a link
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")"/$link"
  fi
done

SAVED="$(pwd)"
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME="$(pwd -P)"
cd "$SAVED" >/dev/null

MAVEN_HOME="$APP_HOME/.mvn/wrapper"
MAVEN_OPTS="$(concat_lines "$APP_HOME/.mvn/jvm.config")"
MAVEN_DEBUG_OPTS=""

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
MAVEN_CMD_LINE_ARGS="$MAVEN_CONFIG $@"
export MAVEN_CMD_LINE_ARGS

wrapperJarPath="$MAVEN_HOME/maven-wrapper.jar"
wrapperDownloadUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if [ -r "$wrapperJarPath" ]; then
    echo "Maven Wrapper JAR found at $wrapperJarPath"
else
    echo "Downloading Maven Wrapper JAR..."
    if command -v wget > /dev/null; then
        wget -q -O "$wrapperJarPath" "$wrapperDownloadUrl"
    elif command -v curl > /dev/null; then
        curl -sS -o "$wrapperJarPath" "$wrapperDownloadUrl"
    else
        echo "Cannot download Maven Wrapper JAR: wget or curl not found"
        exit 1
    fi
    if [ -f "$wrapperJarPath" ]; then
        chmod 644 "$wrapperJarPath"
        echo "Maven Wrapper JAR downloaded to $wrapperJarPath"
    else
        echo "Failed to download Maven Wrapper JAR"
        exit 1
    fi
fi

if [ -z "$JAVA_HOME" ]; then
    javaToUse="java"
else
    javaToUse="$JAVA_HOME/bin/java"
fi

exec "$javaToUse" $MAVEN_OPTS $MAVEN_DEBUG_OPTS \
    -jar "$wrapperJarPath" $MAVEN_CMD_LINE_ARGS
