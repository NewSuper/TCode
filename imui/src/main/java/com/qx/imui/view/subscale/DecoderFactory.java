package com.qx.imui.view.subscale;


import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;

public interface DecoderFactory<T> {

    /**
     * Produce a new instance of a decoder with type {@link T}.
     * @return a new instance of your decoder.
     * @throws IllegalAccessException if the factory class cannot be instantiated.
     * @throws InstantiationException if the factory class cannot be instantiated.
     * @throws NoSuchMethodException if the factory class cannot be instantiated.
     * @throws InvocationTargetException if the factory class cannot be instantiated.
     */
    @NonNull
    T make() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException;
}
