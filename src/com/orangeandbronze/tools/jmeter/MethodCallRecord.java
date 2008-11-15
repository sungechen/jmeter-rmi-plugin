package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class MethodCallRecord
    implements Serializable
{

    private static final long serialVersionUID = -30090001L;

    private String method;
    private Object[] args;
    private byte[] argsPacked;
    private Object returnValue;
    private Throwable returnException;
    private boolean isException = false;

    MethodCallRecord() {
    }

    public MethodCallRecord(Method m, Object[] args) {
        this.method = m.getName();
        this.args = args;
        packArgs();
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return args;
    }

    public void returned(Object returnValue) {
        this.returnValue = returnValue;
    }

    public void thrown(Throwable t) {
        this.returnValue = t;
        this.isException = true;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Throwable getReturnValueAsThrowable() {
        if(isException) {
            return (Throwable) returnValue;
        }
        else {
            throw new IllegalStateException("Not an exception");
        }
    }

    public boolean isException() {
        return isException;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException {
        // Custom format, to allow packed argument values
        out.writeUTF("CALL");

        out.writeUTF(method);

        out.writeInt(argsPacked.length);
        out.write(argsPacked);

        out.writeUTF("RETURN");

        out.writeBoolean(isException);
        out.writeObject(returnValue);

        out.writeUTF("END");
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Custom format, to allow packed argument values
        String head = in.readUTF();
        if(!"CALL".equals(head)) {
            throw new IllegalStateException("Invalid state in input stream: Object header not found");
        }

        method = in.readUTF();

        int packLen = in.readInt();
        argsPacked = new byte[packLen];
        in.read(argsPacked, 0, packLen);

        String ret = in.readUTF();
        if(!"RETURN".equals(ret)) {
            throw new IllegalStateException("Invalid state in input stream: Return value header not found");
        }

        isException = in.readBoolean();
        returnValue = in.readObject();

        String eof = in.readUTF();
        if(!"END".equals(eof)) {
            throw new IllegalStateException("Invalid state in input stream: End of stream not found");
        }

        unpackArgs();
    }

    private void packArgs() {
        try {
            ByteArrayOutputStream packOut = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(packOut);
            ostream.writeObject(args);
            argsPacked = packOut.toByteArray();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
    }

    private void unpackArgs() {
        try {
            ByteArrayInputStream packIn = new ByteArrayInputStream(argsPacked);
            ObjectInputStream istream = new ObjectInputStream(packIn);
            args = (Object[]) istream.readObject();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
        catch(ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
}
