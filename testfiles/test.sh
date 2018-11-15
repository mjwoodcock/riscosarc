#!/bin/bash

while read -r src dest; do
	java riscosarc -x $src
	if [ ! -e $dest ]; then
		echo "$src failed"
		echo "$dest does not exist."
	fi

	if ! diff $dest source/19337.txt > /dev/null 2>&1; then
		echo "$src failed"
	fi

	rm -f $dest
done < manifest
