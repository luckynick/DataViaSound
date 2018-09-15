package com.luckynick.shared;

@FunctionalInterface
public interface PureFunctionalInterfaceWithReturn<T> {
    /**
     * Method which does action specific to this class.
     */
    T performProgramTasks();
}
