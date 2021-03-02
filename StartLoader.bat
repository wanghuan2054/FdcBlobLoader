@echo off
set CLASSPATH=%classpath%
set CLASSPATH=%CLASSPATH%./lib/log4j.jar
set CLASSPATH=%CLASSPATH%;./lib/ojdbc6.jar
set CLASSPATH=%CLASSPATH%;./lib/HikariCP-1.3.9.jar
set CLASSPATH=%CLASSPATH%;./lib/slf4j-log4j12.jar
set CLASSPATH=%CLASSPATH%;./lib/slf4j-api-1.5.5.jar

set CLASSPATH=%CLASSPATH%;./lib/c3p0-0.9.0.jar
set CLASSPATH=%CLASSPATH%;./lib/commons-collections4-4.1.jar
set CLASSPATH=%CLASSPATH%;./lib/commons-dbcp-1.4.jar
set CLASSPATH=%CLASSPATH%;./lib/commons-pool-1.6.jar
set CLASSPATH=%CLASSPATH%;./lib/druid-1.1.6.jar
set CLASSPATH=%CLASSPATH%;./lib/javassist-3.22.0-GA.jar
set CLASSPATH=%CLASSPATH%;./lib/mchange-commons-java-0.2.11.jar
set CLASSPATH=%CLASSPATH%;./lib/tomcat-jdbc.jar
set CLASSPATH=%CLASSPATH%;./lib/tomcat-juli.jar
set CLASSPATH=%CLASSPATH%;./config;./blobData;./datapool

set STARTTIME=20210301 090000
::set ENDTIME=20180101 060000
set XMS=1G
set XMX=2G
set DATE=%Date:~0,4%%Date:~5,2%%Date:~8,2%%Time:~0,2%

echo ========== FDC BlobLoader : From %STARTTIME% is Starting ==========

::java -DSTARTTIME="%STARTTIME%" -cp %CLASSPATH% blobData.MainProgram
java -DSTARTTIME="%STARTTIME%" -server -Xmx%XMX% -Xms%XMS% -verbose:gc -Xloggc:GCLOG/GC_BlobLoader_%DATE%.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+UseParallelOldGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=HeapDump/BlobLoader_%DATE%.hprof -cp %classpath% blobData.MainProgram
pause