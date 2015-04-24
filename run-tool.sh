#!/bin/bash

qarray() {
	for x in "$@"; do
		echo -n "'$x' "
	done
}

READLINK=readlink
if [ "`uname`" = "Darwin" ]; then
	READLINK="greadlink"
fi

SCRIPTPATH=`$READLINK -f "$0"`
SCRIPTDIR=`dirname "$SCRIPTPATH"`

# This might need to be changed!
JAR_FILE="$SCRIPTDIR/target/learnlib-cav2015-0.11.2-SNAPSHOT.jar"

TOOL_NAME="`basename $0`"

/bin/bash -c "java $JVM_ARGS -Dcli.tool='$TOOL_NAME' -jar '$JAR_FILE' `qarray "$@"`"
