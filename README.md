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

**:warning: Do NOT use --delete unless you know what you are doing! **