# filesync

Disclaimer:
This project has solely an educational purpose and will only be used to complete
the Functional Programming course.

It will not be ready to be used in a real environment, as security (encryption, authorization and authentication) is not the main concern and may or may not be implemented reliably or not at all.

--- About ---
Authors:
-> Jesko Plitt (jp063@hdm-stuttgart.de)
-> Daniel Freise (df036@hdm-stuttgart.de)

Description:
The project contains trivial server and client implementations written in Clojure.
The server will act as middleman that enables clients to find each other for further
communication.

The clients itself then specify a folder to analyze and compare with the other client's folder content.
Any differences shall be recognized and changes should be updated / synchronized:

Changes prior to the latest synchronization shall not be taken into account.

## Installation

Download from https://bitbucket.org/Suddoha/file-synchronization-clojure-project.

## Usage

FIXME: explanation

    $ java -jar filesync-0.1.0-standalone.jar [args]

## Options


## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2018 Jesko Plitt, Daniel Freise

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
