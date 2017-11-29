# Scall
Shell is worse on organize command together. Scall is not replace Shell, rather then, enhance the shell command by make it organizable.

## Usage
`libraryDependencies += "com.scalachan" %% "scall" % "0.2.12"` //only support Scala 2.11 and 2.12

## Dependency
- JSch

## Example
[Example.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/Example.scala)

## TODO
- capture disconnect
 1. setting `ServerAliveInterval` [x]

- add For-expressions semantic to operating batch commands happily
- refactor ReaderDispatch to support custom dispatch stream with pattern match and some DSL.
- response message save to Bytes and print to string rather then byte to char to string and only support UTF 8 [x]
- every command use unique SPLIT protocol


- message send to queue which can able to hold message below the queue.

## Consider
