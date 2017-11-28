#!/bin/bash
source /etc/profile.d/proxy.sh
env
make image-format=${image-format} buildNumber=${buildNumber} -j --no-print-directory -f makefiles/master.mk
