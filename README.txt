##############################################################################
#   Copyright 2010 JK
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
###############################################################################

Prerequisites
-------------
1) java version "1.7.0-ea"   (1.6 should be ok)
2) Apache Maven 2.2.1 or more
3) SVN
4) I test this on 2.6.35-23-generic kernal, ubuntu 10.4 version.
   The system uses MappedByteBuffer, so it could well be an issue if you use windows.


How to build the system
-----------------------
1) Checkout the code to a directory
svn checkout http://ashes-queue.googlecode.com/svn/trunk/ ashes-queue-read-only

2) Go to the trunk directory and use
mvn clean install

3) Use the target/ashes-1.0.jar to develop your producer consumer part

How to test the system
----------------------
1) Simple ProducerConsumerSample.java does the both the producing and consuming work
2) The properties of the producer/consumer is configured in pc.properties
3) go to target/ashes-queue/bin and run ./ashes console
4) You can see the messages persistent file inside target/ashes-queue directory

