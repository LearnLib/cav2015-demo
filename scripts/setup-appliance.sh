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

ROOT="${HOME}/LearnLib-Demo-Files"

DEMO_HOME="${HOME}/LearnLib-Demo"

DOWNLOADS="${ROOT}/downloads"
LOCAL="${ROOT}/local"

LOCALBIN="${LOCAL}/bin"

# Apache Maven
MAVEN_VERSION="3.3.1"
MAVEN_BASENAME="apache-maven-${MAVEN_VERSION}"
MAVEN_INSTALL="${LOCAL}/${MAVEN_BASENAME}"
MAVEN_BIN="${MAVEN_INSTALL}/bin"
MVN="${MAVEN_BIN}/mvn"


SOURCES="${ROOT}/Sources"

AUTOMATALIB_SRC="${SOURCES}/automatalib"
AUTOMATALIB_URL="https://github.com/misberner/automatalib.git"
AUTOMATALIB_TAG="automatalib-0.5.2"

LEARNLIB_SRC="${SOURCES}/learnlib"
LEARNLIB_URL="https://github.com/LearnLib/learnlib.git"
LEARNLIB_TAG="learnlib-0.11.2"

LLALF_SRC="${SOURCES}/learnlib-libalf"
LLALF_URL="https://github.com/LearnLib/learnlib-libalf.git"
LLALF_TAG="learnlib-libalf-0.11.2"

LLJL_SRC="${SOURCES}/learnlib-jlearn"
LLJL_URL="https://github.com/LearnLib/learnlib-jlearn.git"
LLJL_TAG="learnlib-jlearn-0.11.2"

DEMO_SRC="${SOURCES}/learnlib-cav2015"
DEMO_URL="https://github.com/LearnLib/cav2015-demo.git"
DEMO_TAG="demo"

ECLIPSE_FILENAME="eclipse-java-luna-SR2-linux-gtk-x86_64.tar.gz"
ECLIPSE_URL="http://ftp.wh2.tu-dresden.de/pub/mirrors/eclipse/technology/epp/downloads/release/luna/SR2/${ECLIPSE_FILENAME}"
ECLIPSEPATH="${LOCAL}/eclipse"
ECLIPSEEXE="${ECLIPSEPATH}/eclipse"


info "Setting up machine for LearnLib demonstration. You may be prompted for your password."
info "Press any key to begin, or press Ctrl+C to cancel."
read || die

info "Creating demonstration root $ROOT"
mkdir -p "$ROOT" || die


info "Creating downloads folder $DOWNLOADS"
mkdir -p "$DOWNLOADS" || die
info "Creating local installation directory $LOCAL"
mkdir -p "$LOCAL" || die
mkdir -p "$LOCALBIN" || die

info "Adding local binary directory ${LOCALBIN} to PATH in ~/.bashrc"
echo 'export PATH="$PATH:'"${LOCALBIN}"'"' >>~/.bashrc || die
echo "Reloading ~/.bashrc"
source ~/.bashrc || die

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

info "Adding symlink to mvn executable"
ln -s "${MVN}" "${LOCALBIN}/mvn"

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
( cd "$DEMO_SRC" && git submodule init && git submodule update && "${MVN}" clean package  && cp -r "target/learnlib-cav2015" "${DEMO_HOME}") || die

info "All done - the CAV2015 demo should now be installed in ${DEMO_HOME}"
info "An example LearnLib project can be found in ${DEMO_HOME}/example-project"
info "Have fun trying out LearnLib!"

info " ******** "
info "Optionally, you can also download Eclipse for Java, to get started with the LearnLib examples."
info "Enter 'y' to download and install Eclipse for Java."
read -n 1 -p 'Download and install Eclipse [N/y]?' ANS
if [ "$ANS" == "y" ]; then
	info "Downloading Eclipse for Java"
	wget "$ECLIPSE_URL" -O "${DOWNLOADS}/${ECLIPSE_FILENAME}" || die
	info "Extracting Eclipse to ${ECLIPSEPATH}"
	tar xCfz "${LOCAL}" "${DOWNLOADS}/${ECLIPSE_FILENAME}"
	info "Adding symlink to Eclipse executable"
	ln -s "${ECLIPSEEXE}" "${LOCALBIN}/eclipse"
	info "Done, you should be able to start eclipse by running ${ECLIPSEEXE}."
	info "Or, re-load your .bashrc (typing 'source ~/.bashrc') and just enter 'eclipse'."
fi
