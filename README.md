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
 1. setting `ServerAliveInterval`

- add For-expressions semantic to operating batch commands happily
- response message save to Bytes and print to string rather then byte to char to string and only support UTF 8 [x]

- every command use unique SPLIT protocol
  :: consider ssh is based stream which is sequence:
  client							server
    req1 ->
          						<- rsp1
		req2 ->
		req3 ->
											<- rsp2
											<- rsp3

	so, sequence will not be break and command needn't unique SPLIT protocol
	:: for debug log file specify send command and response message

## Consider
