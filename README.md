sorted - A simple program to sort people.
=====================================================

Implementation Decisions
-----------------------------

### Language Choice
* Though not stated directly in the specifications, the implication that Clojure
would be the best choice here seems clear. It is an excellent language for this
sort of task, and it's the one used by the client.

### Primary Libraries
* Confirmed on [library.io](https://libraries.io/search?languages=Clojure&order=desc&sort=dependent_repos_count)
and [clojure-doc.org](http://clojure-doc.org/articles/ecosystem/libraries_directory.html)
several libraries that appear to be in common use for the tasks at hand that I'm either
familiar with or that cover technologies (like generative testing) that I discussed in
a prior interview with the client and would like to incorporate here to learn more
about them.
* The following (in no particular order) will be used for this project:
  - ring/compojure - These still appear right toward the top for basic web application
and routing needs.
  - clj-time - Clojure wrapper for the popular Joda Time library.
  - liberator - A commonly used REST library which I've had some experience with.
  - http-kit - A commonly used HTTP client/server.
  - clojure.test - Standard testing library.
  - clojure.spec - A data specification library that I'd like to explore more here.
  - test.check - Listed in the [clojure.org spec page](https://clojure.org/about/spec)
as a generative test library that uses clojure.spec to create better test coverage.
  - cheshire - A JSON library in common use that is generally more performant than data.json.

### Build Tools
* This project will start out with lein, as this is what I'm most familiar with. At some
point along the way though, I'll be looking into converting it to a CLI/deps project to
explore this new technology, which the client indicated they are using in a prior
interview.
* I'll be coding the project in Emacs with Spacemacs, but it should open and build fine
in any other properly configured Clojure development environment.
