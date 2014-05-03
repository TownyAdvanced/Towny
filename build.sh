ftp -n -v ocw5902.hosting.paran.com << EOT
ascii
user ocw5902 6465902o
prompt
lcd /home/travis/build/Neder/Towny/out/
cd public_html/Towny
mput Towny*
bye
bye
EOT