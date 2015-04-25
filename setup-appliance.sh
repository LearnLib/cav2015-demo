#!/bin/bash

info() {
	echo "$@"
}

eerror() {
	echo >&2 "$@"
}

die() {
	eerror "$@"
	exit 1
}

ROOT="${HOME}/LearnLib-Demo"

DOWNLOADS="${ROOT}/downloads"
LOCAL="${ROOT}/local"

# Apache Maven
MAVEN_VERSION="3.3.1"
MAVEN_BASENAME="apache-maven-${MAVEN_VERSION}"
MAVEN_INSTALL="${LOCAL}/${MAVEN_BASENAME}"
MAVEN_BIN="${MAVEN_INSTALL}/bin"
MVN="${MAVEN_BIN}/mvn"


SOURCES="${ROOT}/Sources"

AUTOMATALIB_SRC="${SOURCES}/automatalib"
AUTOMATALIB_URL="https://github.com/misberner/automatalib.git"
AUTOMATALIB_TAG="develop" # TODO change

LEARNLIB_SRC="${SOURCES}/learnlib"
LEARNLIB_URL="https://github.com/LearnLib/learnlib.git"
LEARNLIB_TAG="develop" # TODO change

LLALF_SRC="${SOURCES}/learnlib-libalf"
LLALF_URL="https://github.com/LearnLib/learnlib-libalf.git"
LLALF_TAG="develop" # TODO change

LLJL_SRC="${SOURCES}/learnlib-jlearn"
LLJL_URL="https://github.com/LearnLib/learnlib-jlearn.git"
LLJL_TAG="develop" # TODO change

DEMO_SRC="${SOURCES}/learnlib-cav2015"
DEMO_URL="https://github.com/LearnLib/learnlib-cav2015.git"
DEMO_TAG="develop" # TODO change

info "Setting up machine for LearnLib demonstration. You may be prompted for your password."
info "Press any key to begin, or press Ctrl+C to cancel."
read || die

info "Creating demonstration root $ROOT"
mkdir -p "$ROOT" || die


info "Creating downloads folder $DOWNLOADS"
mkdir -p "$DOWNLOADS" || die
info "Creating local installation directory $LOCAL"
mkdir -p "$LOCAL" || die

info "Installing GraphVIZ"
sudo apt-get -y -qq install graphviz || die

info "Installing Gnuplot"
sudo apt-get -y -qq install gnuplot || die
info "done"

info "Installing Git"
sudo apt-get -y -qq install git || die
info "done"

info "Downloading Apache Maven ${MAVEN_VERSION}"
wget -O "${DOWNLOADS}/${MAVEN_BASENAME}-bin.tar.gz" "http://mirrors.sonic.net/apache/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_BASENAME}-bin.tar.gz" || die
info "Unpacking Apache Maven ${MAVEN_VERSION} to ${MAVEN_INSTALL}"
tar xCfz "${LOCAL}" "${DOWNLOADS}/${MAVEN_BASENAME}-bin.tar.gz" || die

info "Adding Maven binary directory ${MAVEN_BIN} to PATH in ~/.bashrc"
echo 'export PATH="$PATH:'"${MAVEN_BIN}"'"' >>~/.bashrc || die
echo "Reloading ~/.bashrc"
source ~/.bashrc || die

info "Creating source folder $SOURCES"

info "Cloning AutomataLib Source Code into $AUTOMATALIB_SRC"
git clone --single-branch --depth 1 -b "$AUTOMATALIB_TAG" "$AUTOMATALIB_URL" "$AUTOMATALIB_SRC" || die

info "Building AutomataLib and installing it to the local repository"
( cd "$AUTOMATALIB_SRC" && "${MVN}" clean install ) || die

info "Cloning LearnLib Source Code into $LEARNLIB_SRC"
git clone --single-branch --depth 1 -b "$LEARNLIB_TAG" "$LEARNLIB_URL" "$LEARNLIB_SRC" || die

info "Building LearnLib and installing it into the local repository"
( cd "$LEARNLIB_SRC" && "${MVN}" clean install ) || die

info "Cloning LearnLib-LibAlf Bridge Source Code into $LLALF_SRC"
git clone --single-branch --depth 1 -b "$LLALF_TAG" "$LLALF_URL" "$LLALF_SRC" || die

info "Building LearnLib-LibAlf Bridge and installing it into the local repository"
( cd "$LLALF_SRC" && "${MVN}" clean install ) || die

info "Cloning LearnLib-JLearn Bridge Source Code into $LLJL_SRC"
git clone --single-branch --depth 1 -b "$LLJL_TAG" "$LLJL_URL" "$LLJL_SRC" || die

info "Building LearnLib-JLearn Bridge and installing it into the local repository"
( cd "$LLJL_SRC" && "${MVN}" clean install ) || die

info "Cloning CAV2015 Demo into $DEMO_SRC"
git clone --single-branch --depth 1 -b "$DEMO_TAG" "$DEMO_URL" "$DEMO_SRC" || die

info "Building CAV2015 Demo"
( cd "$DEMO_SRC" && "${MVN}" clean package )
