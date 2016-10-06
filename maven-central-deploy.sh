#!/usr/bin/env bash

# Deploy maven artifact in current directory into Maven central repository
# using maven-release-plugin goals

read -p "Really deploy to maven central repository  (yes/no)? "
[ "$REPLY" != "yes" ] && echo "Didn't deploy anything" && exit 1

mvn -Prelease release:clean release:prepare release:perform --batch-mode -e | tee maven-central-deploy.log
