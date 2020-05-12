sorted - A simple program to sort people.
=====================================================

Implementation Decisions
-----------------------------

### Running the Program
The Linux executable version of this program is called __sorted.run__ and can be
downloaded from the latest [release](https://github.com/gekkoe/sorted/releases)
To see the command-line options and usage run `sorted.run --help` If it needs to
be run on a different platform, the __sorted.jar__ file can be built using `lein
uberjar` anywhere within the project. This will build a self-contained jar file
under _$PROJECTROOT/target/uberjar_ that can be run using the command `java -jar
sorted.jar OptionsAndFilesHere` or it may be possible to compile to a GraalVM
fallback executable that is smaller but still requires a java installation by
using Graal's native-image program on the jar file, preferably with the
_--initialize-at-build-time_ flag set.

Sample input files can be found in the _$PROJECTROOT/env/dev/resources_ folder.

### Language Choice
* Though not stated directly in the specifications, the implication that Clojure
would be the best choice here seems clear. It is an excellent language for this
sort of task, and it's the one used by the customer.

### Libraries/Dependencies
* Confirmed on
[library.io](https://libraries.io/search?languages=Clojure&order=desc&sort=dependent_repos_count)
and
[clojure-doc.org](http://clojure-doc.org/articles/ecosystem/libraries_directory.html)
several libraries that appear to be in common use for the tasks at hand that I'm
either familiar with or that cover technologies that I discussed in a prior
interview with the customer and would like to incorporate here to learn more
about them.
* I've also looked to [luminusweb.com](http://luminusweb.com) for examples of
how to set up portions of this project. I've found in the past that they provide
a sane starting point for simple Clojure web apps. That said, they tend to put
quite a bit more than I need for this project in their baseline templates, so
I've only used a few ideas from them here.
* The following will be used for this project:
  - clojure.java-time - A Java 8+ Date-Time API wrapper library.
  - clojure.spec - A data specification library that I'd like to explore more
    here.
  - clojure.spec.gen - Generative testing for specs.
  - clojure.spec.test - Testing library for specs.
  - clojure.test - Clojure's standard testing library.
  - clojure.tools.cli - Simple cli parsing.
  - clojure.tools.logging & logback-classic - Logging.
  - compojure - A routing library for ring.
  - eastwood - Code linter.
  - expound - Provides easier-to-read spec reports for use during development.
  - failjure - A monadic error handler to maintain referential integrity and
    readability.
  - hiccup - Renders HTML from Clojure.
  - jetty - An HTTP server.
  - java-time-literals - Developer convenience library for copying/pasting date
    literals.
  - liberator - A REST library which I have some prior experience with.
  - prone - Better exception reporting middleware for Ring.
  - ring - A web application library for Clojure.
  - test.check - Listed in the [clojure.org spec
page](https://clojure.org/about/spec) as a generative test library that allows
clojure.spec to create better test coverage. This isn't called directly, but is
used by clojure.spec.gen and clojure.spec.test in the development and test
profiles.
  - tools.cli - A command line argument parser.

### Build Tools
* This project will be built using lein.
* I'll be coding the project in Emacs with Spacemacs, but it should open and
build fine in any other properly configured Clojure development environment.
* To create a self-contained executable for the releases I'm using a script
called _stub.sh_ which can be found in _$PROJECTROOT/env/dev/build-tools_ that
can be concatenated with a jar file and runs it, passing in any args it
receives. The file name of the executable is __sorted.run__ and it can be
rebuilt using the shell command __build-executable.sh__ in the project's root.
* I've been able to build a "fallback" version of an executable using GraalVM,
but since I haven't been able to get its native-image to run without resorting
to a fallback and I'm not certain how well such a file will work in various run
environments, I've decided not to use it as the release executable at this time.
It does, however, look like a very promising way to handle such things in the
future, once I better understand their system.

### Design Decisions & Things Learned
* This has been my first real exposure to spec and generative testing. It took
some getting used to at first, but I'm quite impressed with the compliance
testing that they allow, and have used them to weed out some unexpected edge
case bugs.
* I considered using cli-matic instead of tools.cli, but it seems to be rather
opinionated about the formatting of command lines in a way that doesn't keep
with my expectations and understanding of the GNU option parsing conventions.
There may be a way to configure it, but the added value it proposed did not
justify further experimentation at this time.
* At first I was using clj-time, as I've used Joda Time in past projects. When I
ran into some issues, I found out that clojure.java-time is now preferred.
* At first I was going to represent the internal storage of birthdays as a
simple map, but during generative test creation there was enough call for the
java-time functions that it seemed a bit of a kludge to not just store dates as
a java.time.LocalDate, so this is what I ended up settling on.
* I tried to make as few assumptions about the nature of the data files as
possible. The design spec said that I could assume that the values would not
contain other delimiters, but I found that for the sake of the code's logic and
testing it was sometimes simpler to just filter out that possibility.
* I've limited the range of acceptable dates to the years 1 through 9999. I
tried with the built-in MIN and MAX dates but there are some off-by-one bugs and
sign switching that happen with the M/d/yyyy formatter once the year numbers
become large enough for java to throw in a + or - in the date. This seemed out
of scope for this assignment, so I haven't spent more time finding a way around
these issues.
* I found that if a person string was allowed to contain '\n' or '\r' it would
cause the regular expressions that I use to pre-verify the apparent validity of
a line of text to fail. Since line breaks don't make sense in this context
anyway, I've filtered them out of the generative testing and am considering any
text line containing one to be an invalid entry.
* There are a couple of minor kludges that have been left in the code, since
figuring them out seemed a bit off-path. It would be nice to learn better ways
to approach these things, so I've labeled them for later consideration.
* I decided to put in rudimentary sanity checks by not allowing more than a max
number of entries in the list and blocking duplicate entries.
* After quite a bit of development in the handler code, I realized that a
simpler approach would have worked. It turns out that liberator drops namespaces
from keywords when rendering qualified maps to JSON. Having not dealt with
qualified keywords much prior to learning spec, I had not tried them with
liberator before, and made an incorrect assumption. While fixing this would
remove a bit of complexity and would not be particularly difficult to do, I've
decided to leave the current version for demo purposes, as there were a number
of interesting problems to solve in the process, and such code would be useful
under somewhat different circumstances. If this were going to be production code
though, I would certainly look to rewrite that part for added simplicity.
* The logging included in this project is quite rudimentary. If it were going
into production, I'd want to change the f/fail functions to a log-fail pass
through, add a few more info logs to the handler, etc. But I reasoned that the
addition and configuration of logging capability was adequate for the task at
hand.
