package com.shinyhut.vernacular.viewer;

import java.io.Serializable;
import java.util.Objects;

public class Tuple<A, B> implements Serializable
{
    private static final long serialVersionUID = -8748327688104800131L;
    
    private final A objectOne;
    private final B objectTwo;
    
    public Tuple(A objectOne, B objectTwo)
    {
        this.objectOne = objectOne;
        this.objectTwo = objectTwo;
    }
    
    public A getObjectOne()
    {
        return objectOne;
    }
    
    public B getObjectTwo()
    {
        return objectTwo;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final Tuple<?, ?> tuple = (Tuple<?, ?>)o;
        return Objects.equals(objectOne, tuple.objectOne) && Objects.equals(objectTwo, tuple.objectTwo);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(objectOne, objectTwo);
    }
    
    @Override
    public String toString()
    {
        return "Tuple{" + "objectOne=" + objectOne + ", objectTwo=" + objectTwo + '}';
    }
}
