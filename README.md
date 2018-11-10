# RISC OS de-archiver

This is a Java implementation of the various compression algorithms used by
RISC OS archivers.

Supported archive formats are:
- SparkFS
- ArcFS
- PackDir
- Squash

riscosarc can not create archives, it can only extract files from them.

Usage:
java riscosarc [opt] [archive file]
where opt is:
  -l: list contents of archive
  -v: verbose listing
  -x: extract files from archive

Files are extracted with the RISC OS filetype appended to the name.

There is precious little documentation, and probably no comments in the code.

## To Build

javac riscosarc.java
