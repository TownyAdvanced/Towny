echo "Pull Request Number(false = Master Build) => ${TRAVIS_PULL_REQUEST}"
name=$(date +%Y%m%d)
if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_CP949_${TRAVIS_COMMIT}.jar -u u833935363:TownyTowny ftp://ftp.ocw5902.w.pw/Towny/
    sudo curl -T ./out/Towny_${TRAVIS_JOB_NUMBER}_${name}_UTF-8_${TRAVIS_COMMIT}.jar -u u833935363:TownyTowny ftp://ftp.ocw5902.w.pw/Towny/
else
    echo "It's not master build."
fi
