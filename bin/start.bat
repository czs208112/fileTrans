@echo off
set JAVA_MAX_MEM=-Xms256m -Xmx512m
java -cp .;../lib/*;../conf/ %JAVA_MAX_MEM% FileTransApplication