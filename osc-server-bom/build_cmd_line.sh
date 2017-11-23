#!/bin/bash
/etc/profile.d/proxy.sh
env
ant -Dimage-format=${image-format} -Dbranchname=${branchname} -DbuildNumber=${fullVersion} -DproductVersion=${releaseVersion}
