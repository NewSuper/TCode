package com.qx.imui.view.subscale;

import android.graphics.Bitmap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class CompatDecoderFactory<T> implements DecoderFactory<T> {
    private final Class<? extends T> clazz;
    private final Bitmap.Config bitmapConfig;

    /**
     * Construct a factory for the given class. This must have a default constructor.
     * @param clazz a class that implements {@link ImageDecoder} or {@link ImageRegionDecoder}.
     */
    public CompatDecoderFactory(@NonNull Class<? extends T> clazz) {
        this(clazz, null);
    }

    /**
     * Construct a factory for the given class. This must have a constructor that accepts a {@link Bitmap.Config} instance.
     * @param clazz a class that implements {@link ImageDecoder} or {@link ImageRegionDecoder}.
     * @param bitmapConfig bitmap configuration to be used when loading images.
     */
    public CompatDecoderFactory(@NonNull Class<? extends T> clazz, Bitmap.Config bitmapConfig) {
        this.clazz = clazz;
        this.bitmapConfig = bitmapConfig;
    }

    @Override
    @NonNull
    public T make() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (bitmapConfig == null) {
            return clazz.newInstance();
        } else {
            Constructor<? extends T> ctor = clazz.getConstructor(Bitmap.Config.class);
            return ctor.newInstance(bitmapConfig);
        }
    }
}
