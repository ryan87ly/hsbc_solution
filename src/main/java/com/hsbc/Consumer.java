package com.hsbc;

import com.hsbc.output.Output;

class Consumer implements Transmission.MessageMuncher {

    final Transmission transmission; // ... from ctor
    final Output output;

    public Consumer(Transmission transmission, Output output) {
        this.transmission = transmission;
        this.output = output;
    }

    public boolean on(Message m) {
        // your code here
        output.print(m);
        return true;
    }

    // somewhere in the consumer main loop...
    public void run() {
        // ...
        transmission.read(10, this); // request up to 10 msgs from transmission

        // ...
    }
}