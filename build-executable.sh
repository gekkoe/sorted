#!/bin/sh
lein uberjar && cat env/dev/build-tools/stub.sh target/uberjar/sorted.jar > sorted.run && chmod +x sorted.run
