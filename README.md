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

    $ java -jar filesync-0.1.0-standalone.jar

# Configuration
## Server

To successfully start the application as server, you have to create a .syncconf file at the directory of your filesync-0.1.0-standalone.jar
Example .syncconf contents:

```
( 
  :-mode        "server" 
  :-host        "localhost" 
  :-port        50000 
  :-keepAlive   30000 
)
```

## Client
To successfully start the application as client, you have to create a .syncconf file at the directory of your filesync-0.1.0-standalone.jar
Example .syncconf contents:

```
( 
  :-mode        "client" 
  :-host        "localhost" 
  :-port        50000 
  :-keepAlive   10000 

  :-filePath    "/Absolute/Data/Path" 
  :-syncKey     [100 100 100 100 100 100 100 100]
)
```

Please make sure you will change the bytes defined under :-syncKey
The port specified in :-port has to match the server port of the application. The corresponding goes for :-host.


# Ignorefile

You can place a .ignore file within your data directory (clientside). Each line of that file is a regular expression that will exclude some path or file from the synchronization.

Example:

```
.git/.*
target/.*
```

# Build

To build the application run `lein uberjar` in the project root.
You will find the builded files in `target/uberjar/`

## License

Copyright © 2018 Jesko Plitt, Daniel Freise

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
