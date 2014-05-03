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