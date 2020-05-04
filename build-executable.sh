#!/bin/sh
lein uberjar && cat env/dev/resources/stub.sh target/uberjar/sorted.jar > sorted.run && chmod +x sorted.run
