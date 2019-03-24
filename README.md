JdbcShell
=========

### Running

    ./gradlew install
    build/install/jdbcshell/bin/jdbcshell -url URL
    build/install/jdbcshell/bin/jdbcshell -help

### Shipping

    ./gradlew distZip
    unzip build/distributions/jdbcshell-0.0.1-SNAPSHOT.zip
    ./jdbcshell-0.0.1-SNAPSHOT/bin/jdbcshell -config tmp/mysql.properties
