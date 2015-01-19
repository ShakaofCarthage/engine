package com.eaw1805.core.support;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;

public class SilentIncorrectnessListener
        implements IncorrectnessListener
{
    @Override public void notify( String message, Object origin )
    {
        // do nuttin' honey!
    }
}