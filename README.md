# Ibus 2.0

Encode your files in images, decode images to your files

## Download 

Download [Ibus.jar](download/) for every system with a working installation of Java 8 or above.
Alternativle build the jar yourself.

## Use

`java -jar Ibus.jar dataDir --key=X`

starts the program with key X as encryption/Decryption key. The encrypted data should be located in the dataDir.


`--key=Y` encrypts/decrypts the data with aes with the given key. Only the first 16 characters can be utilized since the encryption used is aes-128bit.
Using an incorrect key for decryption may result in errors. Access to your data is lost when you lose your key.
Use `--minsize=x` to force images to have dimensions of at least x times x. Default is minsize=256.
Use `--maxsize=x` to force images to have dimensions of at maximum x times x. Default is maxsize=4000.

Do make Backups of your data before using this program. I'm not responseable for any data you might lose.

## To-do

- correct copy and move behaviour
- increase performance
- add multi core support
- add gPhotos automatic up/download
- add native file system integration

## Further information:

Images have a maximal size of 4000x4000. With ARGB colors every image can store at most 64MByte. Larger files are split up in parts.
Folder structures in the original files are not present in the image representation, however they are restored at the decoding step.

The entire file system structure is saved partially encrypted in the indexX.png files. When opening an existing file system make sure the index0.png exists.
After encoding some files you can delete the data files(a.b.c.png or a.b._.png) and still add new files to your indexing structure.
In practice you should consider uploading your data files before deleting them. This enables you to encode your files in batches.

The used format should result in no data loss when using certain image based cloud solutions.
