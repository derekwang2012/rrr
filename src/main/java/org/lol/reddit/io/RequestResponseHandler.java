package org.lol.reddit.io;

public interface RequestResponseHandler<E, F> {
    public void onRequestFailed(F failureReason);

    public void onRequestSuccess(E result, long timeCached);
}
