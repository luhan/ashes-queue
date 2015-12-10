A Simple user guide on how to use the ashes-queue

# Prerequisites #

  * java version "1.7.0-ea"   (developer used 1.7 although 1.6 should be ok)
  * Apache Maven 2.2.1 or more
  * svn (Sometimes the releases are updated regularly, hence recommend to checkout the source and build)
  * I test this on 2.6.35-23-generic kernel, Ubuntu 10.4 version.
> > The system uses MappedByteBuffer, so make sure the Memory Mapped files operational characteristics of other platforms won't harm your requirements.

# How to build the system #

  * heckout the code to a directory
> > `svn checkout http://ashes-queue.googlecode.com/svn/trunk/ ashes-queue-read-only`

  * o to the trunk directory and use
> > `mvn clean install`

  * se the target/ashes-1.0.jar to develop your producer consumer part

# How to test the system #
  * Simple ProducerConsumerSample.java does the both the producing and consuming work
  * The properties of the producer/consumer is configured in pc.properties
  * Go to `target/ashes-queue/bin` and run `./ashes console`
  * You can see the messages persistent file inside target/ashes-queue directory
  * `mvn test` will run the testcases. There are some handy TestNG cases!