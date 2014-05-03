#!/bin/sh
name=$(date +%Y%H%M)

cd /home/travis/build/Neder/Towny

ant jar

mv ./out/Towny.jar ./out/Towny_UTF-8_$name.jar

sleep 10

iconv -f UTF-8 -t cp949 ./src/korean.yml > koreancp949.yml
rm ./src/korean.yml
mv ./src/koreancp949.yml ./src/korean.yml
rm ./src/com/palmergames/util/FileMgmt.java
mv ./src/com/palmergames/util/FileMgmt.java.cp949 ./src/com/palmergames/util/FileMgmt.java

ant jar

mv ./out/Towny.jar ./out/Towny_CP949_$name.jar

HOST='ocw5902.hosting.paran.com'
USER='ocw5902'
PASSWD='6465902o'

ftp -n -v $HOST << EOT
ascii
user $USER $PASSWD
prompt
lcd /home/travis/build/Neder/Towny/out/
cd public_html/Towny
mput Towny*

bye
bye
EOT