# Ibus

Encode your files in images, decode images to your files

## Download 

Download [Ibus.exe](download/Windows/) for Windows.

Download [Ibus.jar](download/) for every system with a working installation of Java 7 or above.

## Use

### Windows

`Ibus.exe sourceDir targetDir -encode`

encodes all files in the source directory and stores the images in the target directory.

`Ibus.exe sourceDir targetDir -decode`

decodes all files in the source directory and stores the decoded files in the target directory.
An additional parameter `--delete` results in all source files being deleted after they are encoded or decoded.

### General

`java -jar Ibus.jar sourceDir targetDir -encode`

encodes all files in the source directory and stores the images in the target directory.

`java -jar Ibus.jar sourceDir targetDir -decode`

decodes all files in the source directory and stores the decoded files in the target directory.
An additional parameter `--delete` results in all source files being deleted after they are encoded or decoded.

**:warning: Do not use --delete unless you know what you are doing!**

## Further information:

Images have a maximal size of 4000x4000. With ARGB colors every image can store at most 64MByte. Larger files are split up in parts.
Folder structures in the original files are not present in the image representation, however they are restored at the decoding step.

The used format should result in no data loss when using certain image based cloud solutions.