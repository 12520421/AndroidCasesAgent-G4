package com.xzfg.app.security;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;


/**
 * Provides secure delete functionality.
 */
public class SecureDelete {
    private SecureDelete() {
    }

    /**
     * Performs a secure recursive delete.
     *
     * @param file the file or directory to delete.
     * @return boolean success or failure.
     */
    public static boolean delete(File file) {

        ConcurrentLinkedQueue<File> fifo = new ConcurrentLinkedQueue<>();
        LinkedList<File> dirs = new LinkedList<>();
        SecureRandom random = new SecureRandom();
        boolean success = false;
        fifo.add(file);

        while (!fifo.isEmpty()) {
            File head = fifo.poll();
            if (head.exists()) {
                if (head.isDirectory()) {
                    fifo.addAll(Arrays.asList(head.listFiles()));
                    dirs.add(head);
                } else {
                    try {
                        RandomAccessFile raf = new RandomAccessFile(head, "rw");
                        FileChannel channel = raf.getChannel();
                        MappedByteBuffer buffer
                                = channel.map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
                        // overwrite with zeros
                        while (buffer.hasRemaining()) {
                            buffer.put((byte) 0);
                        }

                        buffer.force();
                        buffer.rewind();

                        // overwrite with ones
                        while (buffer.hasRemaining()) {
                            buffer.put((byte) 0xFF);
                        }
                        buffer.force();
                        buffer.rewind();

                        // overwrite with random data; one byte at a time
                        byte[] data = new byte[1];

                        while (buffer.hasRemaining()) {
                            random.nextBytes(data);
                            buffer.put(data[0]);
                        }
                        buffer.force();

                        if (!file.delete()) {
                            success = false;
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error performing secure delete.");
                        success = false;
                    }
                }
            }
        }
        while (!dirs.isEmpty()) {
            if (!dirs.removeLast().delete()) {
                success = false;
            }
        }
        return success;
    }


}
