sorted - A simple program to sort people.
=====================================================

Implementation Decisions
-----------------------------

### Language Choice
* Though not stated directly in the specifications, the implication that Clojure
would be the best choice here seems clear. It is an excellent language for this
sort of task, and it's the one used by the customer.

### Libraries/Dependencies
* Confirmed on [library.io](https://libraries.io/search?languages=Clojure&order=desc&sort=dependent_repos_count)
and [clojure-doc.org](http://clojure-doc.org/articles/ecosystem/libraries_directory.html)
several libraries that appear to be in common use for the tasks at hand that I'm either
familiar with or that cover technologies that I discussed in a prior interview with the
customer and would like to incorporate here to learn more about them.
* The following will be used for this project:
  - cheshire - A JSON library that is generally more performant than data.json.
  - clj-time - Clojure wrapper for the popular Joda Time library.
  - clojure.spec - A data specification library that I'd like to explore more here.
  - clojure.test - Clojure's standard testing library.
  - compojure - A routing library for ring.
  - http-kit - An HTTP client/server.
  - liberator - A REST library which I have some prior experience with.
  - ring - A web application library for Clojure.
  - test.check - Listed in the [clojure.org spec page](https://clojure.org/about/spec)
as a generative test library that uses clojure.spec to create better test coverage.
  - tools.cli - Command line argument parser.

### Build Tools
* This project will start out with lein, as this is what I'm most familiar with. At some
point along the way though, I'll be looking into converting it to a CLI/deps.edn project
to explore this new technology, which the customer indicated they are using in a prior
interview.
* I'll be coding the project in Emacs with Spacemacs, but it should open and build fine
in any other properly configured Clojure development environment.

### Coding Standards
I will be using some fairly standard Clojure conventions such as the ones outlined in
this [archived post](https://web.archive.org/web/20181116030946/http://dev.clojure.org/display/community/Library+Coding+Standards) on the Clojure developers blog.
