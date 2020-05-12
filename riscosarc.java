// vim:ts=2:sw=2:expandtab:ai
import riscos.archive.CRC;
import riscos.archive.LimitOutputStream;
import riscos.archive.container.ArchiveEntry;
import riscos.archive.container.ArchiveFile;
import riscos.archive.container.ArchiveFileFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class riscosarc {

  public static void usage() {
    System.err.println("Usage: java riscosarc <command> <argument> <archiveFile> [filename]");
    System.err.println("  command can be:");
    System.err.println("  -l: list contents of file");
    System.err.println("  -x: extract file");
    System.err.println("  argument can be:");
    System.err.println("  -c: extract console");
    System.err.println("  -d<path>: extract files to <path>");
    System.err.println("            -d- to extract to dirname of archive file");
    System.err.println("  -g<password>: set password to <password>");
    System.err.println("  -i: ignore errors where possible");
    System.err.println("  -n: don't overwrite files");
    System.err.println("  -r: extract raw compressed data");
    System.err.println("  -v: verbose list contents of file");
    System.err.println("  -D: don't set the timestamp on the extracted file");
    System.err.println("  -T: make the extracted file timestamp match the archive");
    System.err.println("  -F: append RISC OS filetype to file name");
    System.err.println("  --delete-archive: delete archive after extraction");

    System.err.println("Supported archive types are:");
    String[] formats = ArchiveFileFactory.getReaderFormatNames();
    int[] filetypes = ArchiveFileFactory.getReaderFiletypes();
    for (int i = 0; i < formats.length && i < filetypes.length; i++) {
      System.err.println("  " + Integer.toHexString(filetypes[i]) + ": " + formats[i]);
    }

    System.exit(1);
  }

  public static void main(String[] args) {
    boolean doList = false;
    boolean doOverwrite = true;
    boolean doVerbose = false;
    boolean doExtract = false;
    boolean extractRaw = false;
    boolean consoleOutput = false;
    boolean appendFiletype = false;
    boolean deleteArchive = false;
    boolean doTimestamp = true;
    boolean useArchiveTimestamp = false;
    int options = 0;
    String outputDirectory = ".";
    String password = null;
    String suffix = "";
    boolean error = false;
    int archiveFileArg = -1;
    String fileToExtract = null;
    String archiveFilename = null;
    long archiveTimestamp = 0;

    if (args.length < 2) {
      riscosarc.usage();
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-c")) {
        consoleOutput = true;
      } else if (args[i].startsWith("-d")) {
        outputDirectory = args[i].substring(2);
      } else if (args[i].startsWith("-g")) {
        password = args[i].substring(2);
      } else if (args[i].startsWith("-i")) {
        options = ArchiveFile.IGNORE_BAD_ZIP_ENTRY_OPT;
      } else if (args[i].equals("-l")) {
        doList = true;
      } else if (args[i].equals("-n")) {
        doOverwrite = false;
      } else if (args[i].equals("-r")) {
        extractRaw = true;
        suffix = ".raw";
      } else if (args[i].equals("-v")) {
        doVerbose = true;
      } else if (args[i].equals("-x")) {
        doExtract = true;
      } else if (args[i].equals("-D")) {
        doTimestamp = false;
      } else if (args[i].equals("-F")) {
        appendFiletype = true;
      } else if (args[i].equals("-T")) {
        useArchiveTimestamp = true;
      } else if (args[i].equals("--delete-archive")) {
        deleteArchive = true;
      } else if (args[i].charAt(0) == '-') {
        riscosarc.usage();
      } else {
        archiveFileArg = i;
        break;
      }
    }

    if (!(doExtract || doList || doVerbose) || archiveFileArg == -1) {
      riscosarc.usage();
    }

    if (archiveFileArg < args.length - 1) {
      fileToExtract = args[archiveFileArg + 1];
    }

    Enumeration<ArchiveEntry> ent = null;
    ArchiveFileFactory aff;
    ArchiveFile af;
    try {
      archiveFilename = args[archiveFileArg];
      File f = new File(archiveFilename);
      if (useArchiveTimestamp) {
        archiveTimestamp = f.lastModified();
      }
      if (outputDirectory.equals("-")) {
        outputDirectory = f.getParent();
        if (outputDirectory == null) {
          outputDirectory = ".";
        }
      }
      aff = new ArchiveFileFactory(archiveFilename, password,
                                   appendFiletype, options);
      af = aff.getArchiveFile();
      ent = af.entries();
    } catch (Exception ex) {
      error = true;
      af = null;
    }

    if (ent == null) {
      System.err.println("Invalid archive file");
      System.exit(1);
    }

    try {
      while (ent.hasMoreElements()) {
        ArchiveEntry entry = ent.nextElement();
        if (doList) {
          System.out.println(entry.getLocalFilename());
        } else if (doVerbose) {
          entry.printEntryData();
        }

        if (fileToExtract != null && !fileToExtract.equals(entry.getLocalFilename())) {
          continue;
        }

        if (doExtract && !doOverwrite && entry.fileExists(outputDirectory)) {
          System.out.println("Not overwriting " + outputDirectory + File.separator +  entry.getLocalFilename());
        } else if (doExtract) {
          CRC crc = af.getCRCInstance();
          crc.setDataLength(entry.getUncompressedLength());
          entry.cleanOldFile(outputDirectory);
          entry.mkDir(outputDirectory);
          try {
            InputStream fis = null;
            if (extractRaw) {
              fis = af.getRawInputStream(entry);
            } else {
              fis = af.getInputStream(entry);
            }
            if (fis == null) {
              System.err.println("Can not get input stream");
              System.exit(1);
            }

            FileOutputStream fos = null;
            LimitOutputStream los = null;

            if (consoleOutput) {
              los = new LimitOutputStream(System.out, entry.getUncompressedLength());
            } else {
              fos = new FileOutputStream(outputDirectory + File.separator
                                         + entry.getLocalFilename() + suffix);
              los = new LimitOutputStream(fos, entry.getUncompressedLength());
            }

            int len = 0;
            byte[] buf = new byte[1024];
            do {
              len = fis.read(buf, 0, buf.length);
              if (len != -1) {
                crc.update(buf, 0, len);
                los.write(buf, 0, len);
              }
            } while (len != -1);

            if (!consoleOutput) {
              if (los != null) {
                los.close();
              }
              if (fos != null) {
                fos.close();
              }

              if (doTimestamp) {
                if (useArchiveTimestamp) {
                  entry.setFileTime(outputDirectory, archiveTimestamp);
                } else {
                  entry.setFileTime(outputDirectory);
                }
              }
            }

            if (!extractRaw && !crc.compare(entry.getCrcValue())) {
              System.out.println("CRC check failed");
              error = true;
            }
          } catch (Exception ex) {
            System.out.println(ex.toString());
            ex.printStackTrace(System.out);
            error = true;
          }
        }
      }
    } catch (Exception ex) {
      System.err.println(ex.toString());
      ex.printStackTrace(System.out);
      error = true;
    }

    if (error) {
      System.exit(1);
    } else if (deleteArchive && doExtract) {
      try {
        if (af != null) {
          af.close();
        }
        File arc = new File(archiveFilename);
        if (arc.delete() == false) {
          System.out.println("Failed to delete " + archiveFilename);
        }
      } catch (IOException e) {
        System.out.println("Failed to close file");
      }
    }
  }
}
