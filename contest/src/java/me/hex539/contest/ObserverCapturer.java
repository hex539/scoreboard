package me.hex539.contest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/**
 * Interface implementation forwarder.
 * <p>
 * Records actions passed into an interface in order to dispatch them in the same order at another
 * time, in another cadence, or to a set of observers that hasn't necessarily been computed yet.
 * <p>
 * This exists because built-in RPC methods are nonexistent. If there is a relatively simple
 * protobuf-based platform agnostic RPC library that can talk to itself in the same process, across
 * threads, without making useless HTTP connections to itself, that would be perfectly suitable as
 * a replacement but I have yet to find one (binder is out of the question due to its strange
 * special case for in-process communication; it may be possible to build a custom transport for gRPC
 * but that would involve a lot more code than this approach).
 * <p>
 * Patches to replace this nasty reflection-based workaround welcome.
 */
class ObserverCapturer {

  /** Static helper class. No instantiation for you. */
  private ObserverCapturer() {}

  /** @param destination will receive all events declared explicitly by @param baseClass */
  public static final <T> T captivate(
      T proxyBase,
      Class<? super T> baseClass,
      Consumer<Consumer<T>> destination) {
    return (T) Proxy.newProxyInstance(
          baseClass.getClassLoader(),
          new Class[] { baseClass },
          new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
              if (method.getDeclaringClass() != baseClass) {
                try {
                  return method.invoke(proxyBase, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                  throw new RuntimeException(e);
                }
              }

              destination.accept(o -> {
                try {
                  method.invoke(o, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                  // TODO: surface errors nicely.
                }
              });
              return null;
            }
          });
  }
}
