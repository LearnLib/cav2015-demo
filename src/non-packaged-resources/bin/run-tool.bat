@echo off

if not defined JVM_ARGS (
	set JVM_ARGS= 
)

for %%X in (java.exe) do (set JAVA=%%~$PATH:X)
if not defined JAVA (
	if not defined JAVA_HOME (
		echo No java.exe found in path, and JAVA_HOME variable is not set. Exiting ...
		exit 1
	)
	set JAVA=%JAVA_HOME%\bin\java.exe
)

%JAVA% %JVM_ARGS% -Dcli.tool=%TOOL% -jar %~dp0\..\learnlib-cav2015.jar %*
