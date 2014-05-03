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
put Towny_C*
put Towny_U*
bye
bye
EOT