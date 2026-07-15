#!/bin/bash

#  Copyright (C) 2026 ReviveMii Project & TheErrorExe, All rights reserved.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.


set -e

FFDEC_VERSION="26.2.1"

echo "checking if ffdec_lib is already installed in local repo..."

if [ -f "$(pwd)/lib/com/jpexs/ffdec_lib/${FFDEC_VERSION}/ffdec_lib-${FFDEC_VERSION}.jar" ]; then
    echo "ffdec_lib is already installed, skipping..."
else
    echo "ffdec_lib not found, downloading..."

    TMPDIR=$(mktemp -d)
    trap "rm -rf '$TMPDIR'" EXIT

    wget -q --show-progress \
        "https://github.com/jindrapetrik/jpexs-decompiler/releases/download/version${FFDEC_VERSION}/ffdec_lib_${FFDEC_VERSION}.zip" \
        -O "$TMPDIR/ffdec_lib.zip"

    echo "extracting ffdec_lib.zip..."
    unzip -q "$TMPDIR/ffdec_lib.zip" -d "$TMPDIR"

    echo "installing all jars into local repo..."
    for JAR in "$TMPDIR"/*.jar; do
        ARTIFACT=$(basename "$JAR" .jar)
        if [ "$ARTIFACT" = "ffdec_lib" ]; then
            mvn install:install-file \
                -Dfile="$JAR" \
                -DgroupId="com.jpexs" \
                -DartifactId="ffdec_lib" \
                -Dversion="$FFDEC_VERSION" \
                -Dpackaging=jar \
                -DlocalRepositoryPath="$(pwd)/lib" \
                -q
        else
            mvn install:install-file \
                -Dfile="$JAR" \
                -DgroupId="com.jpexs.libs" \
                -DartifactId="$ARTIFACT" \
                -Dversion="$FFDEC_VERSION" \
                -Dpackaging=jar \
                -DlocalRepositoryPath="$(pwd)/lib" \
                -q
        fi
        echo "  installed: $ARTIFACT"
    done

    echo "all jars installed successfully. cleaning up..."
fi

echo "building..."
mvn package -q
echo "building was successful!"
