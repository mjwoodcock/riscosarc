#!/bin/bash

while read -r src dest; do
	passopt=""
	if `echo $src | grep "_g," > /dev/null 2>&1`; then
		passopt="-gpassword"
	fi
	java riscosarc -x $passopt $src
	if [ ! -e $dest ]; then
		echo "$src failed"
		echo "$dest does not exist."
	elif ! diff $dest source/19337.txt > /dev/null 2>&1; then
		echo "$src failed"
	fi

	rm -f $dest
done < manifest
