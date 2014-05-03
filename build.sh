name=$(date +%Y%m%d)
ftp -inv neder1.cloudapp.net << EOT
user Towny \n
lcd /home/travis/build/Neder/Towny/out
put Towny_CP949_${name}.jar
put Towny_UTF-8_${name}.jar
bye
bye
EOT