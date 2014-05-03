ls -l
name=$(date +%Y%m%d)
ftp -inv neder1.cloudapp.net << EOT
user Towny \n
help
put Towny_C*
put Towny_U*
bye
bye
EOT