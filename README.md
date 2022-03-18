## Description

`lq` is a commandline tool for querying your [logseq](https://logseq.com/)
knowledge graphs. `lq` makes it easy to define custom datalog queries and rules
and invoke them from the commandline. Rules and queries can be composed to make
complex queries easy to write and run.

## Setup

1. Install [babashka](https://github.com/babashka/babashka#installation) >= 0.7.6
1. Install
[clojure](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
or [build bb-datascript](#build-bb-datascript) (commands run much faster but
this setup is more involved)
1. `git clone https://github.com/cldwalker/logseq-query`
1. Recommended: To run `lq` from any directory and to use `bb-datascript`, put `bin/` on your `$PATH`:

  ```sh
  # In a shell or your .zshrc/.bashrc file
  export PATH=$PATH:$HOME/path/to/logseq-query/bin
  ```

### Build bb-datascript

To run `lq` with sub-second times, you have to build a variant of babashka that
includes [datascript](https://github.com/tonsky/datascript). Steps to do this:

1. Install [graalvm](https://www.graalvm.org/downloads/). On osx, graalvm can be
installed with homebrew e.g. `brew install graalvm-ce-java11`.
1. Clone babashka: `git clone https://github.com/babashka/babashka --recursive`
1. Build `bb-datascript` (this takes 5-10 min): `script/build_bb_datascript.clj /path/to/babashka`
1. Confirm it's correctly built with `bin/bb-datascript --version`

If you get stuck on a step or want to learn more about the build process, see
[babashka's build
doc](https://github.com/babashka/babashka/blob/master/doc/build.md).

## Usage

_Note_: This readme assumes basic familiarity with datalog queries. For a primer
on them, see http://www.learndatalogtoday.org/. Also, if `lq` is not on your
`$PATH`, replace `lq` with `bin/lq` in the examples.

`lq` can query logseq graphs in `~/.logseq`. For example:

```sh
$ lq graphs

|        :name |                                                                                           :path |
|--------------+-------------------------------------------------------------------------------------------------|
|       lambda |                   /Users/me/.logseq/graphs/logseq_local_++Users++me++code++work++lambda.transit |
| logseq-notes |             /Users/me/.logseq/graphs/logseq_local_++Users++me++code++priv++logseq-notes.transit |
|   test-notes | /Users/me/.logseq/graphs/logseq_local_++Users++me++code++repo++logseq-query++test-notes.transit |
Total: 3
```

`lq` runs queries against one of these graphs. To specify a default graph (and
to avoid having to specify one on every query), add this to your lq config with
your `GRAPH`:

```sh
echo '{:default-options {:graph "GRAPH"}}' > ~/.lq/config.edn
```

`lq` comes with a few named queries and rules:

```sh
$ lq queries

|           :name | :namespace |          :parent |                                                      :desc |
|-----------------+------------+------------------+------------------------------------------------------------|
|  content-search |         lq |                  |                         Full text search on :block/content |
|    has-property |         lq |                  |                       List blocks that have given property |
|        property |         lq |                  | List all blocks that have property equal to property value |
|    property-all |         lq |                  |                       List all blocks that have properties |
| property-counts |         lq | :lq/property-all |                            Counts for all block properties |
| property-search |         lq |                  |                               Full text search on property |
|            task |         lq |                  |                      Todos that contain one of the markers |
Total: 7

# Queries can be run by their :name
$ lq q content-search foo
...

# If two queries that have the same base name, invoke their full name i.e. :namespace/:name
$ lq q lq/content-search foo

$ lq rules

|             :name | :namespace |                                                             :desc |
|-------------------+------------+-------------------------------------------------------------------|
|     page-property |     logseq | Pages that have property equal to value or that contain the value |
|     block-content |     logseq |                   Blocks that have given string in :block/content |
|         namespace |     logseq |                                                                   |
|              page |     logseq |                                                                   |
|      has-property |     logseq |                                   Blocks that have given property |
|     all-page-tags |     logseq |                                                                   |
| has-page-property |     logseq |                                    Pages that have given property |
|          priority |     logseq |                                                                   |
|           between |     logseq |                                                                   |
|              task |     logseq |                                 Tasks that contain one of markers |
|         page-tags |     logseq |                                                                   |
Total: 11
```

### Queries

The `q` command runs one of the named queries from the previous section as well as any user-defined queries.

Let's try one of the default queries, `property`, which finds blocks/lines with a specific property value:
```sh
$ lq q property type digital-garden
[{:block/uuid #uuid "620e8da6-e960-4e81-918e-4678db577794", :block/properties {:url "https://note.xuanwo.io/#/page/database", :type "digital-garden", :desc "Great to see one with active use of type"}, :block/left #:db{:id 3436}
...
```

Query results print as [EDN](https://github.com/edn-format/edn) by default. This
allows tools like [babashka](https://github.com/babashka/babashka) to transform
results easily e.g.

```sh
$ lq q property type digital-garden | bb '(->> *input* (map #(-> % :block/properties :url)))'                                                                         
("https://note.xuanwo.io/#/page/database" "https://kvistgaard.github.io/sparql/" "https://zettelkasten.sorenbjornstad.com/")
```

`lq` provides useful transformations with the following options:

* `-c`: Prints the count of the results
* `-p`: Colorizes and pretty prints results with [puget](https://github.com/greglook/puget), assuming `puget` is available as a command
* `-C`: Prints only the contents of block results. This is useful as it allows you to search through query results easily, which is not possible with logseq
* `-t`: Prints results in a table. If it's a block, it will print the :block/properties e.g.

```sh
$ lq q property type digital-garden -t
|  :id |                                     :url |          :type |                                    :desc |
|------+------------------------------------------+----------------+------------------------------------------|
| 3407 |   https://note.xuanwo.io/#/page/database | digital-garden | Great to see one with active use of type |
| 6674 |     https://kvistgaard.github.io/sparql/ | digital-garden |        sparql tutorials made with logseq |
| 6600 | https://zettelkasten.sorenbjornstad.com/ | digital-garden | "remnote employee, made with tiddlywiki" |
...
```

For more options, see `lq q -h`.

### Short queries
`lq` provides short queries through the `sq` command. This command allows you to specify
as little or as much of a query from the commandline.

Some examples:

```sh
# A single where clause can be specified as is
$ lq sq '(content-search ?b "github.com/")'
...

# For larger where clauses, wrap it in a vector with :where
$ lq sq '[:where (content-search ?b "github.com/") (task ?b #{"DONE"})]'
...

# Queries without a :find default to `(pull ?b [*])`. This can be overridden with an explicit :find
$ lq sq '[:find ?b :where (content-search ?b "github.com/") (task ?b #{"DONE"})]'
...

# To print what the full query looks like
$ lq sq '[:find ?b :where (content-search ?b "github.com/") (task ?b #{"DONE"})]' -n
```

The `sq` command supports most of the `q` options. For the full list of
available options, see `lq sq -h`.

### Create a query

Where `lq` shines is in how easy it is to define new queries. Referencing [this
section](#queries.edn), a query is a map entry in `queries.edn` where the map
key is its name and the value is a map with `:query` and `:desc`.edn`. Let's add
the query from the last section:

```sh
# Copies the last command's output to clipboard in osx
$ lq sq '[:find ?b :where (content-search ?b "github.com/") (task ?b #{"DONE"})]' -n | pbcopy
```

In `queries.edn`, paste the clipboard and add a `:desc`:

```clojure
;; cldwalker is my namespace but feel free to choose your own e.g. github username
:cldwalker/github-tasks
{:query
 [:find
  (pull ?b [*])
  :where
  (content-search ?b "github.com/")
  (task ?b #{"DONE"})]
 :desc "Find github tasks"}
```

This query can now be run as `lq q github-tasks`!

To make this query more useful, let's give this query arguments and transform
them with `:in` and `:args-transform` keys respectively. For example:

```clojure
:cldwalker/github-tasks
{:query
 [:find
  (pull ?b [*])
  ;; $ and % are needed for all our queries at the beginning and end respectively
  ;; and refer to database and rules
  :in $ ?markers %
  :where
  (content-search ?b "github.com/")
  (task ?b ?markers)]
 :args-transform (fn [args]
                   (set (map (comp clojure.string/upper-case name) args)))
 :desc "Find github tasks"}
```

This query can now be called with arguments e.g. `lq q github-tasks todo doing`.

### Create a rule

Datalog rules allow you to bundle multiple where clauses behind one clause. They
are a great way to compose functionality, leverage datalog's terse power and
make queries more readable. Referencing [this section](#rules.edn), a rule is a
map entry in `rules.edn` where the key is its name and the value is a map with
`:rule` and `:desc` keys. For example, to reuse the `github-tasks` query in
other queries:

```clojure
;; cldwalker is my namespace but feel free to choose your own e.g. github username
:cldwalker/github-task
{:rule
 [(github-task ?b ?markers)
  (content-search ?b "github.com/")
  (task ?b ?markers)]
:desc "Github tasks"]}
```

With this rule defined, use it in a quick query to find github tasks that
contain the word logseq e.g.

```sh
lq sq '[:where (github-task ?b #{"TODO"}) [?b :block/content "logseq"]]'
```

## Config

lq has three optional config files under `~/.lq/`. Config files allow you to
add functionality to `lq`.

* [config.edn](#config.edn) - General configuration
* [queries.edn](#queries.edn) - Define custom queries
* [rules.edn](#rules.edn) - Define custom rules

_Note_: This tool is alpha and there may be breaking changes with configuration
until it stabilizes.

For examples of these configs, see [mine](https://github.com/cldwalker/dotfiles/tree/master/.lq).

### config.edn

This is the main config file. It is a map with the following keys:

* `:default-options` (map): Provides default values for options to `q` and `sq` commands.

### queries.edn

This file defines custom queries similar to [logseq's advanced queries](https://docs.logseq.com/#/page/advanced%20queries). Queries are maps with the following keys:

* `:query` (vector): A logseq/datascript datalog query. Any lq rules can be used
  in a query
* `:desc` (string): A brief description of the query
* `:parent` (keyword): Refer to an existing query in order to inherit its key
  values. The most common use case is to apply different result-transforms on
  the same query
* `:default-args` (vector): Default arguments to pass to query if none are given
* `:args-transform` (fn): Fn to transform arguments
* `:result-transform` (fn): Fn to transforms query results. Same as logseq

### rules.edn

This file defines custom [datalog
rules](https://docs.datomic.com/on-prem/query/query.html#rules). Rules allow you
to group `:where` clauses in a query. Rules are maps with the following keys:

* `:rule` (vector) - A datalog rule
* `:desc` (string) - A brief description of the rule

## Motivation

This project aims to empower logseq users to access and transform their
knowledge in fine-grained ways from the commandline. This project is also a
great way to experiment with querying. Since this is a
commandline tool, hopefully this inspires folks to script/automate their Logseq
graphs. Personally, I've found this fun as datalog on the commandline with
sub second startup times has not been a thing until babashka.

## Contributing

I'm not seeking major contributions to this project though discussion and issues
on github are always welcome. I may be interested in a query or rule
contribution if it's general enough. For those contributions, I would look for a
test page to be added to the `test-notes` graph and tests to be added to
`cldwalker.logseq-query.queries-test`.

## License
See LICENSE.md

## Additional Links
* [Datalevin](https://github.com/juji-io/datalevin#babashka-pod) - another datalog
db that can be scripted with babashka
* [Zsh autocompletion for lq](https://github.com/cldwalker/dotfiles/blob/181c90681e93170cc682f6e96f8fc169675b760d/.zsh/completions.zsh#L63-L69)
