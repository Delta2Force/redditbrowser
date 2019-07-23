#!/bin/bash
mvn test && mvn clean deploy

exit $?