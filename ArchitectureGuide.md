Small Architecture Guide to explain how the Queue Persistent works

# Introduction #

This is a simple producer consumer solution, but there is a little complexity due to the persistence support for queue overflow. The major part of the design centered around the state transitiosn of the queues and the mechanism for persisting the queue.


# Details #

## Queue States ##

AshesQueue is the facade queue for producers and consumers. The state of the Queue transforms between three different Queue implementations which are

  * emoryQueue
  * ersistentQueue
  * tagingQueue(An instance of MemoryQueue)

Initially the MemoryQueue is used for message production and consumption. A simple ArrayBlockingQueue is used to keep the messages in FIFO order.

When the MemoryQueue is getting full, the stage moves to overflow where the new messages will be kept in PersistentQueue. At the same time, a separate ReloaderThread will be invoked to reload messages back to MemoryQueue when the MemoryQueue slots getting freed by consumer.

There will be a time where this state should be moved to Offloading state where the producer can go back to produce message directly to Memory. To do this, we need to introduce a staging queue, so that while the reloader is reloading the messages back to MemoryQueue, producer can produce message to StagingMemoryQueue. In that way we can ensure the PersistentQueue can become empty. In that stage, we will move all the StatgingMemoryQueue objects to MainMemoryQueue and move the state to Normal.

There is a state transition from Off Flow to Overload stage also which is understandable.

A backlog feature also available in the even when we restart the services, the messages in the persistent file can be cleared first.

## Persistent Queue ##
I tried everything to make sure this is faster!

We are keeping two different MappedByteBuffers one for read and other for write. The buffers are kept moving the positions when the page size is full. This helps not to have a bigger chunk of MappedBuffer.

The message packet has a 5 bytes header and offset of data. 1st byte tells whether the message is already read or not. next four byte to tell the length of the payload and the rest of course is the payload.

I will keep updating this little document whenever I get time and based on the user feedback.