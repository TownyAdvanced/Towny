ls -l
name=$(date +%Y%m%d)
ftp -inv neder1.cloudapp.net << EOT
user Towny \n
pwd
lcd /
lcd home
lcd travis
lcd build
lcd Neder
lcd Towny
lcd out
put Towny_CP949_${name}.jar
put Towny_UTF-8_${name}.jar
bye
bye
EOT