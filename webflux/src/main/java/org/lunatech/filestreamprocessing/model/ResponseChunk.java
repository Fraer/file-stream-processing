package org.lunatech.filestreamprocessing.model;

public class ResponseChunk {

    private String name;

    private int processed;

    private int skipped;

    public ResponseChunk() {}

    public ResponseChunk(String name, int processed, int skipped) {
        this.name = name;
        this.processed = processed;
        this.skipped = skipped;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    @Override
    public String toString() {
        return "Data [content=" + name + ", index=" + processed + "]";
    }

}
