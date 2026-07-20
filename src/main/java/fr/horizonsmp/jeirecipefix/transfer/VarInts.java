package fr.horizonsmp.jeirecipefix.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

final class VarInts {
    private VarInts() {}

    static int read(ByteArrayInputStream input) throws IOException {
        int value = 0;
        int position = 0;
        int current;
        do {
            current = input.read();
            if (current == -1) throw new IOException("Unexpected end of JEI packet");
            value |= (current & 0x7F) << position;
            position += 7;
            if (position >= 35) throw new IOException("VarInt is too large");
        } while ((current & 0x80) != 0);
        return value;
    }
}
