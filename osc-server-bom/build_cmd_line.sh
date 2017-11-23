#!/bin/bash
/etc/profile.d/proxy.sh
ant -Dimage-format=${image-format} -Dbranchname=${branchname} -DbuildNumber=${fullVersion} -DproductVersion=${releaseVersion}
