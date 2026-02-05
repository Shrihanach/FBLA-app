package com.example.fblaapp.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 * Provides a single shared executor for database operations.
 */
public class AppExecutors {

    private static final Executor diskIO = Executors.newSingleThreadExecutor();

    private AppExecutors() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the disk I/O executor for database operations.
     * Uses a single thread to ensure database operations are serialized.
     */
    public static Executor diskIO() {
        return diskIO;
    }
}
