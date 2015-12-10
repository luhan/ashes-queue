# About #
This is a simple FIFO implementation in Java which has persistent support. That is, if the queue is full, the overflowing messages will be persisted and when there is available slots, they will be put back into memory.

# Development #
This is a small project(1400 LOC) done by one developer. No donations required. More you use and fix/report the issues, more happy I would be!

# Current Features #
  * Producer Consumer FIFO Queue support
  * Overflowing queue messages are persisted in file
  * Incase of unexpected process exit, the backlog support works, but the messages which were in the memory queue will have been lost.
  * Higher performing IO operations since its using Memory Mapped files. Java's MappedByteBuffer from nio FileChannel is used to persist and read data which makes it faster.
  * TestNG support

## Documentation ##
  * [User Guide](http://code.google.com/p/ashes-queue/wiki/UserGuide)
  * [Architecture Guide](http://code.google.com/p/ashes-queue/wiki/ArchitectureGuide)