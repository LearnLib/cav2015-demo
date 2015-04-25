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

info "Cleaning up machine after LearnLib demonstration. You may be prompted for your password."
info "Press any key to begin, or press Ctrl+C to cancel."
read || die

info "Deleting demonstration root ${ROOT}"
rm -rf "${ROOT}" || die

info "Removing GraphVIZ"
sudo apt-get -y -qq remove graphviz || die

info "Removing Gnuplot"
sudo apt-get -y -qq remove gnuplot || die

info "Removing Git"
sudo apt-get -y -qq remove git || die

info "Restoring ~/.bashrc"
grep -v "${ROOT}" ~/.bashrc >~/.bashrc_tmp
mv ~/.bashrc_tmp ~/.bashrc
