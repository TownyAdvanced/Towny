echo "Pull Request Number(false = Master Build) => ${TRAVIS_PULL_REQUEST}"
name=$(date +%Y%m%d)
if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    echo Uploading Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar ...
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar -u u262377766.towny:dtdtdtdtdtd ftp://ftp.ocw5902.esy.es/Towny/
    echo Uploading Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar ...
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar -u u262377766.towny:dtdtdtdtdtd ftp://ftp.ocw5902.esy.es/Towny/
else
    echo "It's not master build."
fi
