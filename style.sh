#!/bin/bash

if [ "$1" = "" ]; then
	path="."
else
	path="$1"
fi

checkstyle -c etc/very_nearly_google_checks.xml "$path" | grep -v Javadoc | grep -v AbbreviationAsWordInName | grep -v LineLength
