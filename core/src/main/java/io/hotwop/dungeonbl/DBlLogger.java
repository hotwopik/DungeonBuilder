package io.hotwop.dungeonbl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public final class DBlLogger extends AbstractLogger {
    private final Logger delegate=LoggerFactory.getLogger("DungeonBuilder");

    public DBlLogger(){}

    @Override
    protected String getFullyQualifiedCallerName() {
        return "DungeonBuilder";
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable){
        String formatThrowable;
        if(throwable==null)formatThrowable="";
        else{
            formatThrowable="\n";
            var printOut=new ByteArrayOutputStream();
            var printWriter=new PrintWriter(printOut,true);
            throwable.printStackTrace(printWriter);
            formatThrowable+=printOut.toString();
            printWriter.close();
        }

        String message;
        if(arguments==null || arguments.length==0){
            message=messagePattern+formatThrowable;
        }else{
            message=MessageFormatter.arrayFormat(messagePattern, arguments, throwable).getMessage()+formatThrowable;
        }

        delegate.makeLoggingEventBuilder(level)
            .addMarker(marker)
            .log(message);

        DBlCore.getPlugin()
            .playerLog(level,message);
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled();
    }
}
