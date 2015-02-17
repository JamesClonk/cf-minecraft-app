#!/bin/bash

echo "Checking minecraft version JSON... "
versions_url="http://s3.amazonaws.com/Minecraft.Download/versions/versions.json"
versions_file="/tmp/minecraft_versions.json"
wget --trust-server-names --no-check-certificate -O $versions_file $versions_url
latest_version=$(cat $versions_file | egrep '^[[:space:]]*"release"' | cut -d':' -f2 | egrep -o '[[:digit:].]*')
if [[ -n "$latest_version" ]]; then
	echo "Latest minecraft version: $latest_version"
	wget --trust-server-names --no-check-certificate -O "lib/minecraft_server.jar" "https://s3.amazonaws.com/Minecraft.Download/versions/$latest_version/minecraft_server.$latest_version.jar"
else
	wget --trust-server-names --no-check-certificate -O "lib/minecraft_server.jar" "https://s3.amazonaws.com/Minecraft.Download/versions/1.8.1/minecraft_server.1.8.1.jar"
fi

# TODO: run ant build scripts here to build fat jar of cf-minecraft-app
# TODO
# TODO
# TODO
# TODO
