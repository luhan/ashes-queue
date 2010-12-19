/*
*   Copyright 2010 JK
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package jk.ashes.example;

import java.io.Serializable;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class Message implements Serializable{
    private int index = 1;
    private String value = "My name is JK and I am a crap living in Singapore !@#$$%^^&&";

    public Message(int i) {
        index = i;
    }

    public int index() {
        return index;
    }

    public void index(int index) {
        this.index = index;
    }

    public String value() {
        return value;
    }

    public String toString() {
        return "{index='" + index + '}';
    }
}

