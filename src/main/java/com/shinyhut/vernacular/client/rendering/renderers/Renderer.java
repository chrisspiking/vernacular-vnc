package com.shinyhut.vernacular.client.rendering.renderers;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

public interface Renderer {
    void render(InputStream in, BufferedImage destination, Rectangle rectangle) throws VncException;
}
