@rem ----------------------------------------------------------------------------
@rem Licensed to the Apache Software Foundation (ASF) under one
@rem or more contributor license agreements.  See the NOTICE file
@rem distributed with this work for additional information
@rem regarding copyright ownership.  The ASF licenses this file
@rem to you under the Apache License, Version 2.0 (the
@rem "License"); you may not use this file except in compliance
@rem with the License.  You may obtain a copy of the License at
@rem
@rem    https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing,
@rem software distributed under the License is distributed on an
@rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@rem KIND, either express or implied.  See the License for the
@rem specific language governing permissions and limitations
@rem under the License.
@rem ----------------------------------------------------------------------------

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Maven Start Up Batch script
@rem
@rem  Required ENV vars:
@rem ------------------
@rem   JAVA_HOME - location of a JDK home dir
@rem
@rem Optional ENV vars
@rem -----------------
@rem   M2_HOME - location of maven2's installed home dir
@rem   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@rem     e.g. to debug Maven itself, use
@rem       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@rem   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@rem ---------------------------------------------------------------------------

@if "%MAVEN_SKIP_RC%"=="" @if exist "%HOME%\mavenrc_pre.bat" call "%HOME%\mavenrc_pre.bat"
@if exist "%HOME%\mavenrc.bat" call "%HOME%\mavenrc.bat"
@if exist "%HOME%\mavenrc_post.bat" call "%HOME%\mavenrc_post.bat"

@setlocal

set WRAPPER_JAR="%USERPROFILE%\.m2\wrapper\maven-wrapper.jar"
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@if exist "%WRAPPER_JAR%" goto run

echo Downloading Maven Wrapper JAR...
if exist "%USERPROFILE%\.m2\wrapper\" (
    if not exist "%USERPROFILE%\.m2\wrapper\maven-wrapper.jar" (
        bitsadmin /transfer "maven-wrapper" "%WRAPPER_URL%" "%WRAPPER_JAR%"
    )
) else (
    mkdir "%USERPROFILE%\.m2\wrapper"
    bitsadmin /transfer "maven-wrapper" "%WRAPPER_URL%" "%WRAPPER_JAR%"
)
if not exist "%WRAPPER_JAR%" (
    echo Error: Maven Wrapper JAR not found at %WRAPPER_JAR%
    exit /b 1
)

:run
@REM Execute Maven
"%JAVA_HOME%\bin\java.exe" %MAVEN_OPTS% -jar "%WRAPPER_JAR%" %*
:end
@endlocal
