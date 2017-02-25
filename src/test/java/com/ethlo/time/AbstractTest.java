package com.ethlo.time;

import java.util.function.Consumer;

import org.junit.Before;

public abstract class AbstractTest<T>
{
    protected T instance;

    protected abstract T getInstance();
    
    @Before
    public void setup()
    {
        this.instance = getInstance();
    }
    
    protected final void perform(int runs, Consumer<Void> func, String msg)
    {
        final long start = System.nanoTime();
        for (int i = 0; i < runs; i++)
        {
            func.accept(null);
        }
        final long end = System.nanoTime();
        final double secs = (end - start) / 1_000_000_000D;
        System.out.println(msg + ". " + secs + " elapsed. " + (runs / secs)  + " iterations/sec. " + runs + " total iterations");        
    }
}
