#!/bin/bash

SCRIPTDIR=`dirname "$0"`
JAR_FILE="$SCRIPTDIR/../learnlib-cav2015.jar"
TOOL_NAME="`basename $0`"

(declare -p JVM_ARGS 2>/dev/null | grep -q 'declare -a') || JVM_ARGS=($JVM_ARGS)

java "${JVM_ARGS[@]}" -Dcli.tool="$TOOL_NAME" -jar "$JAR_FILE" "$@"
