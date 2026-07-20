package fr.horizonsmp.jeirecipefix.transfer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferPacketTest {
    @Test
    void decodesUncountedPacket() throws Exception {
        byte[] packet = bytes(1, 10, 1, 1, 1, 2, 10, 11, 1, 0);
        TransferPacket decoded = TransferPacket.decode(packet, false);
        assertEquals(1, decoded.operations().size());
        assertEquals(new TransferPacket.Operation(10, 1, 1), decoded.operations().getFirst());
        assertEquals(java.util.List.of(1), decoded.craftingSlots());
        assertEquals(java.util.List.of(10, 11), decoded.inventorySlots());
        assertTrue(decoded.maxTransfer());
    }

    @Test
    void decodesCountedPacket() throws Exception {
        byte[] packet = bytes(1, 10, 1, 3, 1, 1, 1, 10, 0, 1);
        TransferPacket decoded = TransferPacket.decode(packet, true);
        assertEquals(new TransferPacket.Operation(10, 1, 3), decoded.operations().getFirst());
        assertTrue(decoded.requireCompleteSets());
    }

    @Test
    void rejectsTrailingAndOversizedData() {
        assertThrows(IOException.class, () -> TransferPacket.decode(bytes(0, 0, 0, 0, 0, 1), false));
        assertThrows(IOException.class, () -> TransferPacket.decode(varInt(257), false));
        assertThrows(IOException.class, () -> TransferPacket.decode(bytes(1, 10, 1, 0), true));
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) bytes[i] = (byte) values[i];
        return bytes;
    }

    private static byte[] varInt(int value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        do {
            int current = value & 0x7F;
            value >>>= 7;
            if (value != 0) current |= 0x80;
            output.write(current);
        } while (value != 0);
        return output.toByteArray();
    }
}
