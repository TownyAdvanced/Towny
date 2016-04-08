echo "Pull Request Number(false = Master Build) => ${TRAVIS_PULL_REQUEST}"
name=$(date +%Y%m%d)
echo MD5:
md5sum ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar
md5sum ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar
if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    echo Uploading Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar ...
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar -u ${username}:${password} ftp://${host}
    echo Uploading Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar ...
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar -u ${username}:${password} ftp://${host}
else
    echo "It's not master build."
fi
