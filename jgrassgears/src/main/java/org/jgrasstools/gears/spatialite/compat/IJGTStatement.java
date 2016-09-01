package org.jgrasstools.gears.spatialite.compat;

public interface IJGTStatement extends AutoCloseable {

    void execute( String sql ) throws Exception;

    IJGTResultSet executeQuery( String sql ) throws Exception;

    void setQueryTimeout( int seconds ) throws Exception;

    int executeUpdate( String sql ) throws Exception;

}