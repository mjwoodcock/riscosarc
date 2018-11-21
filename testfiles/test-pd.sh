#!/bin/bash

rm -f failed-pd.txt

for f in `cat manifest.pd`; do
	echo "Testing $f"
	if ! java riscosarc -x -d/tmp $f; then
		echo "Failed to extract $f"
		echo "$f" >> failed-pd.txt
	fi
done
