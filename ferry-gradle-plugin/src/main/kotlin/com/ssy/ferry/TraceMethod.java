package com.ssy.ferry;

public class TraceMethod {
    private int id;
    private long tmp;
    public int accessFlag;


    public TraceMethod(int id, long tmp, int accessFlag) {
        this.id = id;
        this.tmp = tmp;
        this.accessFlag = accessFlag;
    }

    @Override
    public String toString() {
        return "TraceMethod{" +
                "id=" + id +
                ", tmp=" + tmp +
                ", accessFlag=" + accessFlag +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTmp() {
        return tmp;
    }

    public void setTmp(long tmp) {
        this.tmp = tmp;
    }

    public int getAccessFlag() {
        return accessFlag;
    }

    public void setAccessFlag(int accessFlag) {
        this.accessFlag = accessFlag;
    }
}
