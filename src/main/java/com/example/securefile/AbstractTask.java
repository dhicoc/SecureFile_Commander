package com.example.securefile;

public abstract class AbstractTask implements Runnable {
    protected final String owner;
    protected final String name;

    protected AbstractTask(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    public String getName() { return name; }
    public String getOwner() { return owner; }

    public abstract void run();
}