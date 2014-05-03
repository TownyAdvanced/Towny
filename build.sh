ftp -inv ocw5902.hosting.paran.com << EOT
user ocw5902 6465902o
prompt
lcd /home/travis/build/Neder/Towny/out/
cd public_html/Towny
put Towny_CP949_$name.jar
put Towny_UTF-8_$name.jar
bye
bye
EOT