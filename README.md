sorted - A simple program to sort people.
=====================================================

Implementation Decisions
-----------------------------

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
* I've also looked to [luminusweb.com](http://luminusweb.com) for example code
of how to set up portions of this project. I've found in the past that they
provide a sane starting point for simple Clojure web apps.
* The following will be used for this project:
  - cheshire - A JSON library that is generally more performant than data.json.
  - clojure.java-time - A Java 8+ Date-Time API wrapper library.
  - clojure.spec - A data specification library that I'd like to explore more
    here.
  - clojure.spec.gen - Generative testing for specs.
  - clojure.spec.test - Testing library for specs.
  - clojure.test - Clojure's standard testing library.
  - clojure.tools.cli - Simple cli parsing.
  - compojure - A routing library for ring.
  - cprop - Facilitates pre-loading program state and properties on startup.
  - expound - Provides easier-to-read spec reports for use during development.
  - failjure - A monadic error handler to maintain referential integrity and
    readability.
  - http-kit - An HTTP client/server.
  - liberator - A REST library which I have some prior experience with.
  - mount - Works with cprop to support startup and shutdown state management.
  - ring - A web application library for Clojure.
  - test.check - Listed in the [clojure.org spec
page](https://clojure.org/about/spec) as a generative test library that uses
clojure.spec to create better test coverage. This isn't called directly, but is
used by clojure.spec.cen and clojure.spec.test in the development and test
profiles.
  - tools.cli - A command line argument parser.

### Build Tools
* This project will be using lein.
* I'll be coding the project in Emacs with Spacemacs, but it should open and
build fine in any other properly configured Clojure development environment.

### Coding Standards
I will be using some fairly standard Clojure conventions such as the ones
outlined in this [archived
post](https://web.archive.org/web/20181116030946/http://dev.clojure.org/display/community/Library+Coding+Standards)
on the Clojure developers blog.

### Design Choices & Things Learned
* This has been my first real exposure to spec and generative testing. It took a
little getting used to at first, but I'm quite impressed with the compliance
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
testing it was sometimes simpler to just filter out that possibilty.
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
