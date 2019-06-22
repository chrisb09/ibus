# Ibus

Encode your files in images, decode images to your files

## Download 

Download [Ibus.jar](download/) for every system with a working installation of Java 8 or above.

## Use

`java -jar Ibus.jar sourceDir targetDir -encode`

encodes all files in the source directory and stores the images in the target directory.

`java -jar Ibus.jar sourceDir targetDir -decode`

decodes all files in the source directory and stores the decoded files in the target directory.
An additional parameter `--delete` results in all source files being deleted after they are encoded or decoded.
Use `--minsize=x` to force images to have dimensions of at least x times x. Default is minsize=256.
Use `--key=Y` to encrypt/decrypt the data with aes with the given key. Only the first 16 characters can be utilized since the encryption used is aes-128bit.
Using an incorrect key for decryption may result in errors. Access to your data is lost when you lose your key.
Use `--cleartarget` to delete all files in the target directory.
Use  `--no-index` to disable the indexing.

**:warning: Do not use `--delete` or `--cleartarget` unless you know what you are doing!**

Do make Backups of your data before using this program. I'm not responseable for any data you might lose.

## To-do

- output progress and speed as well as addition info
- add multi core support

## Further information:

Images have a maximal size of 4000x4000. With ARGB colors every image can store at most 64MByte. Larger files are split up in parts.
Folder structures in the original files are not present in the image representation, however they are restored at the decoding step.

The used format should result in no data loss when using certain image based cloud solutions.
