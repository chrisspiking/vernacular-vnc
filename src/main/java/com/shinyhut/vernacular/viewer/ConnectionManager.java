package com.shinyhut.vernacular.viewer;

public interface ConnectionManager
{
    void connect(String host, int port);
    
    void disconnect();
    
    boolean isConnected();
}
